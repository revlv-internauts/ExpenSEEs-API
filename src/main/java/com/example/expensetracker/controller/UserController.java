package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.service.UserService;
import com.example.expensetracker.util.ChangePassword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ChangePassword changePassword,
                                                             @RequestParam("email") String email) {
        Map<String, String> response = new HashMap<>();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    response.put("error", "User not found");
                    return new IllegalArgumentException("User not found");
                });
        if (!Objects.equals(changePassword.password(), changePassword.repeatPassword())) {
            response.put("error", "Passwords do not match");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email, encodedPassword);
        response.put("message", "Password has been changed!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserWithDetails(@PathVariable Long userId) {
        Map<String, String> response = new HashMap<>();
        ResponseEntity<User> result = userService.getUserWithDetails(userId);
        if (result.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.ok(result.getBody());
        } else {
            response.put("error", "User not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }
}