package com.example.expensetracker.controller;

import com.example.expensetracker.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class LiquidationReportController {

    @Autowired
    private ExpenseService expenseService;

    @GetMapping("/liquidation")
    public ResponseEntity<Map<String, Object>> getLiquidationReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("totalExpenses", expenseService.getTotalExpenseAmount());
        report.put("expenses", expenseService.getAllExpenses());
        return ResponseEntity.ok(report);
    }
}