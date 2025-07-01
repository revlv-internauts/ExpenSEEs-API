package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.dto.ExpenseDto;
import com.example.expensetracker.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseService expenseService;

    @PostMapping(consumes = {"multipart/form-data", "application/json"})
    public ResponseEntity<Expense> addExpense(
            @RequestParam(value = "category") String category,
            @RequestParam(value = "amount", required = false) Double amount,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "amountPerUnit", required = false) Double amountPerUnit,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "dateOfTransaction", required = false) String dateOfTransaction,
            @RequestParam(value = "files", required = false) MultipartFile[] files) throws IOException {
        ExpenseDto expenseDto = new ExpenseDto();
        expenseDto.setCategory(category);
        expenseDto.setAmount(amount);
        expenseDto.setQuantity(quantity);
        expenseDto.setAmountPerUnit(amountPerUnit);
        expenseDto.setRemarks(remarks);
        expenseDto.setDateOfTransaction(dateOfTransaction != null ? LocalDate.parse(dateOfTransaction) : null);

        Expense expense = expenseService.addExpense(expenseDto);

        if (files != null && files.length > 0) {
            String uploadDir = "Uploads/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            List<String> imagePaths = new ArrayList<>(expense.getImagePaths());
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String contentType = file.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        throw new IllegalArgumentException("Only image files are allowed");
                    }
                    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    Files.write(filePath, file.getBytes());
                    imagePaths.add(uploadDir + fileName);
                }
            }
            expense.setImagePaths(imagePaths);
            expenseRepository.save(expense);
        }

        return ResponseEntity.ok(expense);
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<Expense> updateExpense(
            @PathVariable Long expenseId,
            @RequestParam(value = "category") String category,
            @RequestParam(value = "amount", required = false) Double amount,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "amountPerUnit", required = false) Double amountPerUnit,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "dateOfTransaction", required = false) String dateOfTransaction,
            @RequestParam(value = "files", required = false) MultipartFile[] files) throws IOException {
        ExpenseDto expenseDto = new ExpenseDto();
        expenseDto.setCategory(category);
        expenseDto.setAmount(amount);
        expenseDto.setQuantity(quantity);
        expenseDto.setAmountPerUnit(amountPerUnit);
        expenseDto.setRemarks(remarks);
        expenseDto.setDateOfTransaction(dateOfTransaction != null ? LocalDate.parse(dateOfTransaction) : null);

        Expense expense = expenseService.updateExpense(expenseId, expenseDto);

        if (files != null && files.length > 0) {
            String uploadDir = "Uploads/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            List<String> imagePaths = new ArrayList<>(expense.getImagePaths());
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String contentType = file.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        throw new IllegalArgumentException("Only image files are allowed");
                    }
                    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    Files.write(filePath, file.getBytes());
                    imagePaths.add(uploadDir + fileName);
                }
            }
            expense.setImagePaths(imagePaths);
            expenseRepository.save(expense);
        }

        return ResponseEntity.ok(expense);
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

    @GetMapping("/{expenseId}/images")
    public ResponseEntity<List<String>> getExpenseImages(@PathVariable Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        return ResponseEntity.ok(expense.getImagePaths());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Expense>> getRecentTransactions(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(expenseService.getRecentTransactions(limit));
    }
}