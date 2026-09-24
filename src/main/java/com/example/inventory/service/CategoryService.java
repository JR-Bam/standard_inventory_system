package com.example.inventory.service;

import com.example.inventory.entity.Category;
import com.example.inventory.form.CategoryForm;
import com.example.inventory.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final InventoryLogService logService;

    public CategoryService(CategoryRepository categoryRepository,
                           InventoryLogService logService) {
        this.categoryRepository = categoryRepository;
        this.logService = logService;
    }

    @Transactional(readOnly = true)
    public Page<Category> search(String search, Boolean active, Pageable pageable) {
        return categoryRepository.search(search, active, pageable);
    }

    @Transactional(readOnly = true)
    public List<Category> activeCategories() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
    }

    @Transactional
    public Category create(CategoryForm form, String username) {
        if (categoryRepository.existsByNameIgnoreCase(form.getName())) {
            throw new IllegalArgumentException("Category name already exists");
        }
        Category category = Category.builder()
                .name(form.getName().trim())
                .description(form.getDescription())
                .active(true)
                .build();
        Category saved = categoryRepository.save(category);
        logService.logCategoryCreated(saved, username);
        return saved;
    }

    @Transactional
    public Category update(Long id, CategoryForm form, String username) {
        Category category = getById(id);
        if (!category.getName().equalsIgnoreCase(form.getName())
                && categoryRepository.existsByNameIgnoreCase(form.getName())) {
            throw new IllegalArgumentException("Category name already exists");
        }
        category.setName(form.getName().trim());
        category.setDescription(form.getDescription());
        category.setActive(form.isActive());
        Category saved = categoryRepository.save(category);
        logService.logCategoryUpdated(saved, username);
        return saved;
    }

    @Transactional
    public void softDelete(Long id, String username) {
        Category category = getById(id);
        category.setActive(false);
        Category saved = categoryRepository.save(category);
        logService.logCategoryDeleted(saved, username);
    }
}