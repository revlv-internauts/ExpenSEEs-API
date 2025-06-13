package com.example.expensetracker.services.stats;

import com.example.expensetracker.dto.GraphDTO;
import com.example.expensetracker.dto.StatsDTO;
import com.example.expensetracker.entity.User;

public interface StatsService {
    GraphDTO getChartData(User user);
    StatsDTO getStats(User user);
}