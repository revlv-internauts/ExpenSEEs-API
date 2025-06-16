package com.example.expensetracker.dto;

import com.example.expensetracker.entity.Expense;
import com.example.expensetracker.entity.Income;
import lombok.Data;

import java.util.List;

@Data
public class GraphDTO {

    private List<Expense> expenseList;
    private List<Income> incomeList;
}
