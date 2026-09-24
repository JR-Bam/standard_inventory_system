package com.example.inventory.controller;

import com.example.inventory.entity.Role;
import com.example.inventory.entity.User;
import com.example.inventory.form.UserForm;
import com.example.inventory.service.UserService;
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
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) Role role,
                       @RequestParam(required = false) Boolean enabled,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        Page<User> users = userService.list(search, role, enabled,
                PageRequest.of(page, size, Sort.by("username").ascending()));

        model.addAttribute("users", users);
        model.addAttribute("roles", Role.values());
        model.addAttribute("search", search);
        model.addAttribute("roleFilter", role);
        model.addAttribute("enabledFilter", enabled);
        return "users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new UserForm());
        model.addAttribute("roles", Role.values());
        model.addAttribute("mode", "create");
        return "users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") UserForm form,
                         BindingResult binding,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirect) {
        // Clear password field validation errors that don't apply on create if blank
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            binding.rejectValue("password", "required", "Password is required");
        }

        if (binding.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("mode", "create");
            return "users/form";
        }
        try {
            User created = userService.create(form, auth.getName());
            redirect.addFlashAttribute("success",
                    "User '" + created.getUsername() + "' created");
            return "redirect:/users";
        } catch (IllegalArgumentException ex) {
            binding.rejectValue("username", "duplicate", ex.getMessage());
            model.addAttribute("roles", Role.values());
            model.addAttribute("mode", "create");
            return "users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userService.getById(id);
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setRole(user.getRole());
        form.setEnabled(user.isEnabled());
        form.setForcePasswordChange(user.isMustChangePassword());

        model.addAttribute("form", form);
        model.addAttribute("roles", Role.values());
        model.addAttribute("mode", "edit");
        model.addAttribute("targetUser", user);
        return "users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") UserForm form,
                         BindingResult binding,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("mode", "edit");
            model.addAttribute("targetUser", userService.getById(id));
            return "users/form";
        }
        try {
            userService.update(id, form, auth.getName());
            redirect.addFlashAttribute("success", "User updated");
            return "redirect:/users";
        } catch (IllegalArgumentException ex) {
            binding.reject("global", ex.getMessage());
            model.addAttribute("roles", Role.values());
            model.addAttribute("mode", "edit");
            model.addAttribute("targetUser", userService.getById(id));
            return "users/form";
        }
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable Long id,
                          Authentication auth,
                          RedirectAttributes redirect) {
        try {
            userService.setEnabled(id, false, auth.getName());
            redirect.addFlashAttribute("success", "User disabled");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable Long id,
                         Authentication auth,
                         RedirectAttributes redirect) {
        userService.setEnabled(id, true, auth.getName());
        redirect.addFlashAttribute("success", "User enabled");
        return "redirect:/users";
    }
}