package com.example.expensetracker.controller;

import com.example.expensetracker.dto.GraphDTO;
import com.example.expensetracker.entity.User;
import com.example.expensetracker.services.stats.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@CrossOrigin("*")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/chart")
    public ResponseEntity<GraphDTO> getChartDetails(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(statsService.getChartData(user));
    }

    @GetMapping
    public ResponseEntity<?> getStats(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(statsService.getStats(user));
    }
}