# 🔐 ms-auth

> **Microservicio de Autenticación** — Evaluación Parcial 2  
> Asignatura: DSY1106 – Desarrollo Fullstack III | Instituto DuocUC | 2026  
> Estudiantes: **Benjamín Valdés** · **Ignacio Muñoz**

---

## 📑 Tabla de Contenidos

1. [Descripción del Microservicio](#descripción-del-microservicio)
2. [Tecnologías y Arquetipo Maven](#tecnologías-y-arquetipo-maven)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Patrones de Diseño Aplicados](#patrones-de-diseño-aplicados)
5. [Requisitos Previos](#requisitos-previos)
6. [Configuración de la Base de Datos](#configuración-de-la-base-de-datos)
7. [Instalación y Ejecución](#instalación-y-ejecución)
8. [Variables de Configuración](#variables-de-configuración)
9. [Endpoints de la API](#endpoints-de-la-api)
10. [Autenticación JWT – Flujo Completo](#autenticación-jwt--flujo-completo)
11. [Pruebas Unitarias](#pruebas-unitarias)
12. [Cómo Usar el Arquetipo para Generar Nuevos Proyectos](#cómo-usar-el-arquetipo-para-generar-nuevos-proyectos)
13. [Resolución de Problemas Comunes](#resolución-de-problemas-comunes)

---

## 📌 Descripción del Microservicio

`ms-auth` es el microservicio responsable de la **autenticación y autorización** de la plataforma Innovatech Solutions. Gestiona el registro de usuarios, el inicio de sesión y la validación de tokens JWT, garantizando que solo los usuarios autorizados puedan acceder a los recursos del sistema.

### Responsabilidades

- Registro de nuevos usuarios con hash seguro de contraseñas (BCrypt)
- Generación y firma de tokens JWT para sesiones autenticadas
- Validación de tokens en peticiones entrantes
- Gestión de roles de usuario (ADMIN, MANAGER, DEVELOPER)
- Exposición de perfil del usuario autenticado

### Posición en la Arquitectura

```
[Frontend] → [BFF :3001] → [Kong :8000] → [ms-auth :8081]
                                                ↓
                                         [PostgreSQL - innovatech_auth_db]
```

---

## 🛠️ Tecnologías y Arquetipo Maven

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 17 | Lenguaje de programación |
| Spring Boot | 3.x | Framework principal |
| Spring Security | 6.x | Seguridad y autenticación |
| Spring Data JPA | 3.x | Acceso a datos (ORM) |
| Hibernate | 6.x | Implementación JPA |
| PostgreSQL Driver | 42.x | Conexión a base de datos |
| JJWT (io.jsonwebtoken) | 0.12.x | Generación y validación de JWT |
| BCrypt (Spring Security) | (integrado) | Hash de contraseñas |
| Lombok | 1.18.x | Reducción de boilerplate |
| JUnit 5 | 5.x | Framework de pruebas unitarias |
| Mockito | 5.x | Mocking de dependencias en tests |
| Maven Wrapper | 3.9.x | Ejecución de Maven sin instalación global |

### Arquetipo Maven Utilizado

```
GroupId (padre):    org.springframework.boot
ArtifactId (padre): spring-boot-starter-parent
Versión:            3.x.x
```

Este proyecto fue generado con **Spring Initializr** (`start.spring.io`) usando las dependencias: Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver y Lombok. El arquetipo Maven de Spring Boot establece convenciones de estructura de directorios, gestión de versiones de dependencias y configuración de plugins (spring-boot-maven-plugin).

---

## 📁 Estructura del Proyecto

```
ms-auth/
├── src/
│   ├── main/
│   │   ├── java/com/innovatech/auth/
│   │   │   ├── config/
│   │   │   │   ├── GlobalExceptionHandler.java  # Manejo centralizado de excepciones
│   │   │   │   └── SecurityConfig.java          # Configuración de Spring Security
│   │   │   ├── controller/
│   │   │   │   └── AuthController.java          # Endpoints REST de autenticación
│   │   │   ├── dto/
│   │   │   │   └── AuthDtos.java                # DTOs de request/response
│   │   │   ├── model/
│   │   │   │   └── User.java                    # Entidad JPA Usuario
│   │   │   ├── repository/
│   │   │   │   ├── IUserRepository.java         # Interfaz del repositorio
│   │   │   │   └── JpaUserRepository.java       # Implementación JPA
│   │   │   ├── security/
│   │   │   │   └── JwtService.java              # Generación y validación de JWT
│   │   │   ├── service/
│   │   │   │   └── AuthService.java             # Lógica de negocio de autenticación
│   │   │   └── MsAuthApplication.java           # Clase principal Spring Boot
│   │   └── resources/
│   │       └── application.properties           # Configuración de la aplicación
│   └── test/
│       └── java/com/innovatech/auth/
│           ├── AuthServiceTest.java             # Pruebas unitarias de AuthService
│           └── MsAuthApplicationTests.java      # Test de carga del contexto Spring
├── .mvn/wrapper/
│   └── maven-wrapper.properties                 # Configuración del Maven Wrapper
├── mvnw                                         # Maven Wrapper (Linux/Mac)
├── mvnw.cmd                                     # Maven Wrapper (Windows)
└── pom.xml                                      # Descriptor Maven del proyecto
```

---

## 🎨 Patrones de Diseño Aplicados

### Repository Pattern

`ms-auth` implementa el **Repository Pattern** en su capa de acceso a datos:

```java
// IUserRepository.java — Interfaz abstracta del repositorio
public interface IUserRepository {
    Optional<User> findByUsername(String username);
    User save(User user);
    boolean existsByUsername(String username);
}

// JpaUserRepository.java — Implementación JPA concreta
@Repository
public class JpaUserRepository implements IUserRepository {
    @Autowired
    private UserJpaRepository jpaRepository; // Spring Data JPA bajo el capó
    
    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username);
    }
    // ...
}
```

**Beneficio:** `AuthService` nunca accede directamente a JPA ni a PostgreSQL. Si mañana se cambia a MongoDB, solo se modifica `JpaUserRepository` sin tocar la lógica de negocio.

---

## ✅ Requisitos Previos

1. **Java 17 o superior** — [Descargar en adoptium.net](https://adoptium.net)
2. **Maven 3.9+** — O usar el Maven Wrapper incluido (`./mvnw`)
3. **PostgreSQL 15+** — Base de datos corriendo localmente o via Docker
4. **Base de datos creada** — `innovatech_auth_db`

Verificar instalaciones:
```bash
java -version          # Debe mostrar openjdk 17 o superior
./mvnw --version       # Debe mostrar Apache Maven 3.9.x
```

---

## 🗄️ Configuración de la Base de Datos

### Opción 1: Docker (Recomendado)

La base de datos se levanta automáticamente con Docker Compose desde el directorio raíz del proyecto:

```bash
# Desde el directorio raíz del proyecto (donde está docker-compose.yml)
docker-compose up -d postgres-auth
```

Esto crea automáticamente:
- Contenedor PostgreSQL en `localhost:5432`
- Base de datos `innovatech_auth_db`
- Usuario: `postgres` / Password: `postgres`

### Opción 2: PostgreSQL Local

```sql
-- Crear la base de datos manualmente en PostgreSQL
CREATE DATABASE innovatech_auth_db;
CREATE USER innovatech WITH PASSWORD 'innovatech123';
GRANT ALL PRIVILEGES ON DATABASE innovatech_auth_db TO innovatech;
```

> Hibernate creará las tablas automáticamente al iniciar el microservicio (`spring.jpa.hibernate.ddl-auto=update`).

---

## 📥 Instalación y Ejecución

### Paso 1: Clonar el repositorio

```bash
git clone https://github.com/TU_USUARIO/ms-auth.git
cd ms-auth
```

### Paso 2: Configurar application.properties

Editar `src/main/resources/application.properties` con los datos de tu base de datos:

```properties
# Verificar que estos valores coincidan con tu configuración de PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/innovatech_auth_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### Paso 3: Compilar el proyecto

```bash
# Linux/Mac
./mvnw clean package -DskipTests

# Windows
mvnw.cmd clean package -DskipTests
```

**Salida esperada:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 15.432 s
[INFO] Finished at: 2025-05-XX
```

### Paso 4: Ejecutar el microservicio

**Opción A: Con Maven Wrapper (desarrollo)**
```bash
# Linux/Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

**Opción B: Con el JAR generado (producción)**
```bash
java -jar target/ms-auth-0.0.1-SNAPSHOT.jar
```

**Salida esperada:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.x.x)

...
[main] c.i.auth.MsAuthApplication : Started MsAuthApplication in 4.231 seconds
```

El microservicio queda disponible en **http://localhost:8081**

---

## ⚙️ Variables de Configuración

`src/main/resources/application.properties`:

```properties
# Servidor
server.port=8081

# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/innovatech_auth_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT
jwt.secret=innovatech-super-secret-key-2025-must-be-at-least-256-bits
jwt.expiration=86400000
```

> ⚠️ En producción, el `jwt.secret` debe ser una cadena aleatoria de al menos 32 caracteres, almacenada como variable de entorno, nunca hardcodeada.

---

## 📡 Endpoints de la API

El microservicio expone los siguientes endpoints (normalmente accedidos a través de Kong en `:8000`, no directamente):

### POST `/auth/register`

Registra un nuevo usuario en el sistema.

**Request Body:**
```json
{
  "username": "jdoe",
  "email": "jdoe@innovatech.com",
  "password": "SecurePass123!",
  "rol": "DEVELOPER"
}
```

**Response 201 Created:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "jdoe",
    "email": "jdoe@innovatech.com",
    "rol": "DEVELOPER"
  }
}
```

**Response 400 Bad Request (usuario duplicado):**
```json
{
  "error": "El usuario 'jdoe' ya existe en el sistema"
}
```

### POST `/auth/login`

Autentica un usuario existente.

**Request Body:**
```json
{
  "username": "jdoe",
  "password": "SecurePass123!"
}
```

**Response 200 OK:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "jdoe",
    "email": "jdoe@innovatech.com",
    "rol": "DEVELOPER"
  }
}
```

**Response 401 Unauthorized:**
```json
{
  "error": "Credenciales inválidas"
}
```

### GET `/auth/profile`

Retorna el perfil del usuario autenticado (requiere JWT válido).

**Headers requeridos:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response 200 OK:**
```json
{
  "id": 1,
  "username": "jdoe",
  "email": "jdoe@innovatech.com",
  "rol": "DEVELOPER"
}
```

---

## 🔑 Autenticación JWT – Flujo Completo

```
1. POST /auth/login { username, password }
2. AuthController.login() → AuthService.login(request)
3. AuthService: 
   a. IUserRepository.findByUsername(username) → busca en PostgreSQL
   b. BCrypt.matches(password, user.passwordHash) → valida contraseña
   c. JwtService.generateToken(user) → firma JWT con HMAC-SHA256
4. Retorna { token, user } al BFF
5. BFF → retorna { token, user } al Frontend
6. Frontend: almacena token en localStorage
7. Peticiones futuras incluyen: Authorization: Bearer <token>
8. Spring Security intercepta la petición → JwtAuthenticationFilter
9. JwtService.validateToken(token) → verifica firma y expiración
10. Si válido → continúa con la petición
11. Si inválido → retorna 401 Unauthorized
```

---

## 🧪 Pruebas Unitarias

### Ejecutar todas las pruebas

```bash
# Linux/Mac
./mvnw test

# Windows
mvnw.cmd test
```

**Salida esperada:**
```
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Descripción de las Pruebas (AuthServiceTest.java)

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private IUserRepository userRepository;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    @Test
    void register_success()
    // ✅ Registra usuario con datos válidos → retorna token JWT
    
    @Test
    void register_duplicate_user()
    // ✅ Registro con username existente → lanza BadRequestException
    
    @Test
    void login_success()
    // ✅ Login con credenciales válidas → retorna token JWT + perfil
    
    @Test
    void login_invalid_password()
    // ✅ Login con contraseña incorrecta → lanza UnauthorizedException
    
    @Test
    void login_nonexistent_user()
    // ✅ Login con usuario inexistente → lanza NotFoundException
}
```

---

## 🏗️ Cómo Usar el Arquetipo para Generar Nuevos Proyectos

Para generar un nuevo microservicio Spring Boot con la misma estructura base que `ms-auth`:

### Opción 1: Spring Initializr (Recomendado)

```bash
# Usando curl
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.3.0 \
  -d baseDir=ms-nuevo \
  -d groupId=com.innovatech \
  -d artifactId=ms-nuevo \
  -d name=ms-nuevo \
  -d packageName=com.innovatech.nuevo \
  -d javaVersion=17 \
  -d dependencies=web,data-jpa,security,postgresql,lombok \
  -o ms-nuevo.zip

unzip ms-nuevo.zip
cd ms-nuevo
```

### Opción 2: Maven Archetype Generate

```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.springframework.boot \
  -DarchetypeArtifactId=spring-boot-sample-web-static-archetype \
  -DgroupId=com.innovatech \
  -DartifactId=ms-nuevo \
  -Dversion=0.0.1-SNAPSHOT \
  -Dpackage=com.innovatech.nuevo
```

---

## 🩺 Resolución de Problemas Comunes

### ❌ Error: "Unable to acquire JDBC Connection"

**Causa:** PostgreSQL no está corriendo o las credenciales son incorrectas.  
**Solución:**
```bash
# Verificar que PostgreSQL esté corriendo
docker ps | grep postgres
# o
pg_ctl status

# Verificar la URL en application.properties
# spring.datasource.url=jdbc:postgresql://localhost:5432/innovatech_auth_db
```

### ❌ Error: "Port 8081 already in use"

**Causa:** Hay otro proceso usando el puerto 8081.  
**Solución:**
```bash
# En Linux/Mac
lsof -i :8081 | grep LISTEN
kill -9 <PID>

# Alternativa: cambiar el puerto en application.properties
# server.port=8082
```

### ❌ Error: "InvalidKeyException" al generar JWT

**Causa:** El `jwt.secret` en `application.properties` es demasiado corto para HMAC-SHA256 (mínimo 256 bits = 32 caracteres ASCII).  
**Solución:** Asegurarse de que `jwt.secret` tenga al menos 32 caracteres.

### ❌ BUILD FAILURE: "Cannot find symbol" durante compilación

**Causa:** Versión de Java incorrecta.  
**Solución:**
```bash
java -version  # Debe ser 17 o superior
# Si tienes múltiples versiones de Java, configurar JAVA_HOME:
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

### ❌ Tests fallan con "NullPointerException"

**Causa:** Falta de inicialización del mock en las pruebas.  
**Solución:** Verificar que la clase de test tenga `@ExtendWith(MockitoExtension.class)` y los campos estén anotados con `@Mock` e `@InjectMocks`.

---

## 👥 Contribuidores

| Estudiante | GitHub | Rol Principal |
|-----------|--------|---------------|
| Benjamín Valdés | @benjaminvaldes | ms-auth (implementación principal) |
| Ignacio Muñoz | @ignacionunoz | ms-proyectos + revisión de código |

---

**Instituto DuocUC — 2026 — DSY1106 Desarrollo Fullstack III **
