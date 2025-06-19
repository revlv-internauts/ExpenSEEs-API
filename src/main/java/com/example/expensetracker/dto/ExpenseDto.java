package com.example.expensetracker.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDto {
    private String category;
    private Double amount;
    private Integer quantity;
    private Double amountPerUnit;
    private String comments;

    public Double calculateTotal() {
        if (quantity != null && amountPerUnit != null) {
            return quantity * amountPerUnit;
        }
        return amount != null ? amount : 0.0;
    }
}