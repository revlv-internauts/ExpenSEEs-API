package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.SubmittedBudget;
import com.example.expensetracker.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/funds")
public class FundRequestController {

    @Autowired
    private BudgetService budgetService;

    @PostMapping("/request")
    public ResponseEntity<String> requestFund(@RequestBody SubmittedBudget budget) {
        budgetService.createBudget(budget);
        return ResponseEntity.ok("Fund request for " + budget.getName() + " submitted successfully");
    }
}