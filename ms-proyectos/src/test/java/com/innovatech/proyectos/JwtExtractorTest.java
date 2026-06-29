package com.innovatech.proyectos;

import com.innovatech.proyectos.config.JwtExtractor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtExtractor — Pruebas Unitarias")
class JwtExtractorTest {

    // Mismo secreto Base64 que application.properties (app.jwt.secret)
    private static final String TEST_SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    private JwtExtractor jwtExtractor;
    private String validJwtHeader;

    @BeforeEach
    void setUp() {
        // FIX: constructor sin parámetros; el secreto se inyecta vía @Value -> ReflectionTestUtils
        jwtExtractor = new JwtExtractor();
        ReflectionTestUtils.setField(jwtExtractor, "jwtSecret", TEST_SECRET);

        SecretKey signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        claims.put("userId", 1);

        String token = Jwts.builder()
                .claims(claims)
                .subject("user@test.cl")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(signingKey)
                .compact();

        validJwtHeader = "Bearer " + token;
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
            assertThat(jwtExtractor.extractClaims(null)).isEmpty();
        }

        @Test
        @DisplayName("✅ Retorna mapa vacío si header no empieza con 'Bearer '")
        void extractClaims_noBearer_returnsEmptyMap() {
            assertThat(jwtExtractor.extractClaims("Basic token123")).isEmpty();
        }

        @Test
        @DisplayName("✅ Retorna mapa vacío si JWT tiene formato incorrecto")
        void extractClaims_malformedJwt_returnsEmptyMap() {
            assertThat(jwtExtractor.extractClaims("Bearer notajwt")).isEmpty();
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
            assertThat(jwtExtractor.extractUserId(validJwtHeader)).isEqualTo(1L);
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