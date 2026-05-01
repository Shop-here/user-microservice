package com.company.user_service.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;

import org.springframework.http.ResponseCookie;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.company.user_service.dto.AuthResponse;
import com.company.user_service.dto.LogoutRequest;
import com.company.user_service.dto.RefreshRequest;
import com.company.user_service.dto.RegisterRequest;
import com.company.user_service.entity.User;
import com.company.user_service.exception.InvalidCredentialsException;
import com.company.user_service.exception.OtpException;
import com.company.user_service.exception.ResourceNotFoundException;
import com.company.user_service.exception.UserAlreadyExistsException;
import com.company.user_service.repository.UserRepository;
import com.company.user_service.securityconfig.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final TokenService tokenService;

    // 🔹 REGISTER USER

    @Transactional
    public String register(RegisterRequest request) {

        // 🔴 Validate passwords first
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        // 🔴 Check email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        // ✅ Hash password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // ✅ Save user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(hashedPassword)
                .isVerified(false)
                .roles(new HashSet<>(Set.of("USER")))
                .build();

        userRepository.save(user);

        // ✅ OTP
        otpService.generateOtp(user.getEmail());

        return "OTP sent to email";
    }

    // 🔹 VERIFY OTP
    public String verifyOtp(String email, String otp) {

        // 1. Get user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 🔴 2. Already verified check (ADD THIS)
        if (user.isVerified()) {
            throw new UserAlreadyExistsException("Email already verified");
        }

        // 3. Verify OTP
        otpService.verifyOtp(email, otp);

        // 4. Activate account
        user.setVerified(true);
        userRepository.save(user);

        return "Account verified successfully";
    }

    // 🔹 RESEND OTP
    public String resendOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isVerified()) {
            throw new OtpException("Email already verified");
        }

        otpService.generateOtp(email);

        return "OTP resent successfully";
    }

    public AuthResponse login(String email, String password) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
 if(!user.isVerified()){
throw new OtpException("Verify user before login");

 }
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRoles());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        tokenService.storeRefreshToken(user.getEmail(), refreshToken, 604800000);

        // return both (controller will handle cookie)
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(HttpServletRequest request) {

        if (request.getCookies() == null) {
            throw new RuntimeException("No refresh token found");
        }

        String refreshToken = Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new RuntimeException("No refresh token found"));

        String email = jwtUtil.extractUsername(refreshToken);

        // 🔥 Validate token from Redis
        if (!tokenService.validateRefreshToken(email, refreshToken)) {
            throw new RuntimeException("Session expired. Please login again");
        }

        // 🔥 FIX: get real role from DB
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtUtil.generateAccessToken(email, user.getRoles());

        return new AuthResponse(newAccessToken);
    }

    public String logout(HttpServletRequest request, HttpServletResponse response) {

        // =========================
        // 🔥 1. BLACKLIST ACCESS TOKEN
        // =========================
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String accessToken = header.substring(7);

            long expiry = jwtUtil.getRemainingTime(accessToken);

            tokenService.blacklistAccessToken(accessToken, expiry);
        }

        // =========================
        // 🔥 2. DELETE REFRESH TOKEN (FIXED)
        // =========================
        if (request.getCookies() != null) {
            String refreshToken = Arrays.stream(request.getCookies())
                    .filter(c -> "refreshToken".equals(c.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);

            if (refreshToken != null) {
                String email = jwtUtil.extractUsername(refreshToken); // 🔥 FIX
                tokenService.deleteRefreshTokenByEmail(email); // 🔥 FIX
            }
        }

        // =========================
        // 🔥 3. DELETE COOKIE
        // =========================
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return "Logged out successfully";
    }

    public String createSeller(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .isVerified(true) // 🔥 seller is directly verified
                .roles(new HashSet<>(Set.of("SELLER")))
                .build();

        userRepository.save(user);

        return "Seller created successfully";
    }

    public String createAdmin(RegisterRequest request) {

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .isVerified(true)
                .roles(new HashSet<>(Set.of("ADMIN")))
                .build();

        userRepository.save(user);

        return "Admin created";
    }

    // public Object addSellerRole(String email) {

    //     User user = userRepository.findByEmail(email)
    //             .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    //           if( !user.isVerified()){
    //             return "Please verify before becoming seller";
    //           }
    //     // already seller?
    //     if (user.getRoles().contains("SELLER")) {
    //         return "Already a seller";
    //     }

    //     // 🔥 ADD ROLE
    //     user.getRoles().add("SELLER");

    //     userRepository.save(user);

    //     return "Seller role added successfully";
    // }

    // ===============================
    // 🛍 SELLER LOGIC
    // ===============================

    // 1. Apply seller
    public String applySeller(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isVerified()) {
            throw new RuntimeException("Verify email first");
        }

        if (user.getRoles().contains("SELLER")) {
            return "Already a seller";
        }

        if ("PENDING".equals(user.getSellerStatus())) {
            return "Seller request already pending";
        }

        user.setSellerStatus("PENDING");
        userRepository.save(user);

        return "Seller request submitted";
    }

    // 2. Get seller status
    public Map<String, String> getSellerStatus(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return Map.of(
                "email", user.getEmail(),
                "status", user.getSellerStatus() == null ? "NOT_APPLIED" : user.getSellerStatus());
    }

    // 3. Get pending sellers
    public List<User> getPendingSellers() {
        return userRepository.findBySellerStatus("PENDING");
    }

    // 4. Approve seller
    public String approveSeller(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"PENDING".equals(user.getSellerStatus())) {
            throw new RuntimeException("No pending request");
        }

        user.setSellerStatus("APPROVED");
        user.getRoles().add("SELLER");

        userRepository.save(user);

        return "Seller approved successfully";
    }

    // 5. Reject seller
    public String rejectSeller(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"PENDING".equals(user.getSellerStatus())) {
            throw new RuntimeException("No pending request");
        }

        user.setSellerStatus("REJECTED");
        userRepository.save(user);

        return "Seller rejected successfully";
    }
    // ===============================
// 🗑 DELETE USER LOGIC
// ===============================

// 🔥 ADMIN DELETE
public String deleteUserByAdmin(String email) {

    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // 🔥 delete refresh token
    tokenService.deleteRefreshTokenByEmail(email);

    userRepository.delete(user);

    return "User deleted successfully";
}


// 🔥 USER DELETE OWN ACCOUNT
public String deleteOwnAccount(HttpServletRequest request,
                               HttpServletResponse response) {

    String header = request.getHeader("Authorization");

    if (header == null || !header.startsWith("Bearer ")) {
        throw new RuntimeException("Invalid token");
    }

    String accessToken = header.substring(7);

    String email = jwtUtil.extractUsername(accessToken);

    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // =========================
    // 🔥 1. BLACKLIST ACCESS TOKEN
    // =========================
    long expiry = jwtUtil.getRemainingTime(accessToken);
    tokenService.blacklistAccessToken(accessToken, expiry);

    // =========================
    // 🔥 2. DELETE REFRESH TOKEN
    // =========================
    tokenService.deleteRefreshTokenByEmail(email);

    // =========================
    // 🔥 3. DELETE USER
    // =========================
    userRepository.delete(user);

    // =========================
    // 🔥 4. CLEAR COOKIE
    // =========================
    ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .build();

    response.addHeader("Set-Cookie", cookie.toString());

    return "Account deleted successfully";
}
public List<User> getApprovedSellers() {
    return userRepository.findBySellerStatus("APPROVED");
}

public List<User> getAllUsers() {
    return userRepository.findAll();
}

}