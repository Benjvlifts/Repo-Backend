# 👥 ms-recursos

> **Microservicio de Gestión de Recursos Humanos** — Evaluación Parcial 2  
> Asignatura: DSY1106 – Desarrollo Fullstack III | Instituto DuocUC | 2026  
> Estudiantes: **Benjamín Valdés** · **Ignacio Muñoz**

---

## 📌 Descripción del Microservicio

`ms-recursos` gestiona el capital humano de Innovatech Solutions. Permite administrar la disponibilidad de los consultores y desarrolladores, así como su asignación a proyectos activos.

### Responsabilidades
- Registro y mantenimiento de perfiles profesionales.
- Control de disponibilidad (Available/Busy).
- Consumo de eventos de Kafka para reaccionar ante nuevos proyectos.

---

## 🎨 Patrones de Diseño Aplicados

### 1. Repository Pattern
Utilizado para la gestión de la entidad `Resource`, separando la lógica de negocio en `ResourceService` de la persistencia en PostgreSQL.

### 2. Event-Driven Architecture (Consumer)
El componente `ProjectEventConsumer` escucha el topic `innovatech.project.created`. Cuando se crea un proyecto en el sistema, este servicio recibe la notificación para iniciar el proceso de sugerencia o pre-asignación de personal.

---

## 🛠️ Tecnologías

- **Java 21** & **Spring Boot 3.4**
- **Spring Kafka**: Para el consumo de mensajes asíncronos.
- **Spring Data JPA** & **PostgreSQL**.
- **Lombok**: Para simplificación de POJOs.

---

## 📁 Estructura del Proyecto

```
ms-recursos/
├── src/main/java/com/innovatech/recursos/
│   ├── controller/   # ResourceController
│   ├── dto/          # CreateResourceRequest, ResourceResponse
│   ├── messaging/    # ProjectEventConsumer (Kafka)
│   ├── model/        # Entidad Resource
│   ├── repository/   # IResourceRepository
│   └── service/      # ResourceService
└── src/main/resources/
    └── application.properties # Configuración Kafka y DB
```

---

## 📥 Instalación

1. Crear base de datos `innovatech_recursos_db`.
2. Asegurar que Kafka esté disponible en `localhost:9092`.
3. Ejecutar:
```bash
mvn clean spring-boot:run
```
El servicio estará disponible en el puerto **8083**.

---

## 👥 Contribuidores
| Estudiante | GitHub |
|-----------|--------|
| Benjamín Valdés | @benjaminvaldes |
| Ignacio Muñoz | @ignacionunoz |

*Instituto DuocUC — 2026*
