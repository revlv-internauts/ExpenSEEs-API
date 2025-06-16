package com.example.expensetracker.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    private String message;
    private int status;
    private String timestamp;

    public ErrorResponse(String message, int status, String timestamp) {
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }
}