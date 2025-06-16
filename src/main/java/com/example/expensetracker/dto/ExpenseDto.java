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

    @Column(nullable = false)
    private Double amount;

    private String comments;

}
