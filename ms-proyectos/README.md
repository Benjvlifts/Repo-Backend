# 🏗️ ms-proyectos

> **Microservicio de Gestión de Proyectos** — Evaluación Parcial 2  
> Asignatura: DSY1106 – Desarrollo Fullstack III | Instituto DuocUC | 2026  
> Estudiantes: **Benjamín Valdés** · **Ignacio Muñoz**

---

## 📑 Tabla de Contenidos

1. [Descripción del Microservicio](#descripción-del-microservicio)
2. [Tecnologías Utilizadas](#tecnologías-utilizadas)
3. [Patrones de Diseño Aplicados](#patrones-de-diseño-aplicados)
4. [Estructura del Proyecto](#estructura-del-proyecto)
5. [Configuración y Ejecución](#configuración-y-ejecución)
6. [Endpoints de la API](#endpoints-de-la-api)
7. [Mensajería Asíncrona (Kafka)](#mensajería-asíncrona-kafka)
8. [Resolución de Problemas](#resolución-de-problemas)

---

## 📌 Descripción del Microservicio

`ms-proyectos` es el núcleo transaccional de la plataforma Innovatech Solutions. Se encarga de la gestión del ciclo de vida de los proyectos (Software, Consultoría, Infraestructura), permitiendo su creación, actualización y seguimiento de estados.

### Responsabilidades
- Gestión de proyectos (CRUD).
- Aplicación de lógica de negocio mediante el patrón Factory para diferentes tipos de proyectos.
- Publicación de eventos de creación de proyectos a través de Kafka.
- Gestión de notas de avance y revisiones de estados.

---

## 🛠️ Tecnologías Utilizadas

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 21 | Lenguaje de programación |
| Spring Boot | 3.x | Framework principal |
| Spring Data JPA | 3.x | Acceso a datos relacionales |
| PostgreSQL | 15+ | Motor de base de datos |
| Kafka | 3.x | Broker de mensajería para eventos |
| Lombok | 1.18.x | Productividad y reducción de código |

---

## 🎨 Patrones de Diseño Aplicados

### 1. Factory Method
Se utiliza para instanciar dinámicamente el tipo de proyecto solicitado (`SOFTWARE`, `CONSULTING`, `INFRASTRUCTURE`).
- **Clase Base:** `ProjectFactory`
- **Implementaciones:** `SoftwareProjectFactory`, `ConsultingProjectFactory`, `InfrastructureProjectFactory`.

### 2. Repository Pattern
Abstrae la lógica de persistencia a través de interfaces (`IProjectRepository`, `IProjectNoteRepository`), permitiendo un desacoplamiento total de la tecnología de base de datos.

### 3. Event-Driven Architecture (Producer)
Implementado con `ProjectEventProducer`, que publica un mensaje en el topic `innovatech.project.created` cada vez que se registra un nuevo proyecto.

---

## 📁 Estructura del Proyecto

```
ms-proyectos/
├── src/main/java/com/innovatech/proyectos/
│   ├── config/           # Seguridad y extracción de JWT
│   ├── controller/       # API REST
│   ├── dto/              # Objetos de transferencia de datos
│   ├── messaging/        # Lógica de Kafka (Productor)
│   ├── model/            # Entidades JPA (Project, ProjectNote)
│   ├── patterns/factory/ # Implementación del patrón Factory
│   ├── repository/       # Interfaces e implementaciones JPA
│   └── service/          # Lógica de negocio central
└── src/main/resources/   # application.properties
```

---

## 📥 Configuración y Ejecución

### Requisitos
- PostgreSQL con la DB `innovatech_proyectos_db`.
- Kafka y Zookeeper corriendo (vía Docker Compose en la raíz).

### Ejecución
```bash
./mvnw clean spring-boot:run
```
El servicio corre en el puerto **8082**.

---

## 📡 Endpoints de la API (vía BFF)

| Método | Path | Descripción |
|--------|------|-------------|
| GET | `/api/projects` | Lista todos los proyectos |
| POST | `/api/projects` | Crea un proyecto (dispara evento Kafka) |
| GET | `/api/projects/{id}` | Detalle de un proyecto |
| PATCH | `/api/projects/{id}/status` | Actualiza el estado |
| POST | `/api/projects/{id}/notes` | Agrega una nota de avance |

---

## 👥 Contribuidores
| Estudiante | Rol |
|-----------|-----|
| Benjamín Valdés | Arquitectura + Kafka |
| Ignacio Muñoz | Patrón Factory + API |

*Instituto DuocUC — 2026*
