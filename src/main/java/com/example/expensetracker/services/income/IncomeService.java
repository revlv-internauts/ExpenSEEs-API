package com.example.expensetracker.services.income;

import com.example.expensetracker.dto.IncomeDTO;
import com.example.expensetracker.entity.Income;

import java.util.List;

public interface IncomeService {

    Income postIncome(IncomeDTO incomeDTO);

    List<Income> getAllIncomes();
    Income getIncomeById(Long id);
    Income updateIncome(Long id, IncomeDTO incomeDTO);
    void deleteIncome(Long id);

}
