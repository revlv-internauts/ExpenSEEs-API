package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.SubmittedBudget;
import com.example.expensetracker.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @PostMapping
    public ResponseEntity<SubmittedBudget> createBudget(@RequestBody SubmittedBudget budget) {
        return ResponseEntity.ok(budgetService.createBudget(budget));
    }

    @GetMapping
    public ResponseEntity<List<SubmittedBudget>> getBudgets() {
        return ResponseEntity.ok(budgetService.getAllBudgets());
    }

    @PutMapping("/{budgetId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateStatus(@PathVariable Long budgetId, @RequestBody String status) {
        Map<String, String> response = new HashMap<>();
        try {
            SubmittedBudget.Status enumStatus = SubmittedBudget.Status.valueOf(status.toUpperCase());
            budgetService.updateBudgetStatus(budgetId, enumStatus);
            response.put("message", "Status updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", "Invalid status");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{budgetId}/expenses/{expenseId}")
    public ResponseEntity<Map<String, String>> associateExpense(@PathVariable Long budgetId, @PathVariable Long expenseId) {
        Map<String, String> response = new HashMap<>();
        budgetService.associateExpenseWithBudget(budgetId, expenseId);
        response.put("message", "Expense associated with budget successfully");
        return ResponseEntity.ok(response);
    }
}