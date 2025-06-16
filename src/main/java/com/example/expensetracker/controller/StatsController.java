package com.example.expensetracker.controller;

import com.example.expensetracker.dto.ErrorResponse;
import com.example.expensetracker.dto.GraphDTO;
import com.example.expensetracker.dto.StatsDTO;
import com.example.expensetracker.services.stats.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://your-android-app-domain") // Replace with your frontend domain
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/chart")
    public ResponseEntity<?> getChartDetails(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        try {
            GraphDTO graphDTO = statsService.getChartData(days, startDate, endDate);
            if (graphDTO.getExpenseList().isEmpty() && graphDTO.getIncomeList().isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ErrorResponse("No data found for the specified period", HttpStatus.OK.value(), LocalDateTime.now().toString()));
            }
            return ResponseEntity.ok(graphDTO);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now().toString()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value(), LocalDateTime.now().toString()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getStats() {
        try {
            StatsDTO statsDTO = statsService.getStats();
            if (statsDTO.getIncome() == null && statsDTO.getExpense() == null) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ErrorResponse("No income or expense data available", HttpStatus.OK.value(), LocalDateTime.now().toString()));
            }
            return ResponseEntity.ok(statsDTO);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value(), LocalDateTime.now().toString()));
        }
    }
}