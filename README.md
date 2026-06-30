# ⚙️ Innovatech Solutions - [NOMBRE-DEL-MICROSERVICIO]
> **Arquitectura Orientada a Microservicios**
> Microservicio backend desarrollado en Java y Spring Boot.
---
## 📑 Tabla de Contenidos
1. [Descripción General](#descripción-general)
2. [Requisitos Previos](#requisitos-previos)
3. [Ejecución y Despliegue con
Docker](#ejecución-y-despliegue-con-docker)
4. [GUÍA DE EJECUCIÓN DE PRUEBAS Y
REPORTES](#guía-de-ejecución-de-pruebas-y-reportes)
---
## 📌 Descripción General
Arquitectura del microservicio basada en **Spring Boot**,
exponiendo una API REST y conectada a recursos de persistencia,
todo completamente dockerizado para garantizar la consistencia
entre entornos. Este README es aplicable genéricamente a servicios
del ecosistema como `ms-auth` o `ms-proyectos`.
---
## ✅ Requisitos Previos
* **Docker** y **Docker Compose** (Obligatorio para levantar el

ecosistema).
* **JDK 17+** y **Apache Maven** (Opcional, exclusivamente para
desarrollo local y pruebas aisladas).
---
## 🐳 Ejecución y Despliegue con Docker (Sección Principal)
Para levantar el microservicio o todo el ecosistema de backend,
solo hace falta abrir la terminal en la raíz del proyecto, iniciar
Docker y ejecutar:
```bash
docker-compose up --build
```
Para detener los contenedores y limpiar los recursos de red,
utilice:
```bash
docker-compose down
```

---
## 🧪 GUÍA DE EJECUCIÓN DE PRUEBAS Y REPORTES (Sección Crítica)
Como los contenedores sirven para correr la app en tiempo de
ejecución, el proceso de testing debe aislarse y correr de forma
local usando Maven.
### 1. Ejecución de Tests y Generación de Reporte JaCoCo
Ejecute el siguiente comando local con Maven para testear de forma
aislada y compilar el reporte:
```bash
mvn clean test jacoco:report
```
**Visualización:**
Maven guarda el reporte visual en la siguiente ruta local:
* `target/site/jacoco/index.html`
Para visualizarlo, simplemente ubique el archivo en el explorador
de su sistema y ábralo en su navegador web.
### 2. Integración y Análisis con SonarCloud

Para auditar la deuda técnica, detectar bugs y verificar el
porcentaje de cobertura (se exige un mínimo del **60%**), ejecute
el scanner de SonarCloud paso a paso utilizando la consola:
```bash
mvn sonar:sonar -Dsonar.token=TU_TOKEN_AQUI
```
*(Reemplace `TU_TOKEN_AQUI` por el token válido del proyecto).*
Revise el dashboard de SonarCloud para confirmar que el Quality
Gate sea superado exitosamente.