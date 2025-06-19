package com.example.expensetracker.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.dto.ExpenseDto;
import com.example.expensetracker.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseService expenseService;

    @PostMapping
    public Expense addExpense(@RequestBody ExpenseDto expenseDto) {
        if (expenseDto.getAmount() == null && expenseDto.getComments() != null) {
            String[] parts = expenseDto.getComments().split("\\s+");
            if (parts.length >= 2) {
                try {
                    double quantity = Double.parseDouble(parts[0]);
                    double amountPerUnit = Double.parseDouble(parts[1].replace("₱", ""));
                    expenseDto.setAmount(quantity * amountPerUnit);
                } catch (NumberFormatException e) {
                    expenseDto.setAmount(0.0);
                }
            }
        }
        return expenseService.addExpense(expenseDto);
    }

    @GetMapping
    public List<Expense> getAllExpenses() {
        return expenseService.getAllExpenses();
    }

    @PutMapping("/{id}")
    public Expense updateExpense(@PathVariable Long id, @RequestBody ExpenseDto expenseDto) {
        return expenseService.updateExpense(id, expenseDto);
    }

    @DeleteMapping("/{id}")
    public void deleteExpense(@PathVariable Long id) {
        expenseRepository.deleteById(id);
    }

    @GetMapping("/total-amount")
    public Double getTotalExpenseAmount() {
        return expenseService.getTotalExpenseAmount();
    }

    @GetMapping("/distribution")
    public Map<String, Double> getExpenseDistribution() {
        return expenseService.getExpenseDistributionByCategory();
    }

    @GetMapping("/top")
    public List<Expense> getTopExpenses() {
        List<Expense> expenses = expenseService.getAllExpenses();
        return expenses.stream()
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }
}