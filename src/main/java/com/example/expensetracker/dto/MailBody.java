package com.example.expensetracker.dto;

import lombok.Builder;

@Builder
public record MailBody(String to, String subject, String text) {
}
