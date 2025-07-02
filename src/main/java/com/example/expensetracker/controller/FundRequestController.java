package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.SubmittedBudget;
import com.example.expensetracker.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/funds")
public class FundRequestController {

    @Autowired
    private BudgetService budgetService;

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestFund(@RequestBody SubmittedBudget budget) {
        Map<String, String> response = new HashMap<>();
        budgetService.createBudget(budget);
        response.put("message", "Fund request for " + budget.getName() + " submitted successfully");
        return ResponseEntity.ok(response);
    }
}