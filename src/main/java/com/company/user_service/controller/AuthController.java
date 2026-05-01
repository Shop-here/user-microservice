package com.company.user_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.validation.annotation.Validated;

import com.company.user_service.dto.AuthResponse;
import com.company.user_service.dto.LoginRequest;
import com.company.user_service.dto.RegisterRequest;
import com.company.user_service.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ===============================
    // 🔓 PUBLIC APIs
    // ===============================

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {

        AuthResponse res = authService.login(request.getEmail(), request.getPassword());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", res.getRefreshToken())
                .httpOnly(true)
                .secure(false) // 🔥 make true in production (HTTPS)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(new AuthResponse(res.getAccessToken()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        return ResponseEntity.ok(authService.verifyOtp(email, otp));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam String email) {
        return ResponseEntity.ok(authService.resendOtp(email));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request,
                                         HttpServletResponse response) {
        return ResponseEntity.ok(authService.logout(request, response));
    }

    // ===============================
    // 🔐 SELLER APIs
    // ===============================

    // ✔ Allow USER + ADMIN
    @PostMapping("/apply-seller")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<String> applySeller(@RequestParam String email) {
        return ResponseEntity.ok(authService.applySeller(email));
    }

    // ✔ FIXED: allow ADMIN also (your issue)
    @GetMapping("/seller-status")
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    public ResponseEntity<?> getSellerStatus(@RequestParam String email) {
        return ResponseEntity.ok(authService.getSellerStatus(email));
    }

    // ===============================
    // 🔐 ADMIN APIs
    // ===============================

    // ❗ VERY IMPORTANT: secure these
    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAdmin(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.createAdmin(request));
    }

    @PostMapping("/create-seller")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSeller(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.createSeller(request));
    }

    @GetMapping("/pending-sellers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingSellers() {
        return ResponseEntity.ok(authService.getPendingSellers());
    }

    @PostMapping("/approve-seller")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approveSeller(@RequestParam String email) {
        return ResponseEntity.ok(authService.approveSeller(email));
    }

    @PostMapping("/reject-seller")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rejectSeller(@RequestParam String email) {
        return ResponseEntity.ok(authService.rejectSeller(email));
    }

    @DeleteMapping("/delete-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@RequestParam String email) {
        return ResponseEntity.ok(authService.deleteUserByAdmin(email));
    }
@GetMapping("/approved-sellers")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getApprovedSellers() {
    return ResponseEntity.ok(authService.getApprovedSellers());
}

 @GetMapping("/all-users")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllUsers() {
    return ResponseEntity.ok(authService.getAllUsers());
}
    // ===============================
    // 🔐 USER SELF ACTION
    // ===============================

    @DeleteMapping("/delete-my-account")
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    public ResponseEntity<String> deleteMyAccount(
            HttpServletRequest request,
            HttpServletResponse response) {

        return ResponseEntity.ok(authService.deleteOwnAccount(request, response));
    }
}