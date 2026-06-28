package com.innovatech.proyectos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innovatech.proyectos.config.JwtExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas unitarias para JwtExtractor.
 * Valida: extracción de claims, roles, userId, subject desde header Authorization.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@DisplayName("JwtExtractor — Pruebas Unitarias")
class JwtExtractorTest {

    private JwtExtractor jwtExtractor;

    // JWT de prueba con payload: {"sub":"user@test.cl","role":"ADMIN","userId":1,"exp":9999999999}
    private String validJwtHeader;

    @BeforeEach
    void setUp() throws Exception {
        jwtExtractor = new JwtExtractor(new ObjectMapper());

        // Construir JWT falso (sin firma real, solo para testear el parsing del payload)
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());

        Map<String, Object> payload = Map.of(
                "sub", "user@test.cl",
                "role", "ADMIN",
                "userId", 1,
                "exp", 9999999999L
        );
        String payloadStr = new ObjectMapper().writeValueAsString(payload);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadStr.getBytes());

        validJwtHeader = "Bearer " + header + "." + encodedPayload + ".fakesignature";
    }

    @Nested
    @DisplayName("extractClaims()")
    class ExtractClaimsTests {

        @Test
        @DisplayName("✅ Extrae claims correctamente desde JWT válido")
        void extractClaims_validJwt_returnsClaims() {
            Map<String, Object> claims = jwtExtractor.extractClaims(validJwtHeader);

            assertThat(claims).isNotEmpty();
            assertThat(claims.get("sub")).isEqualTo("user@test.cl");
            assertThat(claims.get("role")).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("✅ Retorna mapa vacío si header es null")
        void extractClaims_nullHeader_returnsEmptyMap() {
            Map<String, Object> claims = jwtExtractor.extractClaims(null);
            assertThat(claims).isEmpty();
        }

        @Test
        @DisplayName("✅ Retorna mapa vacío si header no empieza con 'Bearer '")
        void extractClaims_noBearer_returnsEmptyMap() {
            Map<String, Object> claims = jwtExtractor.extractClaims("Basic token123");
            assertThat(claims).isEmpty();
        }

        @Test
        @DisplayName("✅ Retorna mapa vacío si JWT tiene formato incorrecto")
        void extractClaims_malformedJwt_returnsEmptyMap() {
            Map<String, Object> claims = jwtExtractor.extractClaims("Bearer notajwt");
            assertThat(claims).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractRole()")
    class ExtractRoleTests {

        @Test
        @DisplayName("✅ Extrae rol ADMIN desde JWT")
        void extractRole_adminJwt_returnsAdmin() {
            assertThat(jwtExtractor.extractRole(validJwtHeader)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("✅ Retorna string vacío si no hay header")
        void extractRole_nullHeader_returnsEmptyString() {
            assertThat(jwtExtractor.extractRole(null)).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("extractUserId()")
    class ExtractUserIdTests {

        @Test
        @DisplayName("✅ Extrae userId como Long desde JWT")
        void extractUserId_validJwt_returnsUserId() {
            Long userId = jwtExtractor.extractUserId(validJwtHeader);
            assertThat(userId).isEqualTo(1L);
        }

        @Test
        @DisplayName("✅ Retorna null si no hay userId en el JWT")
        void extractUserId_noUserId_returnsNull() {
            assertThat(jwtExtractor.extractUserId(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Helpers de rol")
    class RoleHelperTests {

        @Test
        @DisplayName("✅ isAdmin retorna true para token ADMIN")
        void isAdmin_adminToken_returnsTrue() {
            assertThat(jwtExtractor.isAdmin(validJwtHeader)).isTrue();
        }

        @Test
        @DisplayName("✅ isManager retorna false para token ADMIN")
        void isManager_adminToken_returnsFalse() {
            assertThat(jwtExtractor.isManager(validJwtHeader)).isFalse();
        }

        @Test
        @DisplayName("✅ isAdminOrManager retorna true para token ADMIN")
        void isAdminOrManager_adminToken_returnsTrue() {
            assertThat(jwtExtractor.isAdminOrManager(validJwtHeader)).isTrue();
        }

        @Test
        @DisplayName("✅ isEmployee retorna false para token ADMIN")
        void isEmployee_adminToken_returnsFalse() {
            assertThat(jwtExtractor.isEmployee(validJwtHeader)).isFalse();
        }
    }
}