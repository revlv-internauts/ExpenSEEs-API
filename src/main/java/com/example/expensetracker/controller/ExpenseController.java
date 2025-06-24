package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.dto.ExpenseDto;
import com.example.expensetracker.service.ExpenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
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

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpense(@PathVariable Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found with id: " + id));
        return ResponseEntity.ok(expense);
    }

    @PostMapping
    public ResponseEntity<Expense> addExpense(@RequestBody ExpenseDto expenseDto) {
        return ResponseEntity.ok(expenseService.addExpense(expenseDto));
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@PathVariable Long id, @RequestBody ExpenseDto expenseDto) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found with id: " + id));
        expense.setCategory(expenseDto.getCategory());
        expense.setAmount(expenseDto.calculateTotal());
        expense.setComments(expenseDto.getComments());
        expense.setDateOfTransaction(expenseDto.getDateOfTransaction() != null ? expenseDto.getDateOfTransaction() : java.time.LocalDate.now());
        expense.setImagePath(expenseDto.getImagePath());
        expense.setUpdatedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(expenseRepository.save(expense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExpense(@PathVariable Long id) {
        if (!expenseRepository.existsById(id)) {
            return new ResponseEntity<>("Expense not found with id: " + id, HttpStatus.NOT_FOUND);
        }
        expenseRepository.deleteById(id);
        return ResponseEntity.ok("Expense deleted successfully");
    }

    @DeleteMapping
    public ResponseEntity<String> deleteExpenses(@RequestBody List<Long> ids) {
        expenseRepository.deleteAllById(ids);
        return ResponseEntity.ok("Expenses deleted successfully");
    }

    @GetMapping("/total-amount")
    public ResponseEntity<Double> getTotalExpenseAmount() {
        return ResponseEntity.ok(expenseService.getTotalExpenseAmount());
    }

    @GetMapping("/distribution")
    public ResponseEntity<Map<String, Double>> getExpenseDistribution() {
        return ResponseEntity.ok(expenseService.getExpenseDistributionByCategory());
    }

    @GetMapping("/top")
    public ResponseEntity<List<Expense>> getTopExpenses() {
        List<Expense> expenses = expenseService.getAllExpenses();
        return ResponseEntity.ok(expenses.stream()
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(5)
                .collect(Collectors.toList()));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadExpenseWithImage(
            @RequestPart("expense") String expenseJson,
            @RequestPart("receiptImage") MultipartFile image) {
        try {
            // Parse the JSON string into ExpenseDto
            ExpenseDto expenseDto = objectMapper.readValue(expenseJson, ExpenseDto.class);

            // Validate and process the expense
            String uploadDir = "uploads/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, image.getBytes());

            // Create and save the expense
            Expense expense = new Expense();
            expense.setCategory(expenseDto.getCategory());
            expense.setAmount(expenseDto.calculateTotal());
            expense.setComments(expenseDto.getComments());
            expense.setDateOfTransaction(expenseDto.getDateOfTransaction() != null ? expenseDto.getDateOfTransaction() : java.time.LocalDate.now());
            expense.setImagePath(uploadDir + fileName);
            expense.setCreatedAt(java.time.LocalDateTime.now());
            expense.setUpdatedAt(java.time.LocalDateTime.now());
            expense.setUser(expenseService.getCurrentUser());

            expenseRepository.save(expense);

            return ResponseEntity.ok("Expense with image uploaded successfully: " + uploadDir + fileName);
        } catch (IOException e) {
            return new ResponseEntity<>("Failed to upload image or process expense: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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