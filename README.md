# ⚙️ Innovatech Solutions - Backend Ecosystem

> **Arquitectura Orientada a Microservicios y Eventos**
> Ecosistema backend distribuido, resiliente y escalable para la
plataforma Innovatech Solutions.

---

## 📑 Tabla de Contenidos
1. [Descripción Arquitectónica](#1-descripción-arquitectónica)
2. [Topología de Microservicios](#2-topología-de-microservicios)
3. [Patrones de Diseño Aplicados](#3-patrones-de-diseño-aplicados)
4. [Persistencia y Mensajería](#4-persistencia-y-mensajería)
5. [Prerrequisitos e Instrucciones de
Despliegue](#5-prerrequisitos-e-instrucciones-de-despliegue)
6. [Aseguramiento de Calidad (QA)](#6-aseguramiento-de-calidad-qa)

---

## 1. Descripción Arquitectónica

El backend de Innovatech Solutions está estructurado bajo una
arquitectura de microservicios robusta y moderna, diseñada para
garantizar bajo acoplamiento, alta cohesión y resiliencia.

El flujo de peticiones sigue la siguiente topología de red:

**Cliente (SPA)** ➔ **Kong API Gateway (Puerto 8000)** ➔ **BFF Node.js
(Puerto 3000)** ➔ **Microservicios Spring Boot (Java 21)**.

* **Kong API Gateway:** Actúa como perímetro de seguridad, aplicando
Rate Limiting global (1000 req/min), CORS estricto y validación de
tokens JWT en rutas protegidas.
* **BFF (Backend For Frontend):** Desarrollado en Node.js/Express
(v5.2.1). Centraliza las peticiones del frontend, actúa como
intermediario hacia la red interna de microservicios y gestiona la
tolerancia a fallos mediante un disyuntor (Circuit Breaker).
* **Microservicios Core:** Cinco servicios independientes construidos
sobre Java 21 y Spring Boot 3.4.5, comunicados asíncronamente para
procesos diferidos.

---

## 2. Topología de Microservicios

| Servicio | Puerto | Base de Datos | Rol Principal | Dependencias Clave
|
| :--- | :--- | :--- | :--- | :--- |
| **BFF** | `3000` | N/A | Orquestación, Agregación, Circuit Breaker |
Node.js, Express, Axios |
| **ms-auth** | `8081` | `auth_db` | Autenticación, JWT, RBAC, Gestión
de Usuarios | Spring Security, JJWT |
| **ms-proyectos** | `8082` | `proyectos_db` | Gestión de proyectos
(CRUD), Notas de avance, Productor de eventos | Kafka, Spring Data JPA |
| **ms-recursos** | `8083` | `recursos_db` | Asignación y disponibilidad
de RRHH, Consumidor de eventos | Kafka, Spring Data JPA |
| **ms-analitica** | `8084` | `analitica_db` | Cálculo de KPIs,
agregación de métricas, Consumidor de eventos | Kafka, Spring Data JPA |
| **ms-notif** | `8085` | `notif_db` | Despacho de notificaciones
asíncronas, Consumidor de eventos | Kafka, Spring Data JPA |

> *El Gateway perimetral (Kong) expone la red al exterior a través de
los puertos `8000` (Proxy) y `8001` (Admin).*

---

## 3. Patrones de Diseño Aplicados

Para resolver la complejidad inherente a los sistemas distribuidos, se
implementaron los siguientes patrones arquitectónicos y de software:

* **Database-per-Service:** Cada microservicio es dueño absoluto de su
propia base de datos PostgreSQL. No existen *Foreign Keys* duras entre
distintos dominios; las relaciones (ej. `manager_id`,
`assigned_user_id`) son referencias lógicas, garantizando el aislamiento
de fallos.
* **Circuit Breaker (Disyuntor):** Implementado a la medida en la capa
BFF. Monitorea el estado de cada microservicio subyacente. Estados:
`CLOSED`, `OPEN` (tras 5 fallos consecutivos), y `HALF_OPEN` (ventana de
recuperación tras 30 segundos). Retorna códigos `503 Service
Unavailable` controlados o respuestas *fallback* para evitar fallos en
cascada.
* **Consistencia Eventual (EDA - Event-Driven Architecture):**
Implementada mediante Apache Kafka. Permite que la asignación de
recursos o los cambios de estado en un proyecto (ej. pasar a
`COMPLETED`) actualicen métricas y liberen RRHH en otros microservicios
sin acoplamiento temporal ni bloqueos HTTP.
* **Repository Pattern:** A través de Spring Data JPA
(`JpaRepository`) e interfaces de dominio, separando completamente la
lógica de negocio de las consultas SQL.
* **Factory Method:** Aplicado en `ms-proyectos` mediante el
`ProjectFactoryProvider`. Instancia dinámicamente diferentes tipos de
proyectos (`SOFTWARE`, `CONSULTING`, `INFRASTRUCTURE`) asignando lógicas

y atributos por defecto sin modificar el servicio core (Principio
Open/Closed).

---

## 4. Persistencia y Mensajería

### Capa de Datos (PostgreSQL 15)
El clúster levanta 5 bases de datos relacionales independientes
inicializadas mediante el script `init-dbs.sql`:
`auth_db`, `proyectos_db`, `recursos_db`, `analitica_db`, `notif_db`.

### Canal de Mensajería (Apache Kafka 7.6.0)
El sistema utiliza Zookeeper y Kafka para la distribución asíncrona.
* **Tópico Principal:** `innovatech.project.created`
* **Estrategia de Discriminación:** Se utiliza el campo `eventType`
dentro del payload JSON (`ProjectEventMessage` / `ProjectEventDto`) para
que los consumidores ejecuten lógica selectiva.
* **Eventos Clave:**
* `PROJECT_CREATED`: Consumido por Analítica (crea KPI base) y
Notificaciones.
* `STATUS_CHANGED`: Consumido por Recursos (libera personal si el
estado es `COMPLETED` o `CANCELLED`).
* `RESOURCE_ASSIGNED` / `RESOURCE_UNASSIGNED`: Sincroniza la
disponibilidad en el servicio de Recursos Humanos.

*(Nota: Se emplea un workaround estratégico en los consumers
deshabilitando los headers `__TypeId__` de Spring Kafka para permitir la
deserialización de DTOs alojados en distintos paquetes Java entre el
productor y los consumidores).*

---

## 5. Prerrequisitos e Instrucciones de Despliegue

### Requisitos Locales
* Docker y Docker Compose (v2.x+).
* Java JDK 21 (Temurin) y Apache Maven 3.9+ (Para desarrollo y pruebas
en local).
* Node.js 20+ (Para el BFF).

### Variables de Entorno Globales Obligatorias
El archivo `docker-compose.yml` centraliza la orquestación. Se requiere
exportar (o definir en un `.env` raíz) las siguientes variables críticas
antes del despliegue:

```env
DB_PASSWORD=tu_password_seguro
JWT_SECRET=clave_secreta_base64_minimo_512_bits # Compartida entre
ms-auth y ms-proyectos (Kong usa esta misma clave)
KAFKA_SERVER=kafka:9092
REPO_BASE_URL=https://github.com/innovatech/ # Opcional, para
ms-proyectos (Factory de Software)
```

### Ejecución con Docker
Para desplegar todo el clúster (Postgres, Zookeeper, Kafka, Kong,
SonarQube DB, BFF y los 5 MS):

```bash
docker-compose up --build -d
```

---

## 6. Aseguramiento de Calidad (QA)

La deuda técnica y cobertura de código del ecosistema han sido
controlados mediante herramientas de Integración Continua (GitHub
Actions) y análisis estático.

* **Análisis Estático (SonarCloud):**
El código ha sido refactorizado y optimizado, eliminando
vulnerabilidades previas (como la exposición del `JWT_SECRET`).
* **Security Rating:** A (0 Bugs / 0 Vulnerabilidades).
* **Reliability & Maintainability:** A.
* **Testing Unitario (JUnit 5 + Mockito):**
Cada microservicio implementa *Architectural Slicing* utilizando
`@Mock` / `@InjectMocks` para lógica de negocio y criptografía, y
`@WebMvcTest` con `MockMvc` para la capa REST sin levantar el contexto
JPA. El pipeline ejecuta pruebas contra bases de datos en memoria (H2).
* **Métricas de Cobertura (JaCoCo):**
Todos los microservicios cuentan con el plugin `jacoco-maven-plugin`
anclado a la fase `verify`.
* Se ha superado exitosamente el **Umbral General > 60%** exigido
por el *Quality Gate*.
* **Reportes de Cobertura:** Tras la ejecución de `mvn clean
verify`, los reportes HTML navegables se encuentran explícitamente en la
ruta:
`microservicio/target/site/index.html`