package com.example.expensetracker.Repository;

import com.example.expensetracker.Entity.ExpenseItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseItemRepository extends JpaRepository<ExpenseItem, Long> {
}
