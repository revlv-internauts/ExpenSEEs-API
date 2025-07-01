package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.dto.ExpenseDto;
import com.example.expensetracker.service.ExpenseService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseService expenseService;

    @PostMapping(consumes = {"multipart/form-data", "application/json"})
    @Transactional
    public ResponseEntity<Expense> addExpense(
            @RequestParam(value = "category") String category,
            @RequestParam(value = "amount", required = false) Double amount,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "amountPerUnit", required = false) Double amountPerUnit,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "dateOfTransaction", required = false) String dateOfTransaction,
            @RequestParam(value = "files") MultipartFile[] files) throws IOException {
        // Validate files before creating expense
        if (files == null || files.length == 0 || files[0].isEmpty()) {
            throw new IllegalArgumentException("At least one image file is required");
        }

        String uploadDir = "Uploads/";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        List<String> imagePaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IllegalArgumentException("Only image files are allowed");
                }
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image == null) {
                    throw new IllegalArgumentException("Invalid image file: " + file.getOriginalFilename());
                }
                String fileName = UUID.randomUUID() + ".jpg";
                Path filePath = uploadPath.resolve(fileName);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                Files.write(filePath, baos.toByteArray());

                imagePaths.add(uploadDir + fileName);
            }
        }

        ExpenseDto expenseDto = new ExpenseDto();
        expenseDto.setCategory(category);
        expenseDto.setAmount(amount);
        expenseDto.setQuantity(quantity);
        expenseDto.setAmountPerUnit(amountPerUnit);
        expenseDto.setRemarks(remarks);
        expenseDto.setDateOfTransaction(dateOfTransaction != null ? LocalDate.parse(dateOfTransaction) : null);

        Expense expense = expenseService.addExpense(expenseDto);
        expense.setImagePaths(imagePaths);
        expenseRepository.save(expense);

        return ResponseEntity.ok(expense);
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @PutMapping("/{expenseId}")
    @Transactional
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

        if (files != null && files.length > 0 && !files[0].isEmpty()) {
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
                    BufferedImage image = ImageIO.read(file.getInputStream());
                    if (image == null) {
                        throw new IllegalArgumentException("Invalid image file: " + file.getOriginalFilename());
                    }
                    String fileName = UUID.randomUUID() + ".jpg";
                    Path filePath = uploadPath.resolve(fileName);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos);
                    Files.write(filePath, baos.toByteArray());

                    imagePaths.add(uploadDir + fileName);
                }
            }
            expense.setImagePaths(imagePaths);
            expenseRepository.save(expense);
        }

        return ResponseEntity.ok(expense);
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Map<String, String>> deleteExpense(@PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Expense deleted successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteExpenses(@RequestBody List<Long> expenseIds) {
        expenseService.deleteExpenses(expenseIds);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Expenses deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/total-amount")
    public ResponseEntity<Map<String, Double>> getTotalExpenseAmount() {
        Map<String, Double> response = new HashMap<>();
        response.put("totalAmount", expenseService.getTotalExpenseAmount());
        return ResponseEntity.ok(response);
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
        Expense expense = expenseService.getExpenseById(expenseId);
        return ResponseEntity.ok(expense.getImagePaths());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Expense>> getRecentTransactions(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(expenseService.getRecentTransactions(limit));
    }
}