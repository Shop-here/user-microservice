package com.company.user_service.service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.company.user_service.exception.OtpException;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 5;

    // 🔹 GENERATE + STORE + SEND OTP
    public void generateOtp(String email) {

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        // Store OTP with expiry
        redisTemplate.opsForValue()
                .set(email, otp, Duration.ofMinutes(OTP_EXPIRY_MINUTES));

        // Send OTP via email
        emailService.sendOtp(email, otp);

        System.out.println("OTP stored in Redis: " + email + " -> " + otp);
    }

    // 🔹 VERIFY OTP
    public void verifyOtp(String email, String otp) {

        String storedOtp = (String) redisTemplate.opsForValue().get(email);

        if (storedOtp == null) {
            throw new OtpException("OTP expired or not found");
        }

       

        if (!storedOtp.equals(otp)) {
            throw new OtpException("Invalid OTP");
        }

        // Delete OTP after success
        redisTemplate.delete(email);
    }
}