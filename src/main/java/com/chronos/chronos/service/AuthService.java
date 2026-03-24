package com.chronos.chronos.service;

import com.chronos.chronos.config.JwtUtil;
import com.chronos.chronos.dto.LoginRequest;
import com.chronos.chronos.dto.LoginResponse;
import com.chronos.chronos.dto.RegisterRequest;
import com.chronos.chronos.exception.ApiException;
import com.chronos.chronos.model.UserModel;
import com.chronos.chronos.repositiory.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email and password are required.", "AUTH_FIELDS_REQUIRED");
        }

        UserModel user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password.",
                        "INVALID_CREDENTIALS"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.", "INVALID_CREDENTIALS");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(user.getName(), token);
    }

    public LoginResponse register(RegisterRequest request) {
        String name = normalizeName(request.getName());
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();

        if (!StringUtils.hasText(name)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name is required.", "NAME_REQUIRED");
        }
        if (!StringUtils.hasText(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required.", "EMAIL_REQUIRED");
        }
        if (!StringUtils.hasText(password)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required.", "PASSWORD_REQUIRED");
        }
        if (password.length() < 6) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be at least 6 characters.",
                    "PASSWORD_TOO_SHORT");
        }

        boolean exists = userRepo.existsByEmail(email);
        if (exists) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists. Please log in or use another email.",
                    "USER_ALREADY_EXISTS");
        }

        UserModel user = new UserModel();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);

        try {
            userRepo.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists. Please log in or use another email.",
                    "USER_ALREADY_EXISTS");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(user.getName(), token);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return name.trim();
    }
}
