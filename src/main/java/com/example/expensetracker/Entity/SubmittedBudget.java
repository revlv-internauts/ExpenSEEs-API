package com.example.expensetracker.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submitted_budget")
@Getter
@Setter
public class SubmittedBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_id")
    private Long budgetId;

    @Column(name = "name")
    private String name;

    @Column(name = "budget_date")
    private LocalDate budgetDate;

    @Column(name = "total")
    private double total;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseItem> expenses = new ArrayList<>();

    @OneToMany(mappedBy = "submittedBudget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Liquidation> liquidations = new ArrayList<>();

    public enum Status {
        PENDING, RELEASED, DENIED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}