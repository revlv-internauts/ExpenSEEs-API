package com.example.expensetracker.controller;

import com.example.expensetracker.service.ExpenseService;
import com.example.expensetracker.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class LiquidationReportController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private BudgetService budgetService;

    @GetMapping("/liquidation")
    public ResponseEntity<Map<String, Object>> getLiquidationReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("totalExpenses", expenseService.getTotalExpenseAmount());
        report.put("expenses", expenseService.getAllExpenses());
        report.put("budgets", budgetService.getAllBudgets());
        return ResponseEntity.ok(report);
    }
}