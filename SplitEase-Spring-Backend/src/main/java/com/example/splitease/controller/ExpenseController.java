package com.example.splitease.controller;


import com.example.splitease.requestAndResponse.AddExpenseRequest;
import com.example.splitease.service.ActivityService;
import com.example.splitease.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.util.Collections;
import java.util.UUID;

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
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/expense/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable("id") UUID id, Principal principal) throws AccessDeniedException {
        System.out.println("delete expense request received");
        expenseService.deleteExpense(id, principal.getName());
        return ResponseEntity.ok().build();
    }
}
