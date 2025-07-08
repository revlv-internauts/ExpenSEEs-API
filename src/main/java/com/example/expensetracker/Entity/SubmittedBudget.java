package com.example.expensetracker.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmittedBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long budgetId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ExpenseItem> expenses = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    public enum Status {
        PENDING, APPROVED, DENIED
    }

    @JsonProperty("username")
    public String getUsername() {
        return user != null ? user.getUsername() : "Unknown";
    }
}