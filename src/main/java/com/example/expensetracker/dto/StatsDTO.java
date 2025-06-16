package com.example.expensetracker.dto;

import com.example.expensetracker.entity.Expense;
import lombok.Data;

@Data
public class StatsDTO {
    private Double expense;
    private Expense latestExpense;
    private Double minExpense;
    private Double maxExpense;
}