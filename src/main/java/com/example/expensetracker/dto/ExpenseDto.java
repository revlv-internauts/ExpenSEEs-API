package com.example.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDto {
    private String category;
    private Double amount;
    private Integer quantity;
    private Double amountPerUnit;
    private String remarks;
    private LocalDate dateOfTransaction;
    private String imagePath;

    public Double calculateTotal() {
        if (quantity != null && amountPerUnit != null) {
            return quantity * amountPerUnit;
        }
        return amount != null ? amount : 0.0;
    }
}