package com.innovatech.auth;

import com.innovatech.auth.dto.AuthDtos.*;
import com.innovatech.auth.model.User;
import com.innovatech.auth.repository.JpaUserRepository;
import com.innovatech.auth.security.JwtService;
import com.innovatech.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * Cubre: registro, login, validación de token, consulta de usuarios.
 * Cobertura objetivo: ≥80% de AuthService.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Pruebas Unitarias")
class AuthServiceTest {

    @Mock private JpaUserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

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

    // ═══════════════════════════════════════════════════════════════
    // REGISTRO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("✅ Registra usuario nuevo y retorna token JWT")
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
            assertThat(response.getName()).isEqualTo("Benjamin Valdes");
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("❌ Lanza IllegalArgumentException si email ya existe")
        void register_duplicateEmail_throwsIllegalArgumentException() {
            when(userRepository.existsByEmail("benjamin@innovatech.cl")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ya está registrado");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ Asigna rol EMPLOYEE por defecto si no se especifica rol")
        void register_noRoleSpecified_assignsEmployeeRoleByDefault() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");

            RegisterRequest noRoleRequest = RegisterRequest.builder()
                    .name("Test User")
                    .email("test@innovatech.cl")
                    .password("pass123")
                    .build();

            AuthResponse response = authService.register(noRoleRequest);

            assertThat(response.getRole()).isEqualTo("EMPLOYEE");
        }

        @Test
        @DisplayName("✅ Registra usuario con rol ADMIN cuando se especifica")
        void register_withAdminRole_returnsAdminRole() {
            User adminUser = User.builder()
                    .id(2L).name("Admin User")
                    .email("admin@innovatech.cl")
                    .password("hashed").role(User.Role.ADMIN).active(true).build();

            RegisterRequest adminRequest = RegisterRequest.builder()
                    .name("Admin User").email("admin@innovatech.cl")
                    .password("admin123").role(User.Role.ADMIN).build();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenReturn(adminUser);
            when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("admin_token");

            AuthResponse response = authService.register(adminRequest);

            assertThat(response.getRole()).isEqualTo("ADMIN");
            assertThat(response.getToken()).isEqualTo("admin_token");
        }

        @Test
        @DisplayName("✅ La contraseña se almacena hasheada, nunca en texto plano")
        void register_passwordIsHashedBeforeSave() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("secret123")).thenReturn("BCrypt$hashed$value");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                assertThat(u.getPassword()).isEqualTo("BCrypt$hashed$value");
                assertThat(u.getPassword()).doesNotContain("secret123");
                u.setId(1L);
                return u;
            });
            when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");

            authService.register(registerRequest);

            verify(passwordEncoder, times(1)).encode("secret123");
        }

        @Test
        @DisplayName("✅ El usuario queda activo por defecto tras el registro")
        void register_newUser_isActiveByDefault() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                assertThat(u.isActive()).isTrue();
                u.setId(1L);
                return u;
            });
            when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");

            authService.register(registerRequest);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("✅ Login exitoso con credenciales válidas retorna token")
        void login_validCredentials_returnsAuthResponse() {
            when(userRepository.findByEmail("benjamin@innovatech.cl")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("secret123", "hashed_password")).thenReturn(true);
            when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("jwt_token_mock");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt_token_mock");
            assertThat(response.getEmail()).isEqualTo("benjamin@innovatech.cl");
            assertThat(response.getType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("❌ Lanza BadCredentialsException con email inexistente")
        void login_unknownEmail_throwsBadCredentialsException() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Credenciales inválidas");
        }

        @Test
        @DisplayName("❌ Lanza BadCredentialsException con contraseña incorrecta")
        void login_wrongPassword_throwsBadCredentialsException() {
            when(userRepository.findByEmail("benjamin@innovatech.cl")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Credenciales inválidas");
        }

        @Test
        @DisplayName("❌ Lanza BadCredentialsException si usuario está inactivo")
        void login_inactiveUser_throwsBadCredentialsException() {
            sampleUser.setActive(false);
            when(userRepository.findByEmail("benjamin@innovatech.cl")).thenReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("inactivo");

            // No debe verificar password si usuario está inactivo
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("✅ El token generado incluye email, rol e ID del usuario")
        void login_success_tokenGeneratedWithCorrectParameters() {
            when(userRepository.findByEmail("benjamin@innovatech.cl")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("secret123", "hashed_password")).thenReturn(true);
            when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");

            authService.login(loginRequest);

            verify(jwtService).generateToken("benjamin@innovatech.cl", "EMPLOYEE", 1L);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VALIDACIÓN DE TOKEN
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validateToken()")
    class ValidateTokenTests {

        @Test
        @DisplayName("✅ Retorna true para token válido")
        void validateToken_validToken_returnsTrue() {
            when(jwtService.isTokenValid("valid_token")).thenReturn(true);

            assertThat(authService.validateToken("valid_token")).isTrue();
        }

        @Test
        @DisplayName("✅ Retorna false para token inválido")
        void validateToken_invalidToken_returnsFalse() {
            when(jwtService.isTokenValid("bad_token")).thenReturn(false);

            assertThat(authService.validateToken("bad_token")).isFalse();
        }

        @Test
        @DisplayName("✅ Retorna false para token nulo")
        void validateToken_nullToken_returnsFalse() {
            when(jwtService.isTokenValid(null)).thenReturn(false);

            assertThat(authService.validateToken(null)).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CONSULTA DE USUARIOS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllUsers() y getUserById()")
    class UserQueryTests {

        @Test
        @DisplayName("✅ getAllUsers retorna lista con todos los usuarios")
        void getAllUsers_returnsUserList() {
            User secondUser = User.builder().id(2L).name("Ignacio Munoz")
                    .email("ignacio@innovatech.cl").role(User.Role.MANAGER).active(true).build();
            when(userRepository.findAll()).thenReturn(List.of(sampleUser, secondUser));

            List<UserResponse> users = authService.getAllUsers();

            assertThat(users).hasSize(2);
            assertThat(users).extracting(UserResponse::getEmail)
                    .containsExactlyInAnyOrder("benjamin@innovatech.cl", "ignacio@innovatech.cl");
        }

        @Test
        @DisplayName("✅ getAllUsers retorna lista vacía si no hay usuarios")
        void getAllUsers_emptyRepo_returnsEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserResponse> users = authService.getAllUsers();

            assertThat(users).isEmpty();
        }

        @Test
        @DisplayName("✅ getUserById retorna usuario existente por ID")
        void getUserById_existingId_returnsUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            UserResponse response = authService.getUserById(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Benjamin Valdes");
            assertThat(response.getEmail()).isEqualTo("benjamin@innovatech.cl");
            assertThat(response.getRole()).isEqualTo("EMPLOYEE");
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("❌ getUserById lanza IllegalArgumentException con ID inexistente")
        void getUserById_nonExistingId_throwsIllegalArgumentException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getUserById(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no encontrado")
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("✅ UserResponse contiene todos los campos correctos")
        void getUserById_mapsAllFieldsCorrectly() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            UserResponse response = authService.getUserById(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Benjamin Valdes");
            assertThat(response.getEmail()).isEqualTo("benjamin@innovatech.cl");
            assertThat(response.getRole()).isEqualTo("EMPLOYEE");
            assertThat(response.isActive()).isTrue();
        }
    }
}
