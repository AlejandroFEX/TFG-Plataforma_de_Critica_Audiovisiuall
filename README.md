# 🎬 Fictionary — Plataforma de Descubrimiento Audiovisual y Recomendación Social

> **Trabajo de Fin de Grado (TFG)** — Grao en Enxeñaría Informática (Mención en Enxeñaría do Software)
> **Facultade de Informática de A Coruña (FIC) — Universidade da Coruña (UDC)**

---

## 📌 Descripción del proyecto

Las plataformas tradicionales de streaming sufren el fenómeno denominado **sesgo de popularidad** (*popularity bias*), que favorece la visibilidad de contenidos muy populares y oculta obras de calidad pertenecientes a la **long tail** (larga cola).

**Fictionary** es un **sistema web para el descubrimiento de contenido audiovisual poco popular basado en recomendaciones sociales**. A diferencia de las plataformas convencionales, traslada el valor a la figura del **curador**:

* Los usuarios publican recomendaciones de películas o series.
* Otros usuarios pueden seguir a los curadores y puntuar la **utilidad de sus recomendaciones** (no la obra).
* **Critic Points:** la reputación de un curador depende de la satisfacción que generan sus recomendaciones y se pondera de forma **decreciente respecto de la popularidad del contenido recomendado**, de modo que descubrir obras poco conocidas aporta más reputación.

> La efectividad de este mecanismo es una **hipótesis de diseño** que se evalúa experimentalmente con datos sintéticos; no es una conclusión demostrada.

---

## 🎯 Objetivos concretos

1. Identificar requisitos del producto a partir del análisis del problema y de soluciones similares.
2. Integrar APIs públicas de contenido audiovisual (TMDB) para obtener **metadatos**, sin almacenar ni redistribuir el contenido.
3. Permitir a cualquier usuario registrado publicar recomendaciones, seguir a otros usuarios y puntuar las recomendaciones de los usuarios que sigue.
4. Diseñar e implementar un sistema de ranking que fomente la valoración de contenidos poco populares.
5. Diseñar y evaluar experimentalmente el ranking mediante datos sintéticos.
6. Implementar un feed con las recomendaciones de los usuarios seguidos, ordenadas según criterios configurables.

---

## 🚧 Estado actual

| Área | Estado |
| :--- | :--- |
| Esquema de base de datos (Flyway) y entidades JPA | ✅ Hecho |
| Motor de ranking (cálculo de la contribución, variantes W0–W3) | ✅ Hecho, con pruebas unitarias |
| Infraestructura de pruebas (Testcontainers + PostgreSQL) | ✅ Hecho |
| Autenticación y perfil (Spring Security + JWT) | ⏳ Pendiente |
| Integración con TMDB (búsqueda, fichas, caché) | ⏳ Pendiente |
| API REST: seguimiento, recomendaciones, valoraciones | ⏳ Pendiente |
| Recálculo incremental de Critic Points en el servicio (CU-15) | ⏳ Pendiente |
| Feed con orden y filtros configurables | ⏳ Pendiente |
| Simulador de datos sintéticos y experimentos | ⏳ Pendiente |
| Frontend (React + TypeScript) | ⏳ Pendiente |

---

## 🛠️ Stack tecnológico

| Capa | Tecnología |
| :--- | :--- |
| **Backend** | Java 21 · Spring Boot 4.1 · Spring Data JPA · Flyway |
| **Base de datos** | PostgreSQL 16 |
| **Pruebas** | JUnit 5 · Testcontainers (PostgreSQL real y efímero) |
| **Construcción** | Apache Maven (wrapper incluido) |
| **Contenedores** | Docker Compose (base de datos de desarrollo) |
| **API externa** | TMDB (The Movie Database) |
| **Frontend** *(planificado)* | React + TypeScript |
| **Seguridad** *(planificado)* | Spring Security + JWT |

---

## 📁 Estructura del repositorio

```
.
├── docker-compose.yml          # PostgreSQL local
├── .env.example                # Plantilla de variables (copiar a .env)
├── docs/adr/                   # Registros de decisiones de arquitectura
└── fictionary/                 # Backend Spring Boot
    └── src/main
        ├── java/es/udc/fic/fictionary
        │   ├── user/           # Usuarios
        │   ├── catalog/        # Títulos y géneros (caché de TMDB)
        │   ├── social/         # Seguimiento entre usuarios
        │   ├── recommendation/ # Recomendaciones y valoraciones
        │   └── ranking/        # Motor de Critic Points (dominio puro)
        └── resources/db/migration/   # Migraciones Flyway
```

---

## 🚀 Cómo ejecutar el proyecto en local

### Requisitos previos
* Java 21+
* Docker (en ejecución)
* No hace falta instalar Maven: se usa el wrapper (`mvnw`).

### 1. Clonar y configurar

```bash
git clone https://github.com/AlejandroFEX/TFG-Plataforma_de_Critica_Audiovisiuall.git
cd TFG-Plataforma_de_Critica_Audiovisiuall
cp .env.example .env        # y edita las contraseñas y la clave de TMDB
```

### 2. Levantar la base de datos

```bash
docker compose up -d
```

### 3. Arrancar el backend

```bash
cd fictionary
./mvnw spring-boot:run      # En Windows: mvnw.cmd spring-boot:run
```

Flyway crea el esquema al arrancar. Hibernate solo lo **valida** (`ddl-auto=validate`): si una entidad no coincide con la migración, la aplicación no arranca.

### 4. Ejecutar las pruebas

```bash
cd fictionary
./mvnw test
```

Las pruebas de integración usan **Testcontainers**, por lo que necesitan Docker en ejecución. Las pruebas del motor de ranking no lo necesitan.

---

## ⚙️ Configuración

| Variable | Descripción | Valor por defecto |
| :--- | :--- | :--- |
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/fictionary` |
| `DB_USER` / `DB_PASSWORD` | Credenciales de la base de datos | `fictionary` / `fictionary` |
| `TMDB_API_KEY` | Clave de la API de TMDB | *(vacía)* |

Los parámetros del ranking se configuran en `application.properties` (`fictionary.ranking.*`). **Nunca subas el archivo `.env` ni claves al repositorio.**

---

## 🧮 Modelo de Critic Points (resumen)

La contribución de una recomendación `r` a la reputación de su curador es

```
x_r = w(π_r) · (S_r − n_r · s₀) / (n_r + k)        CP_curador = Σ x_r
```

donde `π_r` es la popularidad del título **en el momento de recomendar** (instantánea inmutable), `n_r` y `S_r` son el número y la suma de valoraciones, `s₀ = 5,5` es la puntuación neutra, `k = 5` es el peso del conocimiento previo y `w` es una función decreciente de la popularidad (variantes W0–W3). Los detalles, el análisis de casos límite y el plan de evaluación están en la memoria del TFG.

---

## 📚 Documentación

Las decisiones de diseño relevantes se registran en [`docs/adr`](docs/adr).

## 📄 Licencia

Distribuido bajo licencia **GPL-3.0** (ver [`LICENSE`](LICENSE)).
