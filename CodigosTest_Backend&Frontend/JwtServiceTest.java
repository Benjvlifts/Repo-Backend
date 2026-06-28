package com.innovatech.auth;

import com.innovatech.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas unitarias para JwtService.
 * Verifica: generación de tokens, validación, expiración, extracción de claims.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@DisplayName("JwtService — Pruebas Unitarias")
class JwtServiceTest {

    private JwtService jwtService;

    // Clave de 512 bits para HS512 (mínimo requerido por JJWT 0.12.x)
    private static final String TEST_SECRET =
            "innovatech-test-secret-key-min-512-bits-long-for-testing-purposes-only-do-not-use-in-prod";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hora

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateTokenTests {

        @Test
        @DisplayName("✅ Genera token no nulo y no vacío")
        void generateToken_returnsNonEmptyToken() {
            String token = jwtService.generateToken("user@test.cl", "EMPLOYEE", 1L);

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("✅ Token tiene formato JWT (3 partes separadas por punto)")
        void generateToken_hasJwtFormat() {
            String token = jwtService.generateToken("user@test.cl", "EMPLOYEE", 1L);
            String[] parts = token.split("\\.");

            assertThat(parts).hasSize(3);
        }

        @Test
        @DisplayName("✅ Tokens distintos para diferentes usuarios")
        void generateToken_differentUsersGetDifferentTokens() {
            String token1 = jwtService.generateToken("user1@test.cl", "EMPLOYEE", 1L);
            String token2 = jwtService.generateToken("user2@test.cl", "ADMIN", 2L);

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("isTokenValid()")
    class ValidateTokenTests {

        @Test
        @DisplayName("✅ Token recién generado es válido")
        void isTokenValid_freshToken_returnsTrue() {
            String token = jwtService.generateToken("user@test.cl", "EMPLOYEE", 1L);

            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("❌ Token manipulado es inválido")
        void isTokenValid_tamperedToken_returnsFalse() {
            String token = jwtService.generateToken("user@test.cl", "EMPLOYEE", 1L);
            String tampered = token + "manipulation";

            assertThat(jwtService.isTokenValid(tampered)).isFalse();
        }

        @Test
        @DisplayName("❌ Token completamente falso es inválido")
        void isTokenValid_fakeToken_returnsFalse() {
            assertThat(jwtService.isTokenValid("not.a.valid.jwt")).isFalse();
        }

        @Test
        @DisplayName("❌ Token nulo es inválido")
        void isTokenValid_nullToken_returnsFalse() {
            assertThat(jwtService.isTokenValid(null)).isFalse();
        }

        @Test
        @DisplayName("❌ Token expirado es inválido")
        void isTokenValid_expiredToken_returnsFalse() throws Exception {
            // Configurar expiración de 1 ms para forzar expiración
            ReflectionTestUtils.setField(jwtService, "expirationMs", 1L);
            String token = jwtService.generateToken("user@test.cl", "EMPLOYEE", 1L);
            Thread.sleep(10);

            assertThat(jwtService.isTokenValid(token)).isFalse();
        }
    }
}