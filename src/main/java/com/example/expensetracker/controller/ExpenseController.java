package com.example.expensetracker.controller;

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
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
    public ResponseEntity<Expense> addExpense(@RequestBody ExpenseDto expenseDto) {
        return ResponseEntity.ok(expenseService.addExpense(expenseDto));
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<Expense> updateExpense(@PathVariable Long expenseId, @RequestBody ExpenseDto expenseDto) {
        return ResponseEntity.ok(expenseService.updateExpense(expenseId, expenseDto));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<String> deleteExpense(@PathVariable Long expenseId) {
        expenseRepository.deleteById(expenseId);
        return ResponseEntity.ok("Expense deleted successfully");
    }

    @DeleteMapping
    public ResponseEntity<String> deleteExpenses(@RequestBody List<Long> expenseIds) {
        expenseRepository.deleteAllById(expenseIds);
        return ResponseEntity.ok("Expenses deleted successfully");
    }

    @GetMapping("/total-amount")
    public ResponseEntity<String> getTotalExpenseAmount() {
        double total = expenseService.getTotalExpenseAmount();
        return ResponseEntity.ok(new DecimalFormat("#,###.##").format(total));
    }

    @GetMapping("/distribution")
    public ResponseEntity<Map<String, String>> getExpenseDistribution() {
        Map<String, Double> distribution = expenseService.getExpenseDistributionByCategory();
        Map<String, String> formattedDistribution = distribution.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new DecimalFormat("#,###.##").format(entry.getValue())
                ));
        return ResponseEntity.ok(formattedDistribution);
    }

    @GetMapping("/top")
    public ResponseEntity<List<Expense>> getTopExpenses() {
        List<Expense> expenses = expenseService.getAllExpenses();
        return ResponseEntity.ok(expenses.stream()
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(5)
                .collect(Collectors.toList()));
    }

    @PostMapping("/upload-image/{expenseId}")
    public ResponseEntity<String> uploadImage(@PathVariable Long expenseId, @RequestParam("file") MultipartFile file) {
        try {
            String uploadDir = "uploads/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            Expense expense = expenseRepository.findById(expenseId)
                    .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
            expense.setImagePath(uploadDir + fileName);
            expenseRepository.save(expense);

            return ResponseEntity.ok("Image uploaded successfully: " + uploadDir + fileName);
        } catch (IOException e) {
            return new ResponseEntity<>("Failed to upload image", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) throws IOException {
        Path filePath = Paths.get("uploads/" + filename);
        if (!Files.exists(filePath)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        byte[] imageBytes = Files.readAllBytes(filePath);
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .body(imageBytes);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Expense>> getRecentTransactions(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(expenseService.getRecentTransactions(limit));
    }
}
