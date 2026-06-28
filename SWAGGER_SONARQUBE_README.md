# Swagger / OpenAPI y SonarQube — Guía de implementación

Este documento explica los cambios agregados al backend de Innovatech Solutions
para cubrir los dos puntos faltantes detectados en la revisión:

1. Especificación de endpoints (Swagger + Postman Collection)
2. Evidencia y entorno de SonarQube (métricas, gráficos y cobertura ≥ 60%)

---

## 1. Swagger / OpenAPI

### Qué se agregó

En **cada uno de los 5 microservicios** (`ms-auth`, `ms-proyectos`, `ms-recursos`,
`ms-analitica`, `ms-notif`):

- Dependencia `springdoc-openapi-starter-webmvc-ui` (versión `2.6.0`) en el `pom.xml`.
- Clase `config/OpenApiConfig.java` con el título, descripción y esquema de
  seguridad Bearer JWT (para los servicios que lo usan).
- Anotaciones `@Tag`, `@Operation` y `@ApiResponses` en los controllers para que
  la documentación generada sea legible y no solo una lista de rutas.
- Configuración explícita de las rutas de Swagger en `application.properties` /
  `application.yml`:
  ```
  springdoc.swagger-ui.path=/swagger-ui.html
  springdoc.api-docs.path=/v3/api-docs
  ```
- En `ms-auth`, que sí exige JWT (`SecurityConfig`), se agregó una excepción
  explícita para que `/swagger-ui/**` y `/v3/api-docs/**` sean accesibles sin
  token (de lo contrario Spring Security bloquea el acceso a la propia
  documentación). `ms-proyectos` y `ms-recursos` ya permitían todo el tráfico
  en su SecurityConfig interno, por lo que no fue necesario tocarlo.

### Cómo verlo funcionando

Con cada microservicio corriendo (ver sus puertos abajo), abre en el navegador:

| Microservicio  | Puerto | Swagger UI                                   |
|----------------|--------|-----------------------------------------------|
| ms-auth        | 8081   | http://localhost:8081/swagger-ui.html         |
| ms-proyectos   | 8082   | http://localhost:8082/swagger-ui.html         |
| ms-recursos    | 8083   | http://localhost:8083/swagger-ui.html         |
| ms-analitica   | 8084   | http://localhost:8084/swagger-ui.html         |
| ms-notif       | 8085   | http://localhost:8085/swagger-ui.html         |

El JSON del contrato OpenAPI (útil para importar en Postman/Insomnia o adjuntar
como evidencia) está en `http://localhost:<puerto>/v3/api-docs`.

**Captura para tu evidencia:** entra a cada Swagger UI, despliega los endpoints
y toma un pantallazo. Eso cumple con el requisito de "Archivo Swagger... con la
especificación de los endpoints".

### Postman Collection (alternativa/complemento)

También se incluyó una Postman Collection completa en `postman/`:

- `Innovatech-Backend.postman_collection.json`: los 23 endpoints de los 5
  microservicios, agrupados por carpeta.
- `Innovatech-Local.postman_environment.json`: variables de entorno con las
  URLs locales de cada servicio.

Para usarla: Postman → Import → selecciona ambos archivos → elige el
environment "Innovatech - Local" → ejecuta primero **ms-auth / Login** (guarda
el token automáticamente en la variable `{{token}}` vía un script de test) →
ya puedes ejecutar el resto de las requests autenticadas.

---

## 2. SonarQube

### Qué se agregó

- **Servicio SonarQube local** en `docker-compose.yml` (imagen
  `sonarqube:10.6-community`), con su propia base de datos PostgreSQL dedicada
  (`sonarqube-db`) para no interferir con la base de datos de los
  microservicios.
- Se completaron las propiedades de Sonar que estaban con placeholder
  `TU_ORG` (`ms-proyectos`, `ms-recursos`) o que faltaban por completo
  (`ms-auth`), dejando todos los microservicios con la misma convención:
  `sonar.organization=innovatech-org` y
  `sonar.projectKey=innovatech-org_<nombre-del-ms>`.
- Se agregó el plugin de JaCoCo + Sonar Scanner a `ms-auth` (los otros 4 ya
  lo tenían).
- Se actualizó el workflow de CI (`.github/workflows/backend-ci.yml`) para
  ejecutar `mvn verify` (en vez de solo `mvn test`), lo que genera el reporte
  `target/site/jacoco/jacoco.xml` y lo sube como artefacto descargable de cada
  ejecución. Se agregó un job opcional `sonarcloud` que solo se ejecuta si
  configuras el secret `SONAR_TOKEN` en GitHub.

### Opción A — Levantar SonarQube en local (recomendado para la entrega)

1. Verifica los requisitos de memoria virtual de SonarQube (solo Linux/WSL):
   ```bash
   sudo sysctl -w vm.max_map_count=262144
   ```
2. Levanta el stack completo:
   ```bash
   docker compose up -d sonarqube-db sonarqube
   ```
3. Espera ~1-2 minutos a que arranque (la primera vez tarda más) y entra a
   **http://localhost:9000**.
4. Usuario/clave por defecto: `admin` / `admin` (te pedirá cambiarla al
   primer ingreso).
5. Crea un proyecto manual por cada microservicio (o usa "Analyze new
   project" desde el dashboard) y genera un **token** en
   `My Account > Security > Generate Tokens`.
6. Para cada microservicio, ejecuta desde su carpeta:
   ```bash
   cd ms-auth
   ./mvnw verify sonar:sonar \
     -Dsonar.host.url=http://localhost:9000 \
     -Dsonar.login=<TU_TOKEN>
   ```
   Repite para `ms-proyectos`, `ms-recursos`, `ms-analitica`, `ms-notif`.
   El comando `verify` corre los tests, genera el `jacoco.xml` y luego el
   plugin de Sonar sube el análisis (código + cobertura) al servidor local.
7. Vuelve a http://localhost:9000 → entra a cada proyecto → ahí verás los
   **gráficos de cobertura, bugs, code smells, duplicación y el Quality
   Gate**. Esas pantallas son las que debes capturar como evidencia.

### Opción B — SonarCloud (ya estaba parcialmente configurado)

Si prefieres usar SonarCloud (gratis para repos públicos) en vez de levantar
SonarQube local:

1. Crea una cuenta/organización en https://sonarcloud.io e importa el repo.
2. Genera un token en SonarCloud y agrégalo como secret `SONAR_TOKEN` en
   GitHub (`Settings > Secrets and variables > Actions`).
3. Verifica que `sonar.organization` en cada `pom.xml` coincida con el slug
   real de tu organización en SonarCloud (por defecto dejamos
   `innovatech-org`; cámbialo si tu organización se llama distinto).
4. Haz push a `main`/`master`: el job `sonarcloud` del workflow se ejecutará
   automáticamente y subirá las métricas.
5. Las capturas de pantalla las tomas directamente del dashboard de
   SonarCloud (Overview de cada proyecto, con cobertura, duplicación,
   vulnerabilidades, etc.).

### Validar el 60% de cobertura mínima

Para revisar la cobertura sin depender de Sonar (por ejemplo, antes de subir
nada), JaCoCo genera un reporte HTML navegable en cada microservicio tras
correr los tests:

```bash
cd ms-auth
./mvnw verify
# abre target/site/jacoco/index.html en el navegador
```

Ese `index.html` muestra el porcentaje de cobertura por clase/paquete y es,
junto con el dashboard de SonarQube/SonarCloud, una evidencia válida para el
pantallazo de "cobertura mínima del 60%".

> **Nota:** si algún microservicio queda por debajo del 60%, lo más rápido es
> añadir tests a las clases de `service/` (la lógica de negocio), que suelen
> aportar más cobertura por test que los controllers.

---

## Resumen de archivos nuevos/modificados

```
docker-compose.yml                         (+ servicio sonarqube y sonarqube-db)
.github/workflows/backend-ci.yml           (mvn verify + upload artifact + job sonarcloud)
postman/Innovatech-Backend.postman_collection.json   (nuevo)
postman/Innovatech-Local.postman_environment.json    (nuevo)

ms-auth/pom.xml                  (+ springdoc, + jacoco, + sonar, + properties sonar)
ms-auth/.../config/OpenApiConfig.java        (nuevo)
ms-auth/.../config/SecurityConfig.java       (permitAll para swagger-ui y v3/api-docs)
ms-auth/.../controller/AuthController.java   (+ anotaciones @Tag/@Operation)
ms-auth/.../application.properties          (+ rutas springdoc)

ms-proyectos/pom.xml             (+ springdoc, sonar.organization/projectKey reales)
ms-proyectos/.../config/OpenApiConfig.java         (nuevo)
ms-proyectos/.../controller/ProjectController.java (+ anotaciones)
ms-proyectos/.../application.properties            (+ rutas springdoc)

ms-recursos/pom.xml              (+ springdoc, sonar.organization/projectKey reales)
ms-recursos/.../config/OpenApiConfig.java          (nuevo)
ms-recursos/.../controller/ResourceController.java (+ anotaciones)
ms-recursos/.../application.properties             (+ rutas springdoc)

ms-analitica/pom.xml             (+ springdoc)
ms-analitica/.../config/OpenApiConfig.java          (nuevo)
ms-analitica/.../controller/AnaliticaController.java (+ anotaciones)
ms-analitica/.../application.yml                    (+ rutas springdoc)

ms-notif/pom.xml                 (+ springdoc)
ms-notif/.../config/OpenApiConfig.java              (nuevo)
ms-notif/.../controller/NotificationController.java (+ anotaciones)
ms-notif/.../application.yml                        (+ rutas springdoc)
```
