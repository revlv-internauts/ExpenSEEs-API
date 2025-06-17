package com.example.expensetracker.Entity;

import java.util.List;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses;

    // New fields for password reset
    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_expires_at")
    private java.time.LocalDateTime resetExpiresAt;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}