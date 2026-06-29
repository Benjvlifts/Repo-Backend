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
 * Verifica: generación de tokens, validación, expiración y extracción de claims.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@DisplayName("JwtService — Pruebas Unitarias")
class JwtServiceTest {

    private JwtService jwtService;

    // Clave Base64 idéntica a la de application.properties (256 bits / 32 bytes)
    private static final String TEST_SECRET =
            "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";
    private static final long EXPIRATION_MS = 3_600_000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inyección de @Value via ReflectionTestUtils (nombre exacto del campo)
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", EXPIRATION_MS);
    }

    // ── generateToken ─────────────────────────────────────────────────────────

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
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("✅ Tokens distintos para usuarios diferentes")
        void generateToken_differentUsers_differentTokens() {
            String token1 = jwtService.generateToken("user1@test.cl", "EMPLOYEE", 1L);
            String token2 = jwtService.generateToken("user2@test.cl", "ADMIN", 2L);
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    // ── isTokenValid ──────────────────────────────────────────────────────────

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
        @DisplayName("❌ Token manipulado (firma alterada) es inválido")
        void isTokenValid_tamperedToken_returnsFalse() {
            String token = jwtService.generateToken("user@test.cl", "EMPLOYEE", 1L);
            assertThat(jwtService.isTokenValid(token + "tampered")).isFalse();
        }

        @Test
        @DisplayName("❌ String arbitrario no es un JWT válido")
        void isTokenValid_randomString_returnsFalse() {
            assertThat(jwtService.isTokenValid("not.a.jwt.at.all")).isFalse();
        }

        @Test
        @DisplayName("❌ Token nulo es inválido")
        void isTokenValid_null_returnsFalse() {
            assertThat(jwtService.isTokenValid(null)).isFalse();
        }

        @Test
        @DisplayName("❌ Token expirado es inválido")
        void isTokenValid_expiredToken_returnsFalse() throws InterruptedException {
            ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 1L);
            String token = jwtService.generateToken("user@test.cl", "EMPLOYEE", 1L);
            Thread.sleep(20);
            assertThat(jwtService.isTokenValid(token)).isFalse();
        }
    }

    // ── extractEmail ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractEmail()")
    class ExtractEmailTests {

        @Test
        @DisplayName("✅ Extrae el subject (email) del token")
        void extractEmail_validToken_returnsEmail() {
            String token = jwtService.generateToken("test@test.cl", "ADMIN", 99L);
            assertThat(jwtService.extractEmail(token)).isEqualTo("test@test.cl");
        }
    }

    // ── extractRole ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractRole()")
    class ExtractRoleTests {

        @Test
        @DisplayName("✅ Extrae el rol ADMIN del token")
        void extractRole_adminToken_returnsAdmin() {
            String token = jwtService.generateToken("admin@test.cl", "ADMIN", 1L);
            assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("✅ Extrae el rol EMPLOYEE del token")
        void extractRole_employeeToken_returnsEmployee() {
            String token = jwtService.generateToken("emp@test.cl", "EMPLOYEE", 2L);
            assertThat(jwtService.extractRole(token)).isEqualTo("EMPLOYEE");
        }
    }

    // ── extractUserId ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractUserId()")
    class ExtractUserIdTests {

        @Test
        @DisplayName("✅ Extrae el userId del token")
        void extractUserId_validToken_returnsUserId() {
            String token = jwtService.generateToken("user@test.cl", "EMPLOYEE", 42L);
            assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        }
    }
}