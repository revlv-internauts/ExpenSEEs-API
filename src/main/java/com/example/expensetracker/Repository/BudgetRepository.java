package com.example.expensetracker.Repository;

import com.example.expensetracker.Entity.SubmittedBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetRepository extends JpaRepository<SubmittedBudget, Long> {
}

