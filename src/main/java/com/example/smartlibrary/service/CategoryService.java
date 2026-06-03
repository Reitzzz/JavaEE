package com.example.smartlibrary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.smartlibrary.dto.CategoryRequest;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.mapper.CategoryMapper;
import com.example.smartlibrary.model.Category;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<Category> findAll() {
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");
        return categoryMapper.selectList(wrapper);
    }

    public Category findById(Long id) {
        return categoryMapper.selectById(id);
    }

    public Category create(CategoryRequest request) {
        validate(request);
        Category category = new Category();
        category.setName(request.name().trim());
        category.setDescription(request.description().trim());
        categoryMapper.insert(category);
        return findById(category.getId());
    }

    public Category update(Long id, CategoryRequest request) {
        validate(request);
        Category category = new Category();
        category.setId(id);
        category.setName(request.name().trim());
        category.setDescription(request.description().trim());
        int rows = categoryMapper.updateById(category);
        if (rows == 0) {
            throw new BusinessException("分类不存在");
        }
        return findById(id);
    }

    public void delete(Long id) {
        int rows = categoryMapper.deleteById(id);
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
