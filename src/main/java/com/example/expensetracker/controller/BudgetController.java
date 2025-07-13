package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.SubmittedBudget;
import com.example.expensetracker.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> createBudget(@RequestBody SubmittedBudget budget) {
        try {
            SubmittedBudget createdBudget = budgetService.createBudget(budget);
            return ResponseEntity.ok(createdBudget);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create budget: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllBudgets() {
        try {
            List<SubmittedBudget> budgets = budgetService.getAllBudgets();
            return ResponseEntity.ok(budgets);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve budgets: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{budgetId}")
    public ResponseEntity<?> getBudgetById(@PathVariable Long budgetId) {
        try {
            ResponseEntity<SubmittedBudget> response = budgetService.getBudgetById(budgetId);
            if (response.getStatusCode() == HttpStatus.OK) {
                return ResponseEntity.ok(response.getBody());
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", response.getBody() == null ? "Budget not found" : "Unauthorized access");
                return new ResponseEntity<>(errorResponse, response.getStatusCode());
            }
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve budget: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{budgetId}/status")
    public ResponseEntity<Map<String, String>> updateBudgetStatus(@PathVariable Long budgetId, @RequestBody String status) {
        Map<String, String> response = new HashMap<>();
        try {
            SubmittedBudget.Status enumStatus = SubmittedBudget.Status.valueOf(status.toUpperCase());
            ResponseEntity<String> result = budgetService.updateBudgetStatus(budgetId, enumStatus);
            response.put("message", result.getBody());
            return new ResponseEntity<>(response, result.getStatusCode());
        } catch (IllegalArgumentException e) {
            response.put("error", "Invalid status: " + status);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            response.put("error", "Failed to update budget status: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{budgetId}/expenses/{expenseId}")
    public ResponseEntity<?> associateExpenseWithBudget(@PathVariable Long budgetId, @PathVariable Long expenseId) {
        try {
            ResponseEntity<String> result = budgetService.associateExpenseWithBudget(budgetId, expenseId);
            Map<String, String> response = new HashMap<>();
            response.put("message", result.getBody());
            return new ResponseEntity<>(response, result.getStatusCode());
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to associate expense with budget: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}