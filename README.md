# 🎬 Fictionary — Plataforma de Descubrimiento Audiovisual y Recomendación Social

> **Trabajo de Fin de Grado (TFG)** — Grao en Enxeñaría Informática (Mención en Enxeñaría do Software)  
> **Facultade de Informática de A Coruña (FIC) — Universidade da Coruña (UDC)**  

---

## Descripción del Proyecto

Las plataformas tradicionales de streaming sufren del fenómeno denominado **sesgo de popularidad** (*popularity bias*), incentivando la visibilidad de contenidos altamente populares y ocultando obras de gran calidad que pertenecen a la **long tail** (larga cola).

**Fictionary** es un **sistema web para el descubrimiento de contenido audiovisual poco popular basado en recomendaciones sociales**. A diferencia de las plataformas convencionales, Fictionary traslada el valor a la figura del **curador de contenidos**:

* Los usuarios publican recomendaciones de películas o series poco conocidas.
* Otros usuarios pueden seguir a estos curadores y puntuar la utilidad o calidad de sus recomendaciones.
* **Sistema de Reputación Ponderado:** La puntuación y autoridad recibida por un usuario es directamente proporcional a la satisfacción generada e **inversamente proporcional a la popularidad del contenido recomendado**, incentivando activamente el descubrimiento de obras poco comerciales.

---

## Objetivos Principales

1. **Integración con APIs externas:** Obtención de metadatos audiovisuales (pósters, sinopsis, géneros, popularidad base) en tiempo real sin almacenar contenidos con derechos de autor.
2. **Módulo Social:** Gestión de usuarios, seguidores y feeds interactivos de recomendaciones.
3. **Motor de Ranking e Incentivo:** Algoritmo de puntuación que favorece la recomendación de obras de baja visibilidad.
4. **Evaluación Experimental:** Simulación y validación del algoritmo mediante datos sintéticos para demostrar la reducción del sesgo de popularidad.

---

## Stack Tecnológico

| Capa | Tecnología |
| :--- | :--- |
| **Backend** | Java 21 / Spring Boot 3 (Spring Data JPA, Spring Security) |
| **Frontend** | React + TypeScript (Vite, Axios, React Router) |
| **Base de Datos** | PostgreSQL 16 |
| **Gestor de Construcción** | Apache Maven |
| **Contenerización** | Docker / Docker Compose |
| **API Externa** | TMDB (The Movie Database) API |

---

## Cómo Ejecutar el Proyecto en Local

### Requisitos Previos
* Java 21+ instalado.
* Node.js (v18+) y npm.
* Docker Desktop instalado y en ejecución.
* Maven.

### 1. Clonar el Repositorio
```bash
git clone [https://github.com/TU_USUARIO/TFG-Plataforma_de_Critica_Audiovisiuall.git](https://github.com/TU_USUARIO/TFG-Plataforma_de_Critica_Audiovisiuall.git)
cd TFG-Plataforma_de_Critica_Audiovisiuall