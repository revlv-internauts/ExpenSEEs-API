package com.example.expensetracker.controller;

import java.util.Comparator;
import java.util.List;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.dto.ExpenseDto;
import com.example.expensetracker.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseService expenseService;

    @PostMapping
    public Expense addExpense(@RequestBody ExpenseDto expenseDto) {
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

    // New endpoint for uploading image
    @PostMapping("/upload-image/{expenseId}")
    public String uploadImage(@PathVariable Long expenseId, @RequestParam("file") MultipartFile file) {
        try {
            // Define upload directory (e.g., ./uploads)
            String uploadDir = "uploads/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique file name
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            // Update expense with image path
            Expense expense = expenseRepository.findById(expenseId).orElseThrow();
            expense.setImagePath(uploadDir + fileName);
            expenseRepository.save(expense);

            return "Image uploaded successfully: " + uploadDir + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    // New endpoint for recent transactions
    @GetMapping("/recent")
    public List<Expense> getRecentTransactions(@RequestParam(defaultValue = "5") int limit) {
        return expenseService.getRecentTransactions(limit);
    }
}