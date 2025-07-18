package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.Liquidation;
import com.example.expensetracker.Entity.LiquidationExpenseItem;
import com.example.expensetracker.Entity.SubmittedBudget;
import com.example.expensetracker.dto.LiquidationExpenseItemDto;
import com.example.expensetracker.service.LiquidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/liquidation")
@Validated
public class LiquidationReportController {

    @Autowired
    private LiquidationService liquidationService;

    @Autowired
    private Validator validator;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> createLiquidation(
            @RequestParam(value = "expenses") String expensesJson,
            @RequestParam(value = "expenses[0].files", required = false) MultipartFile[] files0,
            @RequestParam(value = "expenses[1].files", required = false) MultipartFile[] files1,
            @RequestParam(value = "expenses[2].files", required = false) MultipartFile[] files2,
            @RequestParam(value = "expenses[3].files", required = false) MultipartFile[] files3,
            @RequestParam Long budgetId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Parse expenses JSON
            ObjectMapper mapper = new ObjectMapper();
            List<LiquidationExpenseItemDto> expenseDtos = mapper.readValue(
                    expensesJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, LiquidationExpenseItemDto.class)
            );

            if (expenseDtos == null || expenseDtos.isEmpty()) {
                response.put("error", "At least one expense is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Validate each expense DTO
            for (LiquidationExpenseItemDto dto : expenseDtos) {
                var violations = validator.validate(dto);
                if (!violations.isEmpty()) {
                    StringBuilder errorMessage = new StringBuilder("Validation errors: ");
                    violations.forEach(v -> errorMessage.append(v.getMessage()).append("; "));
                    response.put("error", errorMessage.toString());
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }

            // Create liquidation
            Liquidation liquidation = new Liquidation();
            liquidation.setDateOfTransaction(expenseDtos.get(0).getDateOfTransaction() != null
                    ? LocalDate.parse(expenseDtos.get(0).getDateOfTransaction())
                    : LocalDate.now());

            // Create expense items and associate files
            List<LiquidationExpenseItem> expenses = new ArrayList<>();
            MultipartFile[][] allFiles = {files0, files1, files2, files3}; // Support up to 4 expenses
            for (int i = 0; i < expenseDtos.size(); i++) {
                LiquidationExpenseItemDto dto = expenseDtos.get(i);
                LiquidationExpenseItem expense = new LiquidationExpenseItem();
                expense.setCategory(dto.getCategory());
                expense.setAmount(dto.getAmount());
                expense.setRemarks(dto.getRemarks() != null ? dto.getRemarks() : "");
                expense.setDateOfTransaction(LocalDate.parse(dto.getDateOfTransaction()));

                // Handle file uploads for this expense
                List<String> imagePaths = new ArrayList<>();
                MultipartFile[] files = i < allFiles.length ? allFiles[i] : null;
                if (files != null && files.length > 0 && !files[0].isEmpty()) {
                    String uploadDir = "Uploads/";
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    for (MultipartFile file : files) {
                        if (!file.isEmpty()) {
                            String contentType = file.getContentType();
                            if (contentType == null || !contentType.startsWith("image/")) {
                                response.put("error", "Only image files are allowed for expense " + i);
                                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                            }
                            BufferedImage image = ImageIO.read(file.getInputStream());
                            if (image == null) {
                                response.put("error", "Invalid image file for expense " + i + ": " + file.getOriginalFilename());
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
                }
                expense.setImagePaths(imagePaths); // Store image paths as JSON
                expenses.add(expense);
            }

            // Create liquidation with budgetId
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

    @GetMapping
    public ResponseEntity<?> getLiquidationReport(
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            String effectiveSortBy = sortBy.equals("date") ? "createdAt" : sortBy;
            List<Liquidation> liquidations = liquidationService.getAllLiquidations(effectiveSortBy, sortOrder);
            List<Map<String, Object>> responseList = liquidations.stream().map(liquidation -> {
                Map<String, Object> liquidationMap = new HashMap<>();
                SubmittedBudget budget = liquidation.getSubmittedBudget();
                liquidationMap.put("liquidationId", liquidation.getLiquidationId());
                liquidationMap.put("budgetId", budget != null ? budget.getBudgetId() : null);
                liquidationMap.put("budgetName", budget != null ? budget.getName() : "Unknown");
                liquidationMap.put("amount", budget != null ? budget.getTotal() : 0.0);
                liquidationMap.put("totalSpent", liquidation.getTotalSpent());
                liquidationMap.put("remainingBalance", liquidation.getRemainingBalance());
                liquidationMap.put("status", liquidation.getStatus().toString());
                liquidationMap.put("remarks", liquidation.getRemarks());
                liquidationMap.put("dateOfTransaction", liquidation.getDateOfTransaction());
                liquidationMap.put("createdAt", liquidation.getCreatedAt());
                liquidationMap.put("expenses", liquidation.getExpenses());
                liquidationMap.put("username", liquidation.getUsername());
                return liquidationMap;
            }).collect(Collectors.toList());
            Map<String, Object> report = new HashMap<>();
            report.put("budgets", responseList);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve liquidations: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
            SubmittedBudget budget = liquidation.getSubmittedBudget();
            response.put("liquidationId", liquidation.getLiquidationId());
            response.put("budgetId", budget != null ? budget.getBudgetId() : null);
            response.put("budgetName", budget != null ? budget.getName() : "Unknown");
            response.put("amount", budget != null ? budget.getTotal() : 0.0);
            response.put("totalSpent", liquidation.getTotalSpent());
            response.put("remainingBalance", liquidation.getRemainingBalance());
            response.put("status", liquidation.getStatus().toString());
            response.put("remarks", liquidation.getRemarks());
            response.put("dateOfTransaction", liquidation.getDateOfTransaction());
            response.put("createdAt", liquidation.getCreatedAt());
            response.put("expenses", liquidation.getExpenses());
            response.put("username", liquidation.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to retrieve liquidation: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{liquidationExpenseId}/images")
    public ResponseEntity<?> getLiquidationImages(@PathVariable Long liquidationExpenseId) {
        Map<String, Object> response = new HashMap<>();
        try {
            LiquidationExpenseItem expense = liquidationService.getLiquidationExpenseById(liquidationExpenseId);
            if (expense == null) {
                response.put("error", "Liquidation expense not found with ID: " + liquidationExpenseId);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            List<String> imagePaths = expense.getImagePaths();
            if (imagePaths == null || imagePaths.isEmpty()) {
                response.put("error", "No images found for liquidation expense " + liquidationExpenseId);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            String imagePath = imagePaths.get(0);
            File file = new File(imagePath);
            if (!file.exists() || !file.isFile()) {
                response.put("error", "Image file not found: " + imagePath);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (IOException e) {
            response.put("error", "Failed to retrieve image: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.put("error", "Failed to retrieve image: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{liquidationId}/images/{liquidationExpenseId}/{imageIndex}")
    public ResponseEntity<?> getLiquidationImage(
            @PathVariable Long liquidationId,
            @PathVariable Long liquidationExpenseId,
            @PathVariable Integer imageIndex) {
        Map<String, Object> response = new HashMap<>();
        try {
            Liquidation liquidation = liquidationService.getLiquidationById(liquidationId);
            if (liquidation == null) {
                response.put("error", "Liquidation not found with ID: " + liquidationId);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            LiquidationExpenseItem expense = liquidation.getExpenses().stream()
                    .filter(e -> e.getLiquidationExpenseId().equals(liquidationExpenseId))
                    .findFirst()
                    .orElse(null);
            if (expense == null) {
                response.put("error", "Liquidation expense not found with ID: " + liquidationExpenseId);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            List<String> imagePaths = expense.getImagePaths();
            if (imagePaths == null || imageIndex < 0 || imageIndex >= imagePaths.size()) {
                response.put("error", "No image found at index " + imageIndex + " for liquidation expense " + liquidationExpenseId);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            String imagePath = imagePaths.get(imageIndex);
            File file = new File(imagePath);
            if (!file.exists() || !file.isFile()) {
                response.put("error", "Image file not found: " + imagePath);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "image/jpeg";
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

    @PutMapping("/{liquidationId}/status")
    public ResponseEntity<Map<String, String>> updateLiquidationStatus(
            @PathVariable Long liquidationId,
            @RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        try {
            String status = request.get("status");
            String remarks = request.get("remarks");
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

    @DeleteMapping("/{liquidationId}")
    public ResponseEntity<Map<String, String>> deleteLiquidation(@PathVariable Long liquidationId) {
        Map<String, String> response = new HashMap<>();
        try {
            ResponseEntity<String> result = liquidationService.deleteLiquidation(liquidationId);
            response.put("message", result.getBody());
            return new ResponseEntity<>(response, result.getStatusCode());
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            response.put("error", "Failed to delete liquidation: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}