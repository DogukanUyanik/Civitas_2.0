package org.example.civitaswebapp.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/login")
public class LoginController {

    @GetMapping
    public String login(String error, String logout, Model model) {

        if (error != null) {
            model.addAttribute("errorKey", "login.error.invalid");
        }
        if (logout != null) {
            model.addAttribute("logoutKey", "login.logout.success");
        }
        return "loginForm";
    }

}