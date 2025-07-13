package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.Liquidation;
import com.example.expensetracker.Entity.LiquidationExpenseItem;
import com.example.expensetracker.service.LiquidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/liquidation")
public class LiquidationReportController {

    @Autowired
    private LiquidationService liquidationService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> createLiquidation(
            @RequestParam("category") String category,
            @RequestParam("amount") Double amount,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam("dateOfTransaction") String dateOfTransaction,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam Long budgetId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate expense input
            if (category == null || category.trim().isEmpty()) {
                response.put("error", "Category is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (amount == null || amount <= 0) {
                response.put("error", "Amount must be a positive number");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            LocalDate expenseDate = LocalDate.parse(dateOfTransaction);

            // Create liquidation
            Liquidation liquidation = new Liquidation();
            liquidation.setDateOfTransaction(expenseDate);

            // Create expense item
            List<LiquidationExpenseItem> expenses = new ArrayList<>();
            LiquidationExpenseItem expense = new LiquidationExpenseItem();
            expense.setCategory(category);
            expense.setAmount(amount);
            expense.setRemarks(remarks != null ? remarks : "");
            expense.setDateOfTransaction(expenseDate);
            expenses.add(expense);

            // Handle file uploads
            if (files != null && files.length > 0 && !files[0].isEmpty()) {
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
                expense.setImagePaths(imagePaths);
            }

            // Create liquidation
            Liquidation createdLiquidation = liquidationService.createLiquidation(liquidation, budgetId, expenses);
            return ResponseEntity.ok(createdLiquidation);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            response.put("error", "Failed to process file upload: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.put("error", "Failed to create liquidation: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{liquidationId}")
    public ResponseEntity<?> getLiquidationById(@PathVariable Long liquidationId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Liquidation liquidation = liquidationService.getLiquidationById(liquidationId);
            if (liquidation == null) {
                response.put("error", "Liquidation not found with ID: " + liquidationId);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.ok(liquidation);
        } catch (Exception e) {
            response.put("error", "Failed to retrieve liquidation: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<?> getLiquidationReport() {
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("budgets", liquidationService.getAllLiquidations());
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve liquidations: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{liquidationId}/status")
    public ResponseEntity<Map<String, String>> updateLiquidationStatus(@PathVariable Long liquidationId, @RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        try {
            String status = request.get("status");
            String remarks = request.get("remarks"); // Optional remarks
            if (status == null || status.trim().isEmpty()) {
                response.put("error", "Status is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            Liquidation.Status enumStatus = Liquidation.Status.valueOf(status.toUpperCase());
            ResponseEntity<String> result = liquidationService.updateLiquidationStatus(liquidationId, enumStatus, remarks);
            response.put("message", result.getBody());
            return new ResponseEntity<>(response, result.getStatusCode());
        } catch (IllegalArgumentException e) {
            response.put("error", "Invalid status: " + request.get("status"));
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            response.put("error", "Failed to update liquidation status: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}