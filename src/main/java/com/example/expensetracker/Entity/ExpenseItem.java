package com.example.expensetracker.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double amountPerUnit;

    private String remarks;

    @ManyToOne
    @JoinColumn(name = "budget_id")
    @JsonBackReference
    private SubmittedBudget budget;
}
