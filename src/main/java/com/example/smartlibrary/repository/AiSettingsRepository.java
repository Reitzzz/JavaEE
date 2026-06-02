package com.example.smartlibrary.repository;

import com.example.smartlibrary.model.AiSettings;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiSettingsRepository {

    private final JdbcTemplate jdbcTemplate;

    public AiSettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AiSettings> find() {
        return jdbcTemplate.query(
                        "SELECT api_key, active_model_id FROM ai_settings WHERE id = 1",
                        (rs, rowNum) -> new AiSettings(rs.getString("api_key"), rs.getObject("active_model_id", Long.class)))
                .stream()
                .findFirst();
    }

    public void saveApiKey(String apiKey) {
        jdbcTemplate.update(
                """
                INSERT INTO ai_settings (id, api_key) VALUES (1, ?)
                ON DUPLICATE KEY UPDATE api_key = VALUES(api_key), updated_at = CURRENT_TIMESTAMP
                """,
                apiKey.trim());
    }

    public void saveActiveModel(Long modelId) {
        jdbcTemplate.update(
                """
                INSERT INTO ai_settings (id, api_key, active_model_id) VALUES (1, '', ?)
                ON DUPLICATE KEY UPDATE active_model_id = VALUES(active_model_id), updated_at = CURRENT_TIMESTAMP
                """,
                modelId);
    }

    public Optional<String> findActiveModelName() {
        try {
            String modelName = jdbcTemplate.queryForObject(
                    """
                    SELECT m.model_name
                    FROM ai_settings s
                    INNER JOIN ai_models m ON m.id = s.active_model_id
                    WHERE s.id = 1
                    """,
                    String.class);
            return Optional.ofNullable(modelName);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }
}
