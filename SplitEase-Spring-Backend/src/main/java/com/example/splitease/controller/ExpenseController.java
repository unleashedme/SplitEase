package com.example.splitease.controller;


import com.example.splitease.requestAndResponse.AddExpenseRequest;
import com.example.splitease.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping("/expense")
    public ResponseEntity<?> addExpense(
            @RequestBody AddExpenseRequest request,
            Principal principal
    ) {
        expenseService.addExpense(request, principal.getName());
        return ResponseEntity.ok("Expense added");
    }

}
