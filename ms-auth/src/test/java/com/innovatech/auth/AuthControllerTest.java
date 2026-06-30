package com.innovatech.auth;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innovatech.auth.config.SecurityConfig;
import com.innovatech.auth.controller.AuthController;
import com.innovatech.auth.dto.AuthDtos.*;
import com.innovatech.auth.model.User;
import com.innovatech.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias para AuthController usando MockMvc (sin servidor real).
 * Valida: status HTTP, content-type, cuerpo de respuesta, manejo de errores.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"app.jwt.secret=un-secreto-muy-largo-y-seguro-para-pruebas-locales"})
@DisplayName("AuthController — Pruebas de Capa Web")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private AuthService authService;

    private AuthResponse sampleAuthResponse;
    private UserResponse sampleUserResponse;

    @BeforeEach
    void setUp() {
        sampleAuthResponse = AuthResponse.builder()
                .token("jwt_test_token")
                .type("Bearer")
                .userId(1L)
                .name("Benjamin Valdes")
                .email("benjamin@innovatech.cl")
                .role("EMPLOYEE")
                .build();

        sampleUserResponse = UserResponse.builder()
                .id(1L)
                .name("Benjamin Valdes")
                .email("benjamin@innovatech.cl")
                .role("EMPLOYEE")
                .active(true)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // POST /api/auth/register
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("✅ Retorna 201 CREATED con token al registrar usuario válido")
        void register_validRequest_returns201WithToken() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("Benjamin Valdes")
                    .email("benjamin@innovatech.cl")
                    .password("secret123")
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(sampleAuthResponse);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("jwt_test_token"))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.email").value("benjamin@innovatech.cl"))
                    .andExpect(jsonPath("$.role").value("EMPLOYEE"));
        }

        @Test
        @DisplayName("❌ Retorna 400 BAD_REQUEST cuando email ya existe")
        void register_duplicateEmail_returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("Benjamin Valdes")
                    .email("benjamin@innovatech.cl")
                    .password("secret123")
                    .build();

            when(authService.register(any())).thenThrow(
                    new IllegalArgumentException("El email ya está registrado: benjamin@innovatech.cl"));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // POST /api/auth/login
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("✅ Retorna 200 OK con token al hacer login válido")
        void login_validCredentials_returns200WithToken() throws Exception {
            LoginRequest request = new LoginRequest("benjamin@innovatech.cl", "secret123");
            when(authService.login(any(LoginRequest.class))).thenReturn(sampleAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt_test_token"))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.email").value("benjamin@innovatech.cl"));
        }

        @Test
        @DisplayName("❌ Retorna 401 cuando credenciales son inválidas")
        void login_invalidCredentials_returns401() throws Exception {
            LoginRequest request = new LoginRequest("wrong@email.cl", "badpass");
            when(authService.login(any())).thenThrow(new BadCredentialsException("Credenciales inválidas"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // POST /api/auth/validate
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/auth/validate")
    class ValidateEndpointTests {

        @Test
        @DisplayName("✅ Retorna {valid: true} para token válido")
        void validateToken_validToken_returnsTrue() throws Exception {
            when(authService.validateToken("valid_jwt")).thenReturn(true);

            mockMvc.perform(post("/api/auth/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("token", "valid_jwt"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }

        @Test
        @DisplayName("✅ Retorna {valid: false} para token inválido")
        void validateToken_invalidToken_returnsFalse() throws Exception {
            when(authService.validateToken("bad_jwt")).thenReturn(false);

            mockMvc.perform(post("/api/auth/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("token", "bad_jwt"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET /api/auth/users
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/auth/users")
    class UserEndpointTests {

        @Test
        @DisplayName("✅ Retorna 200 con lista de usuarios")
        void getAllUsers_returns200WithList() throws Exception {
            when(authService.getAllUsers()).thenReturn(List.of(sampleUserResponse));

            mockMvc.perform(get("/api/auth/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].email").value("benjamin@innovatech.cl"))
                    .andExpect(jsonPath("$[0].role").value("EMPLOYEE"));
        }

        @Test
        @DisplayName("✅ Retorna 200 con usuario específico por ID")
        void getUserById_existingId_returns200() throws Exception {
            when(authService.getUserById(1L)).thenReturn(sampleUserResponse);

            mockMvc.perform(get("/api/auth/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Benjamin Valdes"));
        }

        @Test
        @DisplayName("❌ Retorna 400 si ID de usuario no existe")
        void getUserById_nonExistingId_returns400() throws Exception {
            when(authService.getUserById(99L)).thenThrow(
                    new IllegalArgumentException("Usuario no encontrado con id: 99"));

            mockMvc.perform(get("/api/auth/users/99"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET /api/auth/health
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/auth/health")
    class HealthEndpointTests {

        @Test
        @DisplayName("✅ Health endpoint retorna status UP")
        void health_returns200WithStatusUp() throws Exception {
            mockMvc.perform(get("/api/auth/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("ms-auth"));
        }
    }
}