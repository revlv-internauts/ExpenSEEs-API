package com.example.expensetracker.service;

import com.example.expensetracker.Entity.Liquidation;
import com.example.expensetracker.Entity.LiquidationExpenseItem;
import com.example.expensetracker.Entity.SubmittedBudget;
import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.LiquidationExpenseItemRepository;
import com.example.expensetracker.Repository.LiquidationRepository;
import com.example.expensetracker.Repository.SubmittedBudgetRepository;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.exception.UnauthorizedAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import jakarta.transaction.Transactional;
import java.io.IOException; // Added import
import java.nio.file.Files; // Added import
import java.nio.file.Paths; // Added import

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LiquidationService {

    @Autowired
    private LiquidationRepository liquidationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubmittedBudgetRepository submittedBudgetRepository;

    @Autowired
    private LiquidationExpenseItemRepository liquidationExpenseItemRepository;

    public Liquidation createLiquidation(Liquidation liquidation, Long budgetId, List<LiquidationExpenseItem> expenses) {
        if (liquidation.getDateOfTransaction() == null) {
            throw new IllegalArgumentException("Date of transaction is required");
        }
        if (expenses == null || expenses.isEmpty()) {
            throw new IllegalArgumentException("Liquidation must include at least one expense item");
        }
        if (budgetId == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }

        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        SubmittedBudget submittedBudget = submittedBudgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Submitted Budget not found: " + budgetId));
        if (submittedBudget.getStatus() != SubmittedBudget.Status.RELEASED) {
            throw new IllegalArgumentException("Submitted Budget must have RELEASED status to create a liquidation");
        }
        if (!submittedBudget.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Submitted Budget does not belong to the current user");
        }

        liquidation.setUser(user);
        liquidation.setSubmittedBudget(submittedBudget);
        double totalSpent = expenses.stream()
                .mapToDouble(item -> {
                    if (item.getAmount() <= 0) {
                        throw new IllegalArgumentException("Expense item amount must be positive");
                    }
                    return item.getAmount();
                })
                .sum();
        liquidation.setTotalSpent(totalSpent);
        double remainingBalance = submittedBudget.getTotal() - totalSpent;
        liquidation.setRemainingBalance(remainingBalance);
        liquidation.setStatus(Liquidation.Status.PENDING);
        liquidation.setExpenses(expenses.stream()
                .peek(item -> {
                    item.setBudget(liquidation);
                    if (item.getCreatedAt() == null) item.setCreatedAt(LocalDateTime.now());
                    if (item.getUpdatedAt() == null) item.setUpdatedAt(LocalDateTime.now());
                })
                .toList());
        return liquidationRepository.save(liquidation);
    }

    public List<Liquidation> getAllLiquidations(String sortBy, String sortOrder) {
        User user = userRepository.findByUsername(getCurrentUsername());
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        String sortField;
        switch (sortBy.toLowerCase()) {
            case "date":
                sortField = "createdAt";
                break;
            case "username":
                sortField = "user.username";
                break;
            case "amount":
                sortField = "submittedBudget.total";
                break;
            case "status":
                sortField = "status";
                break;
            default:
                sortField = "createdAt";
        }

        Sort sort = Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
        List<Liquidation> liquidations = liquidationRepository.findAll(sort);

        if (!isAdmin) {
            liquidations = liquidations.stream()
                    .filter(liquidation -> liquidation.getUser().getUserId().equals(user.getUserId()))
                    .toList();
        }
        return liquidations;
    }

    @Transactional
    public ResponseEntity<String> updateLiquidationStatus(Long liquidationId, Liquidation.Status status, String remarks) {
        if (liquidationId == null) {
            return new ResponseEntity<>("Liquidation ID is required", HttpStatus.BAD_REQUEST);
        }
        User currentUser = userRepository.findByUsername(getCurrentUsername());
        if (currentUser == null) {
            return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED);
        }
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return new ResponseEntity<>("Only admins can update liquidation status", HttpStatus.FORBIDDEN);
        }
        if (liquidationRepository.findById(liquidationId).isEmpty()) {
            return new ResponseEntity<>("Liquidation not found", HttpStatus.NOT_FOUND);
        }
        Liquidation liquidation = liquidationRepository.findById(liquidationId).get();
        liquidation.setStatus(status);
        liquidation.setRemarks(remarks);
        liquidationRepository.save(liquidation);
        return new ResponseEntity<>("Status updated successfully", HttpStatus.OK);
    }

    public Liquidation getLiquidationById(Long liquidationId) {
        Liquidation liquidation = liquidationRepository.findById(liquidationId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidation not found with ID: " + liquidationId));
        String currentUsername = getCurrentUsername();
        User currentUser = userRepository.findByUsername(currentUsername);
        if (!liquidation.getUser().getUserId().equals(currentUser.getUserId()) &&
                !SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                        .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            throw new UnauthorizedAccessException("Unauthorized access to liquidation");
        }
        return liquidation;
    }

    public LiquidationExpenseItem getLiquidationExpenseById(Long liquidationExpenseId) {
        LiquidationExpenseItem expense = liquidationExpenseItemRepository.findById(liquidationExpenseId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidation expense not found with ID: " + liquidationExpenseId));
        Liquidation liquidation = expense.getBudget();
        String currentUsername = getCurrentUsername();
        User currentUser = userRepository.findByUsername(currentUsername);
        if (!liquidation.getUser().getUserId().equals(currentUser.getUserId()) &&
                !SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                        .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            throw new UnauthorizedAccessException("Unauthorized access to liquidation expense");
        }
        return expense;
    }

    @Transactional
    public ResponseEntity<String> deleteLiquidation(Long liquidationId) {
        if (liquidationId == null) {
            return new ResponseEntity<>("Liquidation ID is required", HttpStatus.BAD_REQUEST);
        }
        User currentUser = userRepository.findByUsername(getCurrentUsername());
        if (currentUser == null) {
            return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED);
        }
        if (liquidationRepository.findById(liquidationId).isEmpty()) {
            return new ResponseEntity<>("Liquidation not found", HttpStatus.NOT_FOUND);
        }
        Liquidation liquidation = liquidationRepository.findById(liquidationId).get();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !liquidation.getUser().getUserId().equals(currentUser.getUserId())) {
            return new ResponseEntity<>("Unauthorized: You can only delete your own liquidations", HttpStatus.FORBIDDEN);
        }
        liquidation.getExpenses().forEach(expense -> {
            expense.getImagePaths().forEach(path -> {
                try {
                    Files.deleteIfExists(Paths.get(path));
                } catch (IOException e) {
                    // Log error but continue deletion
                }
            });
        });
        liquidationRepository.delete(liquidation);
        return new ResponseEntity<>("Liquidation deleted successfully", HttpStatus.OK);
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        throw new IllegalStateException("User not authenticated");
    }
}