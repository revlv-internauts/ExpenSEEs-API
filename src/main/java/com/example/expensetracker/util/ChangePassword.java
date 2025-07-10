package com.example.expensetracker.util;

public record ChangePassword(String currentPassword, String newPassword, String repeatPassword) {
}