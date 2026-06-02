package com.example.smartlibrary.repository;

import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.model.AiModel;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AiModelRepository {

    private static final RowMapper<AiModel> AI_MODEL_ROW_MAPPER = (rs, rowNum) -> {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new AiModel(
                rs.getLong("id"),
                rs.getString("model_name"),
                rs.getString("provider"),
                createdAt == null ? null : createdAt.toLocalDateTime());
    };

    private final JdbcTemplate jdbcTemplate;

    public AiModelRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AiModel> findAll() {
        return jdbcTemplate.query(
                "SELECT id, model_name, provider, created_at FROM ai_models ORDER BY id DESC",
                AI_MODEL_ROW_MAPPER);
    }

    public Optional<AiModel> findFirst() {
        return jdbcTemplate.query(
                        "SELECT id, model_name, provider, created_at FROM ai_models ORDER BY id DESC LIMIT 1",
                        AI_MODEL_ROW_MAPPER)
                .stream()
                .findFirst();
    }

    public Optional<AiModel> findById(Long id) {
        return jdbcTemplate.query(
                        "SELECT id, model_name, provider, created_at FROM ai_models WHERE id = ?",
                        AI_MODEL_ROW_MAPPER,
                        id)
                .stream()
                .findFirst();
    }

    public AiModel create(String modelName) {
        jdbcTemplate.update(
                "INSERT INTO ai_models (model_name, provider) VALUES (?, 'MiMo')",
                modelName.trim());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return findById(id).orElseThrow(() -> new BusinessException("模型添加失败"));
    }

    public void delete(Long id) {
        int rows = jdbcTemplate.update("DELETE FROM ai_models WHERE id = ?", id);
        if (rows == 0) {
            throw new BusinessException("模型不存在");
        }
    }
}
