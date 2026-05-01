package com.company.user_service.securityconfig;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.company.user_service.service.CustomUserDetailsService;
import com.company.user_service.service.TokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 🔓 PUBLIC APIs
        boolean isPublic =
                path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/verify") ||
                path.startsWith("/api/auth/resend-otp") ||
                path.startsWith("/api/auth/refresh");

        if (isPublic) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        try {
            if (header != null && header.startsWith("Bearer ")) {

                String token = header.substring(7);

                // 🔥 BLACKLIST CHECK
                if (tokenService.isBlacklisted(token)) {
                    chain.doFilter(request, response);
                    return;
                }

                String username = jwtUtil.extractUsername(token);

                if (username != null &&
                        SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails =
                            userDetailsService.loadUserByUsername(username);

                    if (jwtUtil.validate(token, username)) {

                        List<String> roles = jwtUtil.extractRoles(token);

                        List<SimpleGrantedAuthority> authorities =
                                roles.stream()
                                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                        .toList();

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, authorities
                                );

                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }

        } catch (Exception ex) {
            System.out.println("JWT ERROR: " + ex.getMessage());
        }

        chain.doFilter(request, response);
    }
}