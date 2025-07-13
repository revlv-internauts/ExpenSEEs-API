package com.example.expensetracker.Repository;

import com.example.expensetracker.Entity.LiquidationExpenseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiquidationExpenseItemRepository extends JpaRepository<LiquidationExpenseItem, Long> {
}