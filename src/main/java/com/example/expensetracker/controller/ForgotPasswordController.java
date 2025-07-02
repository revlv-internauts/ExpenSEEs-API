package com.example.expensetracker.controller;

import com.example.expensetracker.Entity.ForgotPassword;
import com.example.expensetracker.Entity.User;
import com.example.expensetracker.Repository.ForgotPasswordRepository;
import com.example.expensetracker.Repository.UserRepository;
import com.example.expensetracker.dto.MailBody;
import com.example.expensetracker.service.EmailService;
import com.example.expensetracker.util.ChangePassword;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/verifyMail/{email}")
    @Transactional
    public ResponseEntity<Map<String, String>> verifyEmail(@PathVariable String email) {
        Map<String, String> response = new HashMap<>();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    response.put("error", "Invalid email: " + email);
                    return new UsernameNotFoundException("Invalid email");
                });

        forgotPasswordRepository.deleteByUser(user);

        int otp = otpGenerator();
        MailBody mailBody = MailBody.builder()
                .to(email)
                .text("Your OTP for password reset is: " + otp + ". Valid for 20 minutes.")
                .subject("Password Reset OTP")
                .build();

        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 20 * 60 * 1000))
                .user(user)
                .build();

        try {
            emailService.sendSimpleMessage(mailBody);
            forgotPasswordRepository.save(fp);
            response.put("message", "OTP sent to your email for verification!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            forgotPasswordRepository.deleteByUser(user);
            response.put("error", "Failed to send OTP");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/verifyOtp/{otp}/{email}")
    public ResponseEntity<Map<String, String>> verifyOtp(@PathVariable Integer otp, @PathVariable String email) {
        Map<String, String> response = new HashMap<>();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    response.put("status", "error");
                    response.put("message", "Invalid email: " + email);
                    return new UsernameNotFoundException("Invalid email");
                });

        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> {
                    response.put("status", "error");
                    response.put("message", "Invalid or expired OTP for email: " + email);
                    return new RuntimeException();
                });

        if (fp.getExpirationTime().before(new Date())) {
            forgotPasswordRepository.deleteById(fp.getForgotPasswordId());
            response.put("status", "error");
            response.put("message", "OTP has expired. Please request a new one.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        response.put("status", "success");
        response.put("message", "OTP verified successfully!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/changePassword/{email}")
    @Transactional
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePassword changePassword,
                                                              @PathVariable String email,
                                                              @RequestParam("otp") Integer otp) {
        Map<String, String> response = new HashMap<>();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    response.put("error", "Invalid email: " + email);
                    return new UsernameNotFoundException("Invalid email");
                });

        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> {
                    response.put("error", "Invalid OTP for email: " + email);
                    return new RuntimeException();
                });

        if (fp.getExpirationTime().before(new Date())) {
            forgotPasswordRepository.deleteById(fp.getForgotPasswordId());
            response.put("error", "OTP has expired. Please request a new one.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!changePassword.password().equals(changePassword.repeatPassword())) {
            response.put("error", "Passwords do not match.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email, encodedPassword);
        forgotPasswordRepository.deleteById(fp.getForgotPasswordId());

        response.put("message", "Password changed successfully!");
        return ResponseEntity.ok(response);
    }

    private Integer otpGenerator() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999);
    }
}