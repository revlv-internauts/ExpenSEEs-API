package com.example.expensetracker.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/funds")
public class FundRequestController {

    @PostMapping("/request")
    public ResponseEntity<String> requestFund(@RequestParam String budgetName) {
        return ResponseEntity.ok("Fund request for " + budgetName + " submitted successfully");
    }
}