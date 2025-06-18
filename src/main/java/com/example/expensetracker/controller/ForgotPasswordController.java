package com.example.expensetracker.controller;

import java.util.*;
import java.time.Instant;

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

@RestController
@RequestMapping("/forgotPassword")
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

    // Send OTP to email for verification (Forgot Password)
    @PostMapping("/verifyMail/{email}")
    public ResponseEntity<String> verifyEmail(@PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email: " + email));

        int otp = otpGenerator();
        MailBody mailBody = MailBody.builder()
                .to(email)
                .text("Your OTP for password reset is: " + otp + ". Valid for 20 minutes.")
                .subject("Password Reset OTP")
                .build();

        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 20 * 60 * 1000)) // 20 minutes
                .user(user)
                .build();

        emailService.sendSimpleMessage(mailBody);
        forgotPasswordRepository.save(fp);

        return ResponseEntity.ok("OTP sent to your email for verification!");
    }

    // Verify OTP for Forgot Password
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

        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteById(fp.getFpid());
            response.put("status", "error");
            response.put("message", "OTP has expired. Please request a new one.");
            return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
        }

        response.put("status", "success");
        response.put("message", "OTP verified successfully!");
        // Keep OTP for changePassword verification

        return ResponseEntity.ok(response);
    }

    // Change password after Forgot Password verification
    @PostMapping("/changePassword/{email}")
    @Transactional
    public ResponseEntity<String> changePasswordHandlerForgot(@RequestBody ChangePassword changePassword,
                                                              @PathVariable String email,
                                                              @RequestParam("otp") Integer otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email: " + email));

        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> new RuntimeException("Invalid OTP for email: " + email));

        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteById(fp.getFpid());
            return new ResponseEntity<>("OTP has expired. Please request a new one.", HttpStatus.EXPECTATION_FAILED);
        }

        if (!Objects.equals(changePassword.password(), changePassword.repeatPassword())) {
            return new ResponseEntity<>("Passwords do not match. Please try again.", HttpStatus.BAD_REQUEST);
        }

        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email, encodedPassword);
        forgotPasswordRepository.deleteById(fp.getFpid()); // Clean up after successful change

        return ResponseEntity.ok("Password changed successfully!");
    }

    // Reset password after login (no OTP required)
    @PostMapping("/resetPassword/{email}")
    @Transactional
    public ResponseEntity<String> changePasswordHandler(@RequestBody ChangePassword changePassword,
                                                        @PathVariable String email) {
        if (!Objects.equals(changePassword.password(), changePassword.repeatPassword())) {
            return new ResponseEntity<>("Please enter the password again!", HttpStatus.EXPECTATION_FAILED);
        }

        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email, encodedPassword);

        return ResponseEntity.ok("Password has been changed!");
    }

    private Integer otpGenerator() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999);
    }
}