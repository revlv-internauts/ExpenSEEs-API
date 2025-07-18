package com.example.expensetracker.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private Long liquidationExpenseId;

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

    @Column(columnDefinition = "TEXT")
    private String imagePaths;

    @ManyToOne
    @JoinColumn(name = "liquidation_id")
    @JsonBackReference
    private Liquidation budget;

    @JsonProperty("username")
    public String getUsername() {
        return budget != null && budget.getUser() != null ? budget.getUser().getUsername() : "Unknown";
    }

    public List<String> getImagePaths() {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(imagePaths, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setImagePaths(List<String> imagePaths) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.imagePaths = mapper.writeValueAsString(imagePaths);
        } catch (Exception e) {
            this.imagePaths = "[]";
        }
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