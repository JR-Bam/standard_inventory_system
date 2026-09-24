package com.example.inventory.controller;

import com.example.inventory.entity.User;
import com.example.inventory.form.ChangePasswordForm;
import com.example.inventory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class PasswordController {

    private final UserService userService;

    public PasswordController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/change-password")
    public String showForm(Authentication auth, Model model) {
        User user = userService.getByUsername(auth.getName());
        model.addAttribute("form", new ChangePasswordForm());
        model.addAttribute("forced", user.isMustChangePassword());
        return "auth/change-password";
    }

    @PostMapping("/change-password")
    public String change(@Valid @ModelAttribute("form") ChangePasswordForm form,
                         BindingResult binding,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            User user = userService.getByUsername(auth.getName());
            model.addAttribute("forced", user.isMustChangePassword());
            return "auth/change-password";
        }
        try {
            userService.changeOwnPassword(auth.getName(), form);
            redirect.addFlashAttribute("success",
                    "Password changed successfully");
            return "redirect:/";
        } catch (IllegalArgumentException ex) {
            binding.reject("global", ex.getMessage());
            User user = userService.getByUsername(auth.getName());
            model.addAttribute("forced", user.isMustChangePassword());
            return "auth/change-password";
        }
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication auth) {
        return auth == null ? null : userService.getByUsername(auth.getName());
    }
}