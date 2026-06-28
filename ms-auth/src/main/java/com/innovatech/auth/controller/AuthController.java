package com.innovatech.auth.controller;

import com.innovatech.auth.dto.AuthDtos.*;
import com.innovatech.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST del microservicio ms-auth.
 * Expone los endpoints de registro, login, validación de token y consulta de usuarios.
 *
 * @author Benjamin Valdes, Ignacio Munoz
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Autenticación", description = "Registro, login, validación de tokens JWT y consulta de usuarios")
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Registra un nuevo usuario en el sistema.
     */
    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario en el sistema y retorna el token JWT correspondiente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de registro inválidos"),
            @ApiResponse(responseCode = "409", description = "El usuario ya existe")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Autentica un usuario y retorna un token JWT.
     */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario con sus credenciales y retorna un token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/validate
     * Valida un token JWT (usado por el BFF y otros microservicios).
     */
    @PostMapping("/validate")
    @Operation(summary = "Validar token JWT", description = "Verifica si un token JWT es válido. Usado internamente por el BFF y otros microservicios.")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    /**
     * GET /api/auth/users
     * Retorna todos los usuarios del sistema.
     */
    @GetMapping("/users")
    @Operation(summary = "Listar usuarios", description = "Retorna todos los usuarios registrados en el sistema.")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    /**
     * GET /api/auth/users/{id}
     * Retorna un usuario por ID.
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Obtener usuario por ID", description = "Retorna el detalle de un usuario específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    /**
     * GET /api/auth/health
     * Endpoint de salud del microservicio.
     */
    @GetMapping("/health")
    @Operation(summary = "Estado del servicio", description = "Endpoint de health check de ms-auth.")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ms-auth",
                "version", "1.0.0"
        ));
    }
}
