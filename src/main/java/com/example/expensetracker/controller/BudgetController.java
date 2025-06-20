package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.SubmittedBudget;
import com.example.expensetracker.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(@PathVariable Long id, @RequestBody SubmittedBudget.Status status) {
        budgetService.updateBudgetStatus(id, status);
        return ResponseEntity.ok("Status updated successfully");
    }

    @PostMapping("/{budgetId}/expenses/{expenseId}")
    public ResponseEntity<String> associateExpense(@PathVariable Long budgetId, @PathVariable Long expenseId) {
        budgetService.associateExpenseWithBudget(budgetId, expenseId);
        return ResponseEntity.ok("Expense associated with budget successfully");
    }
}