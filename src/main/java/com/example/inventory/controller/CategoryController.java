package com.example.inventory.controller;

import com.example.inventory.entity.Category;
import com.example.inventory.form.CategoryForm;
import com.example.inventory.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) Boolean active,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        Page<Category> result = categoryService.search(
                search, active, PageRequest.of(page, size, Sort.by("name").ascending()));
        model.addAttribute("categories", result);
        model.addAttribute("search", search);
        model.addAttribute("activeFilter", active);
        return "categories/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new CategoryForm());
        model.addAttribute("mode", "create");
        return "categories/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CategoryForm form,
                         BindingResult binding,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("mode", "create");
            return "categories/form";
        }
        try {
            categoryService.create(form, auth.getName());
            redirect.addFlashAttribute("success", "Category created");
            return "redirect:/categories";
        } catch (IllegalArgumentException ex) {
            binding.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("mode", "create");
            return "categories/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getById(id);
        CategoryForm form = new CategoryForm();
        form.setId(category.getId());
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        form.setActive(category.isActive());
        model.addAttribute("form", form);
        model.addAttribute("mode", "edit");
        return "categories/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") CategoryForm form,
                         BindingResult binding,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("mode", "edit");
            return "categories/form";
        }
        try {
            categoryService.update(id, form, auth.getName());
            redirect.addFlashAttribute("success", "Category updated");
            return "redirect:/categories";
        } catch (IllegalArgumentException ex) {
            binding.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("mode", "edit");
            return "categories/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        categoryService.softDelete(id, auth.getName());
        redirect.addFlashAttribute("success", "Category deactivated");
        return "redirect:/categories";
    }
}