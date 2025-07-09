package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.ForgotPassword;
import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.ForgotPasswordRepository;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.dto.MailBody;
import com.example.expensetracker.service.EmailService;
import com.example.expensetracker.util.ChangePassword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/forgotPassword")
@CrossOrigin(origins = "*")
public class ForgotPasswordController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ForgotPasswordRepository forgotPasswordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${otp.expiration.minutes:20}")
    private int otpExpirationMinutes;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$");

    @PostMapping("/verifyMail")
    @Transactional
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Email is required");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    response.put("status", "error");
                    response.put("message", "Email not found");
                    return new UsernameNotFoundException("Email not found");
                });

        forgotPasswordRepository.deleteByUser(user);
        int otp = otpGenerator();
        MailBody mailBody = MailBody.builder()
                .to(email)
                .text("Your OTP for password reset is: " + otp + ". Valid for " + otpExpirationMinutes + " minutes.")
                .subject("Password Reset OTP")
                .build();

        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + otpExpirationMinutes * 60 * 1000))
                .user(user)
                .build();

        try {
            emailService.sendSimpleMessage(mailBody);
            forgotPasswordRepository.save(fp);
            response.put("status", "success");
            response.put("message", "OTP sent to your email for verification");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            forgotPasswordRepository.deleteByUser(user);
            response.put("status", "error");
            response.put("message", "Failed to send OTP");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/verifyOtp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody Map<String, Object> request) {
        Map<String, String> response = new HashMap<>();
        String email = (String) request.get("email");
        Integer otp = Integer.parseInt(request.get("otp").toString());

        ForgotPassword fp = validateOtp(email, otp);
        if (fp == null) {
            response.put("status", "error");
            response.put("message", "Invalid or expired OTP");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        response.put("status", "success");
        response.put("message", "OTP verified successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/changePassword")
    @Transactional
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, Object> request) {
        Map<String, String> response = new HashMap<>();
        String email = (String) request.get("email");
        Integer otp = Integer.parseInt(request.get("otp").toString());
        String password = (String) request.get("password");
        String repeatPassword = (String) request.get("repeatPassword");

        ForgotPassword fp = validateOtp(email, otp);
        if (fp == null) {
            response.put("status", "error");
            response.put("message", "Invalid or expired OTP");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!password.equals(repeatPassword)) {
            response.put("status", "error");
            response.put("message", "Passwords do not match");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            response.put("status", "error");
            response.put("message", "Password must be at least 8 characters long and include a number and a special character");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String encodedPassword = passwordEncoder.encode(password);
        userRepository.updatePassword(email, encodedPassword);
        forgotPasswordRepository.deleteById(fp.getForgotPasswordId());

        response.put("status", "success");
        response.put("message", "Password changed successfully");
        return ResponseEntity.ok(response);
    }

    private ForgotPassword validateOtp(String email, Integer otp) {
        Map<String, String> response = new HashMap<>();
        User user = userRepository.findByEmail(email)
                .orElse(null);
        if (user == null) {
            return null;
        }

        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElse(null);
        if (fp == null || fp.getExpirationTime().before(new Date())) {
            if (fp != null) {
                forgotPasswordRepository.deleteById(fp.getForgotPasswordId());
            }
            return null;
        }
        return fp;
    }

    private Integer otpGenerator() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999);
    }
}