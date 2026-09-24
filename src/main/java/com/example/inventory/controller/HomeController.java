package com.example.inventory.controller;

import com.example.inventory.dto.DashboardData;
import com.example.inventory.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String dashboard(@RequestParam(defaultValue = "14") int days, Model model) {
        int clamped = Math.clamp(days, 7, 30);
        DashboardData data = dashboardService.build(clamped);
        model.addAttribute("dashboard", data);
        return "dashboard";
    }

}