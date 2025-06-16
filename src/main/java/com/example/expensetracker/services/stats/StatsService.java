package com.example.expensetracker.services.stats;

import com.example.expensetracker.dto.GraphDTO;
import com.example.expensetracker.dto.StatsDTO;

import java.time.LocalDate;

public interface StatsService {
    GraphDTO getChartData(int days, LocalDate startDate, LocalDate endDate);
    StatsDTO getStats();
}