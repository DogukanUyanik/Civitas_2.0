package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.MyUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardPageController {

    @GetMapping
    public String showDashboardPage(Model model, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof MyUser user) {
            model.addAttribute("currentUserId", user.getId());
        }
        return "dashboard";
    }
}
