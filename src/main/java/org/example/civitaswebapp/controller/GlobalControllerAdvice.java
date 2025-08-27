package org.example.civitaswebapp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("username")
    public String populateUsername(Authentication authentication) {
        return authentication == null ? "" : authentication.getName();
    }

    @ModelAttribute("role")
    public String populateRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().isEmpty()) {
            return "GUEST";
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .findFirst()
                .orElse("USER");
    }

    @ModelAttribute("currentUri")
    public String populateCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
