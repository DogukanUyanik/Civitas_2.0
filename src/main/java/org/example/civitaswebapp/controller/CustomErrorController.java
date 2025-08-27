package org.example.civitaswebapp.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomErrorController implements ErrorController {

    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR_MESSAGE = "errorMessage";

    @RequestMapping("/error")
    public String handleError(@RequestParam(name = "errorCode", required = false, defaultValue = "Unknown") String errorCode, @RequestParam(name = "errorMessage", required = false, defaultValue = "An unexpected error occurred") String errorMessage, HttpServletRequest request, Model model) {

        if ("Unknown".equals(errorCode) && "An unexpected error occurred".equals(errorMessage)) {
            Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

            if (status != null) {
                Integer statusCode = Integer.valueOf(status.toString());

                if (statusCode == HttpStatus.NOT_FOUND.value()) {
                    errorCode = "404";
                    errorMessage = "Page not found";
                } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                    errorCode = "403";
                    errorMessage = "Access is denied";
                } else {
                    errorCode = statusCode.toString();
                    errorMessage = "An unexpected error occurred";
                }
            }
        }

        model.addAttribute(ERROR_CODE, errorCode);
        model.addAttribute(ERROR_MESSAGE, errorMessage);

        return "error/error";
    }
}