package com.example.expensetracker.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(columnDefinition = "TEXT")
    private String imagePaths;

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
}