package com.company.user_service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor   // for (accessToken, refreshToken)
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    // 🔥 ADD THIS CONSTRUCTOR
    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}