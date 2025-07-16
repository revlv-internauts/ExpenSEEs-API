package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.Expense;
import com.example.expensetracker.Repository.ExpenseRepository;
import com.example.expensetracker.dto.ExpenseDto;
import com.example.expensetracker.service.ExpenseService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
    public ResponseEntity<?> addExpense(
            @RequestParam(value = "category") String category,
            @RequestParam(value = "amount", required = false) Double amount,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "amountPerUnit", required = false) Double amountPerUnit,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "dateOfTransaction", required = false) String dateOfTransaction,
            @RequestParam(value = "files") MultipartFile[] files) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (files == null || files.length == 0 || files[0].isEmpty()) {
                response.put("error", "At least one image file is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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
                        response.put("error", "Only image files are allowed");
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }
                    BufferedImage image = ImageIO.read(file.getInputStream());
                    if (image == null) {
                        response.put("error", "Invalid image file: " + file.getOriginalFilename());
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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
        } catch (IOException e) {
            response.put("error", "Failed to process file upload: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{expenseId}")
    @Transactional
    public ResponseEntity<?> updateExpense(
            @PathVariable Long expenseId,
            @RequestParam(value = "category") String category,
            @RequestParam(value = "amount", required = false) Double amount,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "amountPerUnit", required = false) Double amountPerUnit,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "dateOfTransaction", required = false) String dateOfTransaction,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        Map<String, Object> response = new HashMap<>();

        try {
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

                List<String> imagePaths = new ArrayList<>(expense.getImagePaths() != null ? expense.getImagePaths() : new ArrayList<>());
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String contentType = file.getContentType();
                        if (contentType == null || !contentType.startsWith("image/")) {
                            response.put("error", "Only image files are allowed");
                            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                        }
                        BufferedImage image = ImageIO.read(file.getInputStream());
                        if (image == null) {
                            response.put("error", "Invalid image file: " + file.getOriginalFilename());
                            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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
        } catch (IOException e) {
            response.put("error", "Failed to process file upload: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses(
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        String effectiveSortBy = sortBy.equals("date") ? "createdAt" : sortBy;
        return ResponseEntity.ok(expenseService.getAllExpenses(effectiveSortBy, sortOrder));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<Expense>> getAllExpensesForAdmin(
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        // Map 'date' to 'createdAt' for sorting
        String effectiveSortBy = sortBy.equals("date") ? "createdAt" : sortBy;
        return ResponseEntity.ok(expenseService.getAllExpensesForAdmin(effectiveSortBy, sortOrder));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Map<String, String>> deleteExpense(@PathVariable Long expenseId) {
        Map<String, String> response = new HashMap<>();
        try {
            expenseService.deleteExpense(expenseId);
            response.put("message", "Expense deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteExpenses(@RequestBody List<Long> expenseIds) {
        Map<String, String> response = new HashMap<>();
        try {
            expenseService.deleteExpenses(expenseIds);
            response.put("message", "Expenses deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
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
        List<Expense> expenses = expenseService.getAllExpenses("amount", "desc"); // Pass sortBy and sortOrder
        List<Expense> topExpenses = expenses.stream()
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(5)
                .collect(Collectors.toList());
        return ResponseEntity.ok(topExpenses);
    }

    @GetMapping("/{expenseId}/images")
    public ResponseEntity<?> getExpenseImages(@PathVariable Long expenseId, @RequestParam(value = "index", defaultValue = "0") int index) {
        Map<String, Object> response = new HashMap<>();
        try {
            Expense expense = expenseService.getExpenseById(expenseId);
            List<String> imagePaths = expense.getImagePaths();

            if (imagePaths.isEmpty() || index < 0 || index >= imagePaths.size()) {
                response.put("error", "No image found at index " + index + " for expense " + expenseId);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            String imagePath = imagePaths.get(index);
            File file = new File(imagePath);
            if (!file.exists() || !file.isFile()) {
                response.put("error", "Image file not found: " + imagePath);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "image/jpeg"; // Default to JPEG
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (IOException e) {
            response.put("error", "Failed to retrieve image: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Expense>> getRecentTransactions(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(expenseService.getRecentTransactions(limit));
    }
}