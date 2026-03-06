package com.chronos.chronos.service;

import com.chronos.chronos.config.JwtUtil;
import com.chronos.chronos.dto.LoginRequest;
import com.chronos.chronos.dto.LoginResponse;
import com.chronos.chronos.dto.RegisterRequest;
import com.chronos.chronos.model.UserModel;
import com.chronos.chronos.repositiory.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        UserModel user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid Credentials");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(user.getName(), token);
    }

    public LoginResponse register(RegisterRequest request) {
        boolean exists = userRepo.existsByEmail(request.getEmail());
        if (exists)
            throw new RuntimeException("User Already Exists");
        UserModel user = new UserModel();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        userRepo.save(user);
        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(user.getName(), token);
    }
}
