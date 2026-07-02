# Innovatech Solutions — Ecosistema Backend

> **Arquitectura distribuida orientada a microservicios** · Java 21 + Spring Boot 3.4.5 · Node.js/Express (BFF) · Kong Gateway · Apache Kafka (EDA) · PostgreSQL 15

Asignatura: Desarrollo Fullstack III (DSY1106) — DuocUC, 2026
Autores: Benjamín Valdés e Ignacio Muñoz

---

## 📑 Tabla de Contenidos

1. [Descripción Arquitectónica](#-descripción-arquitectónica)
2. [Topología de Microservicios](#-topología-de-microservicios)
3. [Patrones de Diseño Aplicados](#-patrones-de-diseño-aplicados)
4. [Persistencia y Mensajería](#-persistencia-y-mensajería)
5. [Prerrequisitos e Instrucciones de Despliegue](#-prerrequisitos-e-instrucciones-de-despliegue)
6. [Aseguramiento de Calidad (QA)](#-aseguramiento-de-calidad-qa)

---

## 📌 Descripción Arquitectónica

El backend de Innovatech Solutions está estructurado como un **sistema distribuido de cuatro capas**, diseñado para desacoplar el tráfico de cliente de la lógica de negocio y aislar fallos de disponibilidad entre servicios:

```
Cliente (SPA React)
        │
        ▼
┌───────────────────┐
│   Kong Gateway     │  ← Enrutamiento perimetral, JWT, CORS, Rate Limiting
│   (puertos 8000/   │
│    8001 admin)     │
└─────────┬──────────┘
          ▼
┌───────────────────┐
│   BFF (Node.js/    │  ← Agregación de peticiones, Circuit Breaker,
│   Express, :3000)  │     Helmet, Rate Limiting, CORS dinámico
└─────────┬──────────┘
          ▼
┌─────────────────────────────────────────────────────────────┐
│              Microservicios Core (Spring Boot)                │
│   ms-auth · ms-proyectos · ms-recursos · ms-analitica · ms-notif│
└─────────┬───────────────────────────────────────┬─────────────┘
          ▼                                       ▼
   PostgreSQL 15                            Apache Kafka
   (1 base de datos por servicio)      (comunicación asíncrona EDA)
```

El **API Gateway (Kong)** concentra las políticas de seguridad perimetral (validación JWT, CORS, límites de tasa) antes de que el tráfico llegue al **BFF**, que actúa como capa de orquestación y agregación exclusiva para el cliente React, aislando a este último de la complejidad interna de la red de microservicios. Cada microservicio Spring Boot expone su propia API REST documentada vía OpenAPI/Swagger y persiste en una base de datos PostgreSQL independiente, comunicándose de forma asíncrona con el resto del ecosistema a través de Apache Kafka.

---

## 🗂 Topología de Microservicios

| Microservicio | Responsabilidad | Puerto | Base de Datos | Swagger UI |
|---|---|---|---|---|
| **ms-auth** | Autenticación y gestión de usuarios (JWT, RBAC) | `8081` | `auth_db` | `/swagger-ui.html` |
| **ms-proyectos** | Gestión de proyectos, hitos y notas | `8082` | `proyectos_db` | `/swagger-ui.html` |
| **ms-recursos** | Control y asignación de personal a proyectos | `8083` | `recursos_db` | `/swagger-ui.html` |
| **ms-analitica** | Cómputo de KPIs y métricas agregadas | `8084` | `analitica_db` | `/swagger-ui.html` |
| **ms-notif** | Despacho y consulta de notificaciones/alertas | `8085` | `notif_db` | `/swagger-ui.html` |
| **BFF** (Node.js/Express) | Orquestación y agregación para el cliente SPA | `3000` | — | — |
| **Kong Gateway** | Enrutamiento perimetral, JWT, CORS, Rate Limiting | `8000` (proxy) / `8001` (admin) | — | — |

Cada microservicio Java expone además un endpoint `/v3/api-docs` (springdoc-openapi) para su especificación OpenAPI en formato JSON.

---

## 🧩 Patrones de Diseño Aplicados

- **Database-per-Service:** cada microservicio es dueño exclusivo de su esquema PostgreSQL (`auth_db`, `proyectos_db`, `recursos_db`, `analitica_db`, `notif_db`), sin *foreign keys* cruzadas a nivel de motor de base de datos. Las relaciones de negocio entre dominios (p. ej. un empleado asignado a un proyecto) se resuelven mediante referencias lógicas (IDs) propagadas por eventos, preservando la independencia de despliegue de cada servicio.

- **Repository Pattern:** la capa de persistencia de cada microservicio abstrae el acceso a datos mediante interfaces de dominio (`IUserRepository`, `IProjectRepository`, `IResourceRepository`, `IProjectNoteRepository`) implementadas por interfaces Spring Data JPA (`JpaUserRepository`, `JpaProjectRepository`, etc.) que extienden `JpaRepository`, manteniendo la lógica de negocio desacoplada de las consultas SQL concretas.

- **Circuit Breaker (BFF):** el componente `CircuitBreaker.js` implementa manualmente una máquina de estados `CLOSED → OPEN → HALF_OPEN` para cada microservicio consumido desde el BFF, evitando fallas en cascada ante la caída o degradación de un servicio crítico (por ejemplo, latencias en `ms-analitica`). Configuración por defecto: `failureThreshold = 5` fallos consecutivos y `timeout = 30000 ms` antes de reintentar en estado `HALF_OPEN`. Ante un fallo con el circuito abierto, el BFF retorna inmediatamente `503 Service Unavailable` sin saturar la red interna.

- **Factory Method (ms-proyectos):** la creación de proyectos está estructurada bajo un patrón creacional (`ProjectFactory`, `ProjectFactoryProvider` y las fábricas concretas `SoftwareProjectFactory`, `ConsultingProjectFactory`, `InfrastructureProjectFactory`), que instancia dinámicamente el tipo correcto de proyecto (Software, Consultoría o Infraestructura) con sus parámetros y esquemas de hitos base, sin acoplar esta lógica al servicio de aplicación.

- **Consistencia Eventual con Apache Kafka:** dado que no existen transacciones distribuidas entre microservicios, la sincronización de estado entre dominios (proyectos → recursos, proyectos → analítica, proyectos → notificaciones) se resuelve mediante un modelo de **Event-Driven Architecture (EDA)**, donde `ms-proyectos` publica eventos de dominio que los demás servicios consumen de forma asíncrona para actualizar su propio estado (ver sección siguiente).

- **Architectural Slicing en Testing:** `@WebMvcTest` + `@MockBean` para aislar exclusivamente la capa web (controladores) del contexto completo de Spring, y `@Mock`/`@InjectMocks` (Mockito) para aislar la lógica de servicios de sus dependencias de persistencia y mensajería.

---

## 🗄 Persistencia y Mensajería

### PostgreSQL — Database per Service

El script `scripts/init-dbs.sql` aprovisiona las cinco bases de datos independientes al iniciar el contenedor `postgres`:

```sql
CREATE DATABASE auth_db;
CREATE DATABASE proyectos_db;
CREATE DATABASE recursos_db;
CREATE DATABASE analitica_db;
CREATE DATABASE notif_db;
```

La persistencia a nivel de código se maneja con **Spring Data JPA + Hibernate**, usando `spring.jpa.hibernate.ddl-auto=update` para traducir automáticamente las entidades anotadas (`@Entity`) a tablas, aplicando las restricciones de integridad declaradas (`@Column(nullable = false, unique = true)`, etc.).

### Apache Kafka — Tópicos y Eventos de Dominio

`ms-proyectos` actúa como **productor** de eventos de dominio sobre el tópico `innovatech.project.created`, publicando distintos `eventType` según la acción de negocio:

| `eventType` | Disparado por | Consumido por |
|---|---|---|
| `PROJECT_CREATED` | Creación de un proyecto | `ms-analitica`, `ms-notif` |
| `STATUS_CHANGED` | Cambio de estado de un proyecto | `ms-recursos` (libera personal si `COMPLETED`/`CANCELLED`), `ms-analitica`, `ms-notif` |
| `RESOURCE_ASSIGNED` | Asignación de un empleado a un proyecto | `ms-recursos` (actualiza estado del recurso) |
| `RESOURCE_UNASSIGNED` | Desasignación de un empleado | `ms-recursos` (libera el recurso) |

**Consumidores por microservicio:**

- **`ms-recursos`** escucha únicamente `innovatech.project.created` (`ProjectEventConsumer`) y reacciona según `eventType`, delegando en `ResourceService` la asignación/liberación de personal.
- **`ms-analitica`** y **`ms-notif`** escuchan tanto `innovatech.project.created` como el tópico legado `project-events`, para mantener compatibilidad con productores previos del ecosistema.

Cada consumidor opera bajo su propio `group-id` (`ms-recursos-group`, `analitica-group`, `notif-group`), permitiendo que los cinco microservicios evolucionen sus modelos de lectura de forma independiente sin bloquear al productor. Este diseño garantiza que, ante la caída temporal de un consumidor (p. ej. `ms-notif`), el resto del ecosistema (`ms-recursos`, `ms-analitica`) continúe operando con normalidad, ya que Kafka retiene los eventos hasta que el consumidor vuelva a estar disponible.

---

## 🚀 Prerrequisitos e Instrucciones de Despliegue

### Prerrequisitos

| Herramienta | Versión | Uso |
|---|---|---|
| **Docker** y **Docker Compose** | — | Levantar el ecosistema completo (obligatorio) |
| **JDK** | 21 (distribución Temurin) | Compilación y testing local aislado de cada microservicio |
| **Apache Maven** | 3.9+ | Build, testing y análisis estático local |
| **Node.js** | 20.x | Ejecución/desarrollo local del BFF |

### Variables de Entorno

**Compartidas** (usadas por múltiples servicios vía `docker-compose.yml`):

| Variable | Descripción | Consumida por |
|---|---|---|
| `DB_PASSWORD` | Contraseña del usuario `postgres` | `postgres`, los 5 microservicios |
| `JWT_SECRET` | Secreto HMAC compartido para firmar/validar JWT | `ms-auth` (firma), `ms-proyectos` (validación) |

**Específicas por servicio:**

| Variable | Servicio | Valor por defecto (docker-compose) |
|---|---|---|
| `DB_HOST` / `DB_NAME` / `DB_USER` | Todos los MS | `postgres` / `<servicio>_db` / `postgres` |
| `KAFKA_SERVER` | ms-proyectos, ms-recursos, ms-analitica, ms-notif | `kafka:9092` |
| `REPO_BASE_URL` | ms-proyectos | URL base para repositorios autogenerados de proyectos tipo Software |
| `MS_AUTH_URL`, `MS_PROYECTOS_URL`, `MS_RECURSOS_URL`, `MS_ANALITICA_URL`, `MS_NOTIF_URL` | BFF | `http://ms-<servicio>:<puerto>` |
| `CORS_ORIGIN` | BFF | Lista de orígenes permitidos separados por coma (por defecto `http://localhost:5173`) |

> ⚠️ Ningún servicio del ecosistema admite el comodín `*` en CORS cuando `credentials: true` está activo; tanto el BFF como Kong exigen una lista blanca explícita de orígenes.

### Despliegue con Docker Compose

Desde la raíz del repositorio:

```bash
docker-compose up --build
```

Esto levanta, en orden de dependencia: `postgres` (con *healthcheck*), `zookeeper` → `kafka`, `kong` (con configuración declarativa desde `kong-gateway/kong.yml`), los cinco microservicios Spring Boot y el `bff`.

Para detener y limpiar los recursos de red:

```bash
docker-compose down
```

### Perfil de Análisis Estático Local (opcional)

El proyecto incluye un perfil adicional de Docker Compose para auditar calidad de código **sin depender de la nube**, levantando una instancia local de SonarQube respaldada por su propia base PostgreSQL:

```bash
docker-compose --profile analisis up -d sonarqube sonarqube-db
```

SonarQube queda disponible en `http://localhost:9000`.

### Verificación de Salud

El BFF expone un endpoint de *health check* que incluye el estado en tiempo real de cada Circuit Breaker hacia los microservicios:

```
GET http://localhost:3000/health
```

---

## ✅ Aseguramiento de Calidad (QA)

### Resultados Actuales de SonarCloud

Tras la refactorización del ecosistema (extracción de `JWT_SECRET` a variables de entorno y corrección de *code smells*), el repositorio backend aprueba el **Quality Gate con calificación A** en todas las dimensiones evaluadas:

| Métrica | Resultado |
|---|---|
| **Security Rating** | A (0 vulnerabilidades) |
| **Reliability** | A |
| **Maintainability** | A |
| **Hotspots Reviewed** | 100% |
| **Duplications** | 0.0% |

El análisis se ejecuta automáticamente vía GitHub Actions (`sonar-maven-plugin` v5.0.0.4389) apuntando a la organización `innovatech-org` en `sonarcloud.io`, e inyectando el secreto `SONAR_TOKEN` de forma encriptada. El job de SonarCloud depende del éxito previo de la suite de tests (`needs: test`) y realiza una clonación completa del historial (`fetch-depth: 0`) para permitir la atribución de autoría por línea (*Git Blame*).

### Testing Unitario (JUnit 5 + Mockito)

La lógica de negocio se valida mediante pruebas unitarias aisladas en las cinco capas de servicio:

- **`@Mock` / `@InjectMocks`** (Mockito) para simular repositorios y dependencias de persistencia sin levantar contexto de base de datos real.
- **`@WebMvcTest` + `MockMvc` + `@MockBean`** para la capa de controladores REST, restringiendo el contexto de Spring exclusivamente a la infraestructura web y validando códigos de estado HTTP, *parsing* de payloads JSON y el manejador global de excepciones (`GlobalExceptionHandler`) ante errores de negocio controlados.
- **`ReflectionTestUtils`** (Spring) en `JwtServiceTest`, para manipular en tiempo de prueba propiedades como el tiempo de expiración del token y validar el comportamiento criptográfico ante casos límite (tokens expirados, firmas inválidas).
- **H2 Database** en memoria (`jdbc:h2:mem:testdb`, ciclo de vida `create-drop`) como sustituto de PostgreSQL durante la ejecución de tests en CI, garantizando aislamiento absoluto entre corridas concurrentes.

### Cobertura de Código (JaCoCo)

El plugin `jacoco-maven-plugin` (v0.8.12) está integrado en la fase `verify` del ciclo de vida de Maven en los cinco microservicios, bloqueando la compilación si no se alcanza el umbral mínimo configurado:

| Microservicio | Umbral mínimo (`COVEREDRATIO` sobre `INSTRUCTION`) |
|---|---|
| ms-auth | 60% |
| ms-recursos | 60% |
| ms-analitica | 60% |
| ms-notif | 60% |
| ms-proyectos | 50% |

> 📂 **Los reportes de cobertura HTML se generan localmente en la ruta `<microservicio>/target/site/jacoco/index.html`** tras ejecutar el comando de Maven indicado abajo. Ábrase directamente en el navegador para inspeccionar cobertura por clase, línea y rama.

### Ejecución Local de Pruebas y Reportes

Desde la carpeta de cada microservicio (`ms-auth`, `ms-proyectos`, `ms-recursos`, `ms-analitica`, `ms-notif`):

```bash
mvn verify
```

Este comando compila, ejecuta la suite completa de JUnit 5/Mockito, genera el reporte JaCoCo y aplica el *check* de umbral mínimo de cobertura.

Para además publicar el análisis a SonarCloud:

```bash
mvn verify sonar:sonar -Dsonar.token=TU_TOKEN_AQUI
```

### Pipeline de Integración Continua (`backend-ci.yml`)

El workflow se dispara ante `push` o `pull_request` hacia `main`/`master` y opera con **estrategia de matriz** sobre los cinco microservicios, en contenedores `ubuntu-latest` con **JDK 21 (Temurin)**:

1. **Job `test`:** ejecuta `mvn verify` por servicio (tests + reporte + *check* de umbral JaCoCo) y sube el reporte como artefacto (`jacoco-report-<servicio>`, retención de 7 días).
2. **Job `sonarcloud`:** depende del éxito del job anterior (`needs: test`), realiza checkout completo del historial y ejecuta `mvn verify sonar:sonar` inyectando `${{ secrets.SONAR_TOKEN }}`.

---

### 🔭 Recomendaciones de Mejora Continua

- **Testcontainers:** sustituir progresivamente H2 y los dobles de prueba de Kafka (`@MockBean`) por instancias reales y efímeras de PostgreSQL y Apache Kafka dentro del pipeline de CI, reduciendo discrepancias de sintaxis SQL y de orquestación de tópicos respecto al entorno de producción.
- **Análisis estático del Frontend:** integrar un escáner formal (SonarCloud o linter corporativo estricto) para homologar la detección de *code smells* en ambas capas tecnológicas del ecosistema.
