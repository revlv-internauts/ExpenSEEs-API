package com.example.expensetracker.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
public class Liquidation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long liquidationId;

    @Column(nullable = false)
    private Double totalSpent;

    @Column(nullable = false)
    private Double remainingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = true)
    private String remarks;

    @Column(nullable = true)
    private LocalDate dateOfTransaction;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<LiquidationExpenseItem> expenses = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "submitted_budget_id")
    @JsonIgnore
    private SubmittedBudget submittedBudget;

    public enum Status {
        PENDING, LIQUIDATED, DENIED
    }

    @JsonProperty("username")
    public String getUsername() {
        return user != null ? user.getUsername() : "Unknown";
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}