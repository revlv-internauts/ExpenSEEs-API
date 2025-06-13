package com.example.expensetracker.entity;

import com.example.expensetracker.dto.ExpenseDTO;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String category;
    private LocalDate date;
    private Integer amount;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public ExpenseDTO getExpenseDTO() {
        ExpenseDTO expenseDTO = new ExpenseDTO();
        expenseDTO.setId(id);
        expenseDTO.setTitle(title);
        expenseDTO.setAmount(amount);
        expenseDTO.setCategory(category);
        expenseDTO.setDescription(description);
        expenseDTO.setDate(date);
        return expenseDTO;
    }
}