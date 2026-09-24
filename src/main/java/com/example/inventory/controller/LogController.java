package com.example.inventory.controller;

import com.example.inventory.entity.LogAction;
import com.example.inventory.entity.LogEntityType;
import com.example.inventory.service.InventoryLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequestMapping("/logs")
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    private final InventoryLogService logService;

    public LogController(InventoryLogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) LogEntityType entityType,
            @RequestParam(required = false) LogAction action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.atTime(LocalTime.MAX);

        Page<?> logs = logService.search(
                entityType, action, userId, fromDateTime, toDateTime, search,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("logs", logs);
        model.addAttribute("entityTypes", LogEntityType.values());
        model.addAttribute("actions", LogAction.values());
        model.addAttribute("users", logService.distinctUsers());
        model.addAttribute("entityType", entityType);
        model.addAttribute("action", action);
        model.addAttribute("userId", userId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("search", search);
        return "logs/list";
    }
}