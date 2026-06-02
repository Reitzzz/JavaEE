package com.example.smartlibrary.repository;

import com.example.smartlibrary.dto.CategoryRequest;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.model.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Category> findAll() {
        return jdbcTemplate.query(
                "SELECT id, name, description FROM categories ORDER BY id DESC",
                (rs, rowNum) -> new Category(rs.getLong("id"), rs.getString("name"), rs.getString("description")));
    }

    public Optional<Category> findById(Long id) {
        List<Category> categories = jdbcTemplate.query(
                "SELECT id, name, description FROM categories WHERE id = ?",
                (rs, rowNum) -> new Category(rs.getLong("id"), rs.getString("name"), rs.getString("description")),
                id);
        return categories.stream().findFirst();
    }

    public Category create(CategoryRequest request) {
        validate(request);
        jdbcTemplate.update(
                "INSERT INTO categories (name, description) VALUES (?, ?)",
                request.name().trim(),
                request.description().trim());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return findById(id).orElseThrow(() -> new BusinessException("分类创建失败"));
    }

    public Category update(Long id, CategoryRequest request) {
        validate(request);
        int rows = jdbcTemplate.update(
                "UPDATE categories SET name = ?, description = ? WHERE id = ?",
                request.name().trim(),
                request.description().trim(),
                id);
        if (rows == 0) {
            throw new BusinessException("分类不存在");
        }
        return findById(id).orElseThrow(() -> new BusinessException("分类不存在"));
    }

    public void delete(Long id) {
        int rows = jdbcTemplate.update("DELETE FROM categories WHERE id = ?", id);
        if (rows == 0) {
            throw new BusinessException("分类不存在");
        }
    }

    private void validate(CategoryRequest request) {
        if (request == null || isBlank(request.name()) || isBlank(request.description())) {
            throw new BusinessException("分类名称和描述不能为空");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
