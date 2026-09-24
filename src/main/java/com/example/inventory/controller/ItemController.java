package com.example.inventory.controller;

import com.example.inventory.entity.Item;
import com.example.inventory.form.ItemForm;
import com.example.inventory.service.CategoryService;
import com.example.inventory.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;
    private final CategoryService categoryService;

    public ItemController(ItemService itemService, CategoryService categoryService) {
        this.itemService = itemService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Boolean active,
                       @RequestParam(required = false) Boolean lowStock,
                       @RequestParam(required = false) Boolean outOfStock,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(defaultValue = "name") String sort,
                       @RequestParam(defaultValue = "asc") String dir,
                       Model model) {

        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<Item> result = itemService.search(search, categoryId, active, lowStock, outOfStock, pageable);

        model.addAttribute("items", result);
        model.addAttribute("categories", categoryService.activeCategories());
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("activeFilter", active);
        model.addAttribute("lowStock", lowStock);
        model.addAttribute("outOfStock", outOfStock);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "items/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("item", itemService.getById(id));
        return "items/detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ItemForm());
        model.addAttribute("categories", categoryService.activeCategories());
        model.addAttribute("mode", "create");
        return "items/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ItemForm form,
                         BindingResult binding,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("categories", categoryService.activeCategories());
            model.addAttribute("mode", "create");
            return "items/form";
        }
        try {
            itemService.create(form, auth.getName());
            redirect.addFlashAttribute("success", "Item created");
            return "redirect:/items";
        } catch (IllegalArgumentException ex) {
            binding.rejectValue("sku", "duplicate", ex.getMessage());
            model.addAttribute("categories", categoryService.activeCategories());
            model.addAttribute("mode", "create");
            return "items/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Item item = itemService.getById(id);
        ItemForm form = new ItemForm();
        form.setId(item.getId());
        form.setSku(item.getSku());
        form.setName(item.getName());
        form.setDescription(item.getDescription());
        form.setCategoryId(item.getCategory().getId());
        // quantity is NOT populated — it's not editable
        form.setUnitPrice(item.getUnitPrice());
        form.setReorderLevel(item.getReorderLevel());
        form.setLocation(item.getLocation());
        form.setActive(item.isActive());

        model.addAttribute("form", form);
        model.addAttribute("item", item);   // needed for the read-only quantity display
        model.addAttribute("categories", categoryService.activeCategories());
        model.addAttribute("mode", "edit");
        return "items/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ItemForm form,
                         BindingResult binding,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("categories", categoryService.activeCategories());
            model.addAttribute("item", itemService.getById(id));
            model.addAttribute("mode", "edit");
            return "items/form";
        }
        try {
            itemService.update(id, form, auth.getName());
            redirect.addFlashAttribute("success", "Item updated");
            return "redirect:/items/" + id;
        } catch (IllegalArgumentException ex) {
            binding.rejectValue("sku", "duplicate", ex.getMessage());
            model.addAttribute("categories", categoryService.activeCategories());
            model.addAttribute("item", itemService.getById(id));
            model.addAttribute("mode", "edit");
            return "items/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        itemService.softDelete(id, auth.getName());
        redirect.addFlashAttribute("success", "Item deactivated");
        return "redirect:/items";
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public String restore(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            itemService.restore(id, auth.getName());
            redirect.addFlashAttribute("success", "Item restored");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/items/" + id;
    }
}