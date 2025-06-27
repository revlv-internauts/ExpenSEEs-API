package com.example.expensetracker.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long expenseId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Double amount;

    private String remarks;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDate dateOfTransaction;
    private String imagePath;

    // Method to format amount with comma as thousand separator
    @JsonProperty("amount")
    public String getFormattedAmount() {
        DecimalFormat formatter = new DecimalFormat("#,###.##");
        return formatter.format(amount);
    }
}
