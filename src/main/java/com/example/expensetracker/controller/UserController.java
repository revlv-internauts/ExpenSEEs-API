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
    public ResponseEntity<String> resetPassword(@RequestBody ChangePassword changePassword,
                                                @RequestParam("email") String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!Objects.equals(changePassword.password(), changePassword.repeatPassword())) {
            return new ResponseEntity<>("Passwords do not match", HttpStatus.BAD_REQUEST);
        }
        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email, encodedPassword);
        return ResponseEntity.ok("Password has been changed!");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserWithDetails(@PathVariable Long userId) {
        return userService.getUserWithDetails(userId);
    }
}