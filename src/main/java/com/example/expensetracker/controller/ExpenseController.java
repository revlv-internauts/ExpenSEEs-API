package com.example.expensetracker.controller;

import java.util.Comparator;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.dto.ExpenseDto;
import com.example.expensetracker.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @DeleteMapping
    public ResponseEntity<String> deleteExpenses(@RequestBody List<Long> ids) {
        expenseRepository.deleteAllById(ids);
        return ResponseEntity.ok("Expenses deleted successfully");
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

    @PostMapping("/upload-image/{expenseId}")
    public String uploadImage(@PathVariable Long expenseId, @RequestParam("file") MultipartFile file) {
        try {
            String uploadDir = "uploads/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            Expense expense = expenseRepository.findById(expenseId).orElseThrow();
            expense.setImagePath(uploadDir + fileName);
            expenseRepository.save(expense);

            return "Image uploaded successfully: " + uploadDir + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) throws IOException {
        Path filePath = Paths.get("uploads/" + filename);
        byte[] imageBytes = Files.readAllBytes(filePath);
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .body(imageBytes);
    }

    @GetMapping("/recent")
    public List<Expense> getRecentTransactions(@RequestParam(defaultValue = "5") int limit) {
        return expenseService.getRecentTransactions(limit);
    }
}