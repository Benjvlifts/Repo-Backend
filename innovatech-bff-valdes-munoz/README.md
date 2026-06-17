# 🔀 innovatech-bff-valdes-munoz

> **Backend For Frontend (BFF)** de la plataforma Innovatech Solutions — Evaluación Parcial 2  
> Asignatura: DSY1106 – Desarrollo Fullstack III | Instituto DuocUC | 2026  
> Estudiantes: **Benjamín Valdés** · **Ignacio Muñoz**

---

## 📑 Tabla de Contenidos

1. [Descripción del Proyecto](#descripción-del-proyecto)
2. [Patrón BFF – Justificación](#patrón-bff--justificación)
3. [Tecnologías Utilizadas](#tecnologías-utilizadas)
4. [Estructura del Proyecto](#estructura-del-proyecto)
5. [Requisitos Previos](#requisitos-previos)
6. [Instalación Paso a Paso](#instalación-paso-a-paso)
7. [Variables de Entorno](#variables-de-entorno)
8. [Ejecución del BFF](#ejecución-del-bff)
9. [Endpoints Expuestos](#endpoints-expuestos)
10. [Circuit Breaker – Implementación](#circuit-breaker--implementación)
11. [Flujo de Peticiones](#flujo-de-peticiones)
12. [Pruebas con Postman / curl](#pruebas-con-postman--curl)
13. [Resolución de Problemas Comunes](#resolución-de-problemas-comunes)

---

## 📌 Descripción del Proyecto

El **Backend For Frontend (BFF)** es el componente intermediario entre el frontend React y los microservicios internos de la plataforma Innovatech Solutions. Actúa como un punto de agregación y adaptación: recibe las peticiones del frontend, las enruta al microservicio correspondiente a través del Kong API Gateway, y devuelve las respuestas en el formato que el frontend necesita.

### Arquitectura en Contexto

```
[Frontend React - :5173]
        ↓  POST /api/auth/login
[BFF Node.js/Express - :3001]   ← Este componente
        ↓  POST http://localhost:8000/auth/login
[Kong API Gateway - :8000]
        ↓
[ms-auth - :8081]   [ms-proyectos - :8082]   [ms-recursos - :8083]
```

---

## 🏗️ Patrón BFF – Justificación

El patrón BFF (Backend For Frontend) resuelve los siguientes problemas de arquitectura:

1. **Agregación de datos**: El frontend necesita datos de múltiples microservicios en una sola llamada. El BFF los agrega y los devuelve en un solo response.
2. **Adaptación de formato**: Los microservicios pueden retornar datos en formatos diferentes. El BFF los transforma al formato esperado por el frontend.
3. **Seguridad**: El BFF actúa como barrera de seguridad, validando el JWT antes de reenviar la petición. El frontend nunca tiene acceso directo a los microservicios.
4. **Circuit Breaker**: Implementa el patrón Circuit Breaker para proteger al frontend de fallos en los microservicios.
5. **CORS**: Gestiona las políticas CORS de forma centralizada, sin que cada microservicio necesite configurarlas individualmente.

---

## 🛠️ Tecnologías Utilizadas

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Node.js | ≥ 18 | Entorno de ejecución |
| Express | 4.x | Framework HTTP |
| Axios | 1.x | Cliente HTTP para llamadas a microservicios |
| CORS | 2.x | Middleware de políticas CORS |
| dotenv | 16.x | Gestión de variables de entorno |
| NPM | ≥ 9 | Gestor de paquetes |

---

## 📁 Estructura del Proyecto

```
innovatech-bff-valdes-munoz/
├── src/
│   ├── middleware/
│   │   └── errorHandler.js      # Middleware global de manejo de errores
│   ├── routes/
│   │   ├── authRoutes.js        # Rutas de autenticación (/api/auth/*)
│   │   ├── projectRoutes.js     # Rutas de proyectos (/api/projects/*)
│   │   └── resourceRoutes.js    # Rutas de recursos (/api/resources/*)
│   ├── services/
│   │   ├── api.js               # Configuración central de Axios
│   │   ├── CircuitBreaker.js    # Implementación del patrón Circuit Breaker
│   │   └── httpClient.js        # Cliente HTTP con Circuit Breaker integrado
│   └── index.js                 # Punto de entrada del servidor Express
├── .env                         # Variables de entorno (no commitear)
├── .env.example                 # Template de variables de entorno
└── package.json                 # Dependencias y scripts NPM
```

---

## ✅ Requisitos Previos

1. **Node.js ≥ 18** — [Descargar en nodejs.org](https://nodejs.org)
2. **NPM ≥ 9** — Se instala automáticamente con Node.js
3. **Kong API Gateway corriendo** — En `http://localhost:8000`
4. **ms-auth corriendo** — En `http://localhost:8081`
5. **ms-proyectos corriendo** — En `http://localhost:8082`

Verificar instalaciones:
```bash
node --version    # Debe mostrar v18.x o superior
npm --version     # Debe mostrar 9.x o superior
```

---

## 📥 Instalación Paso a Paso

### Paso 1: Clonar el repositorio

```bash
git clone https://github.com/TU_USUARIO/innovatech-bff-valdes-munoz.git
cd innovatech-bff-valdes-munoz
```

### Paso 2: Instalar dependencias

```bash
npm install
```

**Salida esperada:**
```
added 87 packages, and audited 88 packages in 12s
```

### Paso 3: Configurar variables de entorno

```bash
cp .env.example .env
# Editar .env con los valores correctos (ver sección Variables de Entorno)
```

---

## 🔧 Variables de Entorno

Crear archivo `.env` en la raíz del proyecto con el siguiente contenido:

```env
# Puerto en que escucha el BFF
PORT=3001

# URL base del Kong API Gateway
GATEWAY_URL=http://localhost:8000

# URL directa de ms-auth (para fallback si Kong no está disponible)
AUTH_SERVICE_URL=http://localhost:8081

# URL directa de ms-proyectos
PROJECTS_SERVICE_URL=http://localhost:8082

# URL directa de ms-recursos
RESOURCES_SERVICE_URL=http://localhost:8083

# Configuración del Circuit Breaker
CB_FAILURE_THRESHOLD=5        # Número de fallos antes de abrir el circuito
CB_RECOVERY_TIMEOUT=60000     # Tiempo en ms antes de intentar recuperación (60 seg)
CB_SUCCESS_THRESHOLD=2        # Número de éxitos en half-open para cerrar el circuito
```

> ⚠️ **Nunca commitear el archivo `.env`** al repositorio. Está incluido en `.gitignore`.

---

## ▶️ Ejecución del BFF

### Modo Desarrollo (con hot-reload)

```bash
npm run dev
```

**Salida esperada:**
```
[BFF] Innovatech BFF corriendo en http://localhost:3001
[BFF] GATEWAY_URL: http://localhost:8000
[BFF] Circuit Breaker inicializado para: auth, projects, resources
```

### Modo Producción

```bash
npm start
```

---

## 📡 Endpoints Expuestos

El BFF expone los siguientes endpoints al frontend:

### Autenticación (`/api/auth`)

| Método | Endpoint | Body | Headers | Descripción |
|--------|----------|------|---------|-------------|
| POST | `/api/auth/register` | `{username, email, password, rol}` | — | Registra un nuevo usuario |
| POST | `/api/auth/login` | `{username, password}` | — | Autentica usuario, retorna JWT |
| GET | `/api/auth/profile` | — | `Authorization: Bearer <JWT>` | Obtiene perfil del usuario autenticado |

**Ejemplo de body para registro:**
```json
{
  "username": "jdoe",
  "email": "jdoe@empresa.com",
  "password": "SecurePass123!",
  "rol": "DEVELOPER"
}
```

**Ejemplo de respuesta de login:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5...",
  "user": {
    "id": 1,
    "username": "jdoe",
    "email": "jdoe@empresa.com",
    "rol": "DEVELOPER"
  }
}
```

### Proyectos (`/api/projects`)

| Método | Endpoint | Body | Headers | Descripción |
|--------|----------|------|---------|-------------|
| GET | `/api/projects` | — | `Authorization: Bearer <JWT>` | Lista todos los proyectos |
| GET | `/api/projects/:id` | — | `Authorization: Bearer <JWT>` | Obtiene un proyecto por ID |
| POST | `/api/projects` | `{name, type, description, ...}` | `Authorization: Bearer <JWT>` | Crea un nuevo proyecto |
| PUT | `/api/projects/:id` | `{name, status, ...}` | `Authorization: Bearer <JWT>` | Actualiza un proyecto |
| DELETE | `/api/projects/:id` | — | `Authorization: Bearer <JWT>` | Elimina un proyecto |

### Recursos (`/api/resources`)

| Método | Endpoint | Headers | Descripción |
|--------|----------|---------|-------------|
| GET | `/api/resources` | `Authorization: Bearer <JWT>` | Lista todos los recursos |
| GET | `/api/resources/:id` | `Authorization: Bearer <JWT>` | Obtiene un recurso por ID |

---

## ⚡ Circuit Breaker – Implementación

El BFF implementa el patrón **Circuit Breaker** en `src/services/CircuitBreaker.js` para proteger el sistema ante fallos en los microservicios.

### Estados del Circuit Breaker

```
CLOSED ──(umbral de fallos superado)──► OPEN
  ▲                                        │
  │                              (timeout recovery)
  │                                        ▼
  └──(éxitos en half-open)────── HALF_OPEN
```

| Estado | Comportamiento |
|--------|---------------|
| `CLOSED` | Operación normal. Las peticiones fluyen al microservicio. |
| `OPEN` | El circuito está abierto. Las peticiones retornan error 503 inmediatamente sin intentar conectar. |
| `HALF_OPEN` | Periodo de prueba. Se permite una petición de ensayo. Si tiene éxito, se cierra el circuito. Si falla, se vuelve a abrir. |

### Configuración por Microservicio

| Microservicio | Umbral de Fallos | Timeout Recovery | Respuesta de Fallback |
|--------------|-----------------|------------------|-----------------------|
| ms-auth | 5 fallos consecutivos | 60 segundos | `{ error: "Servicio de autenticación no disponible" }` |
| ms-proyectos | 5 fallos consecutivos | 60 segundos | `{ projects: [], message: "Servicio de proyectos temporalmente no disponible" }` |
| ms-recursos | 5 fallos consecutivos | 60 segundos | `{ resources: [], message: "Servicio de recursos temporalmente no disponible" }` |

---

## 🔄 Flujo de Peticiones

### Login Exitoso

```
1. Frontend → POST /api/auth/login { username, password }
2. BFF authRoutes.js → httpClient.post('/auth/login', body)
3. CircuitBreaker: estado CLOSED → permite la petición
4. httpClient → POST http://localhost:8000/auth/login (via Kong)
5. Kong → ms-auth:8081 /auth/login
6. ms-auth: valida credenciales con BCrypt → genera JWT → retorna { token, user }
7. BFF → retorna { token, user } al frontend (HTTP 200)
8. Frontend: AuthContext.login({ token, user }) → almacena JWT
```

### Login con Circuit Breaker OPEN

```
1. Frontend → POST /api/auth/login { username, password }
2. BFF authRoutes.js → httpClient.post('/auth/login', body)
3. CircuitBreaker: estado OPEN → NO envía petición al microservicio
4. BFF → retorna { error: "Servicio de autenticación no disponible" } (HTTP 503)
5. Frontend: muestra mensaje de error al usuario
```

---

## 🧪 Pruebas con Postman / curl

### Registrar usuario

```bash
curl -X POST http://localhost:3001/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@innovatech.com",
    "password": "Test1234!",
    "rol": "DEVELOPER"
  }'
```

### Iniciar sesión

```bash
curl -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test1234!"
  }'
```

**Respuesta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "testuser",
    "email": "test@innovatech.com",
    "rol": "DEVELOPER"
  }
}
```

### Obtener proyectos (con JWT)

```bash
# Reemplazar <TOKEN> con el JWT obtenido en el login
curl -X GET http://localhost:3001/api/projects \
  -H "Authorization: Bearer <TOKEN>"
```

### Crear proyecto

```bash
curl -X POST http://localhost:3001/api/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "Portal Cliente v2",
    "type": "SOFTWARE",
    "description": "Rediseño completo del portal de clientes",
    "status": "PLANNING"
  }'
```

---

## 🩺 Resolución de Problemas Comunes

### ❌ Error: "ECONNREFUSED 127.0.0.1:8000"

**Causa:** Kong API Gateway no está corriendo.  
**Solución:** Iniciar los servicios con Docker Compose:
```bash
cd <directorio-raíz-del-proyecto>
docker-compose up -d kong zookeeper kafka
```

### ❌ Error: "Cannot find module 'express'"

**Causa:** Dependencias no instaladas.  
**Solución:**
```bash
npm install
```

### ❌ Error: "PORT 3001 already in use"

**Causa:** Hay otro proceso usando el puerto 3001.  
**Solución:**
```bash
# En Linux/Mac
lsof -i :3001 | grep LISTEN
kill -9 <PID>

# En Windows
netstat -ano | findstr :3001
taskkill /PID <PID> /F
```

### ❌ HTTP 401 en todas las peticiones autenticadas

**Causa:** El JWT enviado es inválido o el secret del JWT no coincide entre BFF y ms-auth.  
**Solución:** Verificar que la variable `JWT_SECRET` en ms-auth coincida con la que usa el BFF para reenviar el header `Authorization`.

### ❌ Circuit Breaker siempre en estado OPEN

**Causa:** El microservicio destino nunca responde correctamente.  
**Solución:** 
1. Verificar que ms-auth y ms-proyectos estén corriendo.
2. Verificar que Kong esté configurado correctamente (`kong.yml`).
3. Reiniciar el BFF para resetear el estado del Circuit Breaker.

---

## 👥 Contribuidores

| Estudiante | GitHub | Rol Principal |
|-----------|--------|---------------|
| Benjamín Valdés | @benjaminvaldes | BFF + Circuit Breaker + ms-auth |
| Ignacio Muñoz | @ignacionunoz | Frontend + ms-proyectos + Kafka |

---

**Instituto DuocUC — 2026 — DSY1106 Desarrollo Fullstack III —*
