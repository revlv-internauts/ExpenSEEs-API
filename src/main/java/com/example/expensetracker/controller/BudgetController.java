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
    public ResponseEntity<?> getAllBudgets(
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            String effectiveSortBy = sortBy.equals("date") ? "createdAt" : sortBy;
            List<SubmittedBudget> budgets = budgetService.getAllBudgets(effectiveSortBy, sortOrder);
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
    public ResponseEntity<Map<String, String>> updateBudgetStatus(@PathVariable Long budgetId, @RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        try {
            String status = request.get("status");
            String remarks = request.get("remarks");
            if (status == null || status.trim().isEmpty()) {
                response.put("error", "Status is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            SubmittedBudget.Status enumStatus = SubmittedBudget.Status.valueOf(status.toUpperCase());
            ResponseEntity<String> result = budgetService.updateBudgetStatus(budgetId, enumStatus, remarks);
            response.put("message", result.getBody());
            return new ResponseEntity<>(response, result.getStatusCode());
        } catch (IllegalArgumentException e) {
            response.put("error", "Invalid status: " + request.get("status"));
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

    @DeleteMapping("/{budgetId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteBudget(@PathVariable Long budgetId) {
        Map<String, String> response = new HashMap<>();
        try {
            ResponseEntity<String> result = budgetService.deleteBudget(budgetId);
            response.put("message", result.getBody());
            return new ResponseEntity<>(response, result.getStatusCode());
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            response.put("error", "Failed to delete budget: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}