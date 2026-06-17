package com.innovatech.auth;

import com.innovatech.auth.dto.AuthDtos.*;
import com.innovatech.auth.model.User;
import com.innovatech.auth.repository.JpaUserRepository;
import com.innovatech.auth.security.JwtService;
import com.innovatech.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para AuthService.
 * Cobertura: registro, login, validación de token, consulta de usuarios.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JpaUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Benjamin Valdes")
                .email("benjamin@innovatech.cl")
                .password("hashed_password")
                .role(User.Role.EMPLOYEE)
                .active(true)
                .build();

        registerRequest = RegisterRequest.builder()
                .name("Benjamin Valdes")
                .email("benjamin@innovatech.cl")
                .password("secret123")
                .build();

        loginRequest = new LoginRequest("benjamin@innovatech.cl", "secret123");
    }

    // ── Registro ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: debe registrar usuario nuevo y retornar token")
    void register_newUser_returnsAuthResponse() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("jwt_token_mock");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt_token_mock");
        assertThat(response.getEmail()).isEqualTo("benjamin@innovatech.cl");
        assertThat(response.getType()).isEqualTo("Bearer");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("register: debe lanzar excepción si el email ya existe")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("benjamin@innovatech.cl")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: debe asignar rol EMPLOYEE por defecto si no se especifica")
    void register_noRoleSpecified_assignsEmployeeRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");

        RegisterRequest noRoleRequest = RegisterRequest.builder()
                .name("Test")
                .email("test@test.cl")
                .password("pass123")
                .build();

        AuthResponse response = authService.register(noRoleRequest);
        assertThat(response.getRole()).isEqualTo("EMPLOYEE");
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: debe autenticar usuario válido y retornar token")
    void login_validCredentials_returnsAuthResponse() {
        when(userRepository.findByEmail("benjamin@innovatech.cl")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("secret123", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("jwt_token_mock");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt_token_mock");
        assertThat(response.getEmail()).isEqualTo("benjamin@innovatech.cl");
    }

    @Test
    @DisplayName("login: debe lanzar excepción con email inexistente")
    void login_unknownEmail_throwsBadCredentials() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Credenciales inválidas");
    }

    @Test
    @DisplayName("login: debe lanzar excepción con contraseña incorrecta")
    void login_wrongPassword_throwsBadCredentials() {
        when(userRepository.findByEmail("benjamin@innovatech.cl")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login: debe lanzar excepción si usuario está inactivo")
    void login_inactiveUser_throwsBadCredentials() {
        sampleUser.setActive(false);
        when(userRepository.findByEmail("benjamin@innovatech.cl")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("inactivo");
    }

    // ── Consulta ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsers: debe retornar lista de usuarios")
    void getAllUsers_returnsUserList() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserResponse> users = authService.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("benjamin@innovatech.cl");
    }

    @Test
    @DisplayName("getUserById: debe retornar usuario existente")
    void getUserById_existingId_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = authService.getUserById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Benjamin Valdes");
    }

    @Test
    @DisplayName("getUserById: debe lanzar excepción con ID inexistente")
    void getUserById_nonExistingId_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrado");
    }

    // ── Validación de token ───────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken: debe retornar true para token válido")
    void validateToken_validToken_returnsTrue() {
        when(jwtService.isTokenValid("valid_token")).thenReturn(true);
        assertThat(authService.validateToken("valid_token")).isTrue();
    }

    @Test
    @DisplayName("validateToken: debe retornar false para token inválido")
    void validateToken_invalidToken_returnsFalse() {
        when(jwtService.isTokenValid("bad_token")).thenReturn(false);
        assertThat(authService.validateToken("bad_token")).isFalse();
    }
}
