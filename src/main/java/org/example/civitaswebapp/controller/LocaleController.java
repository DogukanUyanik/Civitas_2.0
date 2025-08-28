package org.example.civitaswebapp.controller;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class LocaleController {

    @Autowired
    private LocaleResolver localeResolver;

    @GetMapping("/changeLocale")
    public String changeLocale(HttpServletRequest request, HttpServletResponse response, @RequestParam("lang") String lang) {
        Locale locale = switch (lang) {
            case "nl" -> Locale.forLanguageTag("nl-NL");
            case "en" -> Locale.ENGLISH;
            default -> Locale.ENGLISH;
        };

        localeResolver.setLocale(request, response, locale);
        return "redirect:" + request.getHeader("Referer");
    }
}