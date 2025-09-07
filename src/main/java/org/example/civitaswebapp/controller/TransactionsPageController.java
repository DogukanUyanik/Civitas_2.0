package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/transactions")
public class TransactionsPageController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public String showTransactions(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) TransactionStatus status,
                                   @RequestParam(required = false) TransactionType type,
                                   @RequestParam(defaultValue = "createdAt") String sort,
                                   @RequestParam(defaultValue = "desc") String direction) {

        // Create sort object
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortBy = Sort.by(sortDirection, sort);

        // Create pageable with sorting
        Pageable pageable = PageRequest.of(page, size, sortBy);

        // Get filtered and sorted transactions
        var transactionsPage = transactionService.getTransactions(pageable, search, status, type);

        // Add attributes to model
        model.addAttribute("transactionsPage", transactionsPage);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);

        return "transactions/transactionsList";
    }
}