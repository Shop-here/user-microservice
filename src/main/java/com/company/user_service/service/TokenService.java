package com.company.user_service.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_PREFIX = "REFRESH:";
    private static final String BLACKLIST_PREFIX = "BLACKLIST:";

    // =========================
    // 🔐 STORE REFRESH TOKEN
    // =========================
    public void storeRefreshToken(String email, String token, long ttl) {
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + email,
                token,
                ttl,
                TimeUnit.MILLISECONDS
        );
    }

    // =========================
    // 🔍 GET REFRESH TOKEN
    // =========================
    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(REFRESH_PREFIX + email);
    }

    // =========================
    // ❌ DELETE REFRESH TOKEN
    // =========================
    public void deleteRefreshTokenByEmail(String email) {
        redisTemplate.delete(REFRESH_PREFIX + email);
    }

    // =========================
    // 🚫 BLACKLIST ACCESS TOKEN
    // =========================
    public void blacklistAccessToken(String token, long expiryMillis) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token,
                "true",
                expiryMillis,
                TimeUnit.MILLISECONDS
        );
    }

    // =========================
    // 🔍 CHECK BLACKLIST
    // =========================
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    // =========================
    // ✅ VALIDATE REFRESH TOKEN
    // =========================
    public boolean validateRefreshToken(String email, String token) {

        String storedToken = redisTemplate.opsForValue().get(REFRESH_PREFIX + email);

        // 🔥 Prevent null issues + ensure exact match
        return storedToken != null && storedToken.equals(token);
    }
    
}