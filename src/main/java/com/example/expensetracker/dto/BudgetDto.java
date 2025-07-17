package com.example.expensetracker.dto;

import com.example.expensetracker.Entity.SubmittedBudget;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class BudgetDto {
    private Long budgetId;
    private String name;
    private LocalDate budgetDate;
    private double total;
    private SubmittedBudget.Status status;
    private String remarks;
    private LocalDateTime createdAt;
    private String username;
    private Long userId;
    private List<Long> liquidationIds;
}