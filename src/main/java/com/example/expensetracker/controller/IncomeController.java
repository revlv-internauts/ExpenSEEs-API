package com.example.expensetracker.controller;

import com.example.expensetracker.dto.IncomeDTO;
import com.example.expensetracker.entity.Income;
import com.example.expensetracker.entity.User;
import com.example.expensetracker.services.income.IncomeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
@CrossOrigin("*")
public class IncomeController {

    private final IncomeService incomeService;

    @PostMapping
    public ResponseEntity<?> postIncome(@RequestBody IncomeDTO dto, @AuthenticationPrincipal User user) {
        Income createdIncome = incomeService.postIncome(dto, user);
        if (createdIncome != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(createdIncome);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllIncomes(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(incomeService.getAllIncomes(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getIncomeById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(incomeService.getIncomeById(id, user));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateIncome(@PathVariable Long id, @RequestBody IncomeDTO dto, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(incomeService.updateIncome(id, dto, user));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIncome(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            incomeService.deleteIncome(id, user);
            return ResponseEntity.ok(null);
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong");
        }
    }
}