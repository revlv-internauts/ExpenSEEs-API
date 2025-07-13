package com.example.expensetracker.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LiquidationExpenseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long expenseId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Double amount;

    private String remarks;

    @Column(nullable = false)
    private LocalDate dateOfTransaction;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "liquidation_expense_images", joinColumns = @JoinColumn(name = "expense_id"))
    @Column(name = "image_path")
    private List<String> imagePaths = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "liquidation_id")
    @JsonBackReference
    private Liquidation budget;

    @JsonProperty("username")
    public String getUsername() {
        return budget != null && budget.getUser() != null ? budget.getUser().getUsername() : "Unknown";
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}