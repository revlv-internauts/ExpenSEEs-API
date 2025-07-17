package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.User;
import com.example.expensetracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody User user) {
        Map<String, String> response = new HashMap<>();
        ResponseEntity<String> result = userService.createUser(user);
        if (result.getStatusCode() == HttpStatus.CREATED) {
            response.put("message", result.getBody());
        } else {
            response.put("error", result.getBody());
        }
        return new ResponseEntity<>(response, result.getStatusCode());
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        Map<String, String> response = new HashMap<>();
        ResponseEntity<String> result = userService.deleteUser(userId);
        if (result.getStatusCode() == HttpStatus.OK) {
            response.put("message", result.getBody());
        } else {
            response.put("error", result.getBody());
        }
        return new ResponseEntity<>(response, result.getStatusCode());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        List<User> users = userService.getAllUsers(sortBy, sortOrder);
        return ResponseEntity.ok(users);
    }
}