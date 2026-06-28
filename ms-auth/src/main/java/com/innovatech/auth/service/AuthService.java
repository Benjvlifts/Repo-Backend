package com.innovatech.auth.service;

import com.innovatech.auth.dto.AuthDtos.*;
import com.innovatech.auth.model.User;
import com.innovatech.auth.repository.IUserRepository; // FIX Bug 2: solo la abstracción
import com.innovatech.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    // FIX Bug 2: era JpaUserRepository (implementación). Ahora usa la interfaz de dominio.
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // ... resto de métodos existentes sin cambios ...

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado: " + request.getEmail());
        }
        User.Role role = (request.getRole() != null) ? request.getRole() : User.Role.EMPLOYEE;
        User user = User.builder()
                .name(request.getName()).email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role).active(true).build();
        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getEmail(), saved.getRole().name(), saved.getId());
        return AuthResponse.builder().token(token).type("Bearer").userId(saved.getId())
                .name(saved.getName()).email(saved.getEmail()).role(saved.getRole().name()).build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!user.isActive()) throw new BadCredentialsException("Usuario inactivo");
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new BadCredentialsException("Credenciales inválidas");
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return AuthResponse.builder().token(token).type("Bearer").userId(user.getId())
                .name(user.getName()).email(user.getEmail()).role(user.getRole().name()).build();
    }

    public boolean validateToken(String token) {
        return jwtService.isTokenValid(token);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toUserResponse).collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder().id(user.getId()).name(user.getName())
                .email(user.getEmail()).role(user.getRole().name()).active(user.isActive()).build();
    }
}