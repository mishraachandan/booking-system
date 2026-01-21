package com.mishraachandan.booking_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mishraachandan.booking_system.dto.entity.Category;
import com.mishraachandan.booking_system.repository.CategoryRepository;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name).orElse(null);
    }
}
