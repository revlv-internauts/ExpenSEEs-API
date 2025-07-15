package com.example.expensetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class LiquidationExpenseItemDto {
    @NotBlank(message = "Category is required")
    private String category;

    @Positive(message = "Amount must be positive")
    private Double amount;

    private String remarks;

    @NotBlank(message = "Date of transaction is required")
    private String dateOfTransaction;
}