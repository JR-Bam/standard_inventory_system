package com.example.inventory.controller;

import com.example.inventory.entity.Item;
import com.example.inventory.entity.StockMovement;
import com.example.inventory.form.StockMovementForm;
import com.example.inventory.service.ItemService;
import com.example.inventory.service.StockMovementService;
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
@RequestMapping("/items/{itemId}/movements")
public class StockMovementController {

    private final StockMovementService movementService;
    private final ItemService itemService;

    public StockMovementController(StockMovementService movementService,
                                   ItemService itemService) {
        this.movementService = movementService;
        this.itemService = itemService;
    }

    @GetMapping
    public String list(@PathVariable Long itemId,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        Item item = itemService.getById(itemId);
        Page<StockMovement> movements = movementService.findByItem(
                itemId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        model.addAttribute("item", item);
        model.addAttribute("movements", movements);
        return "movements/list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long itemId, Model model) {
        Item item = itemService.getById(itemId);
        model.addAttribute("item", item);
        model.addAttribute("form", new StockMovementForm());
        return "movements/form";
    }

    @PostMapping
    public String create(@PathVariable Long itemId,
                         @Valid @ModelAttribute("form") StockMovementForm form,
                         BindingResult binding,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirect) {
        Item item = itemService.getById(itemId);

        if (binding.hasErrors()) {
            model.addAttribute("item", item);
            return "movements/form";
        }

        try {
            movementService.record(itemId, form, auth.getName());
            redirect.addFlashAttribute("success", "Stock movement recorded");
            return "redirect:/items/" + itemId + "/movements";
        } catch (IllegalArgumentException ex) {
            binding.reject("movement", ex.getMessage());
            model.addAttribute("item", item);
            return "movements/form";
        }
    }
}