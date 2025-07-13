package com.example.expensetracker.Repository;

import com.example.expensetracker.Entity.Liquidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiquidationRepository extends JpaRepository<Liquidation, Long> {
}