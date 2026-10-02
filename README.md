# 🎬 Fictionary — Plataforma de Descubrimiento Audiovisual y Recomendación Social

> **Trabajo de Fin de Grado (TFG)** — Grao en Enxeñaría Informática (Mención en Enxeñaría do Software)  
> **Facultade de Informática de A Coruña (FIC) — Universidade da Coruña (UDC)**  

---

## 📌 Descripción del Proyecto

Las plataformas tradicionales de streaming sufren del fenómeno denominado **sesgo de popularidad** (*popularity bias*), incentivando la visibilidad de contenidos altamente populares y ocultando obras de gran calidad que pertenecen a la **long tail** (larga cola).

**Fictionary** es un **sistema web para el descubrimiento de contenido audiovisual poco popular basado en recomendaciones sociales**. A diferencia de las plataformas convencionales, Fictionary traslada el valor a la figura del **curador de contenidos**:

* Los usuarios publican recomendaciones de películas o series.
* Otros usuarios pueden seguir a estos curadores y puntuar la utilidad o calidad de sus recomendaciones.
* **Sistema de Reputación Ponderado:** La puntuación y autoridad recibida por un usuario es directamente proporcional a la satisfacción generada e **inversamente proporcional a la popularidad del contenido recomendado**, incentivando activamente el descubrimiento de obras poco comerciales.

---

## 🎯 Objetivos Concretos

1. **Identificar requisitos del producto** a partir del análisis del problema y de soluciones similares existentes.
2. **Integrar una o más APIs públicas de contenidos audiovisuales** para obtener metadatos (título, género, popularidad, póster, sinopsis, etc.), sin almacenar ni redistribuir el contenido audiovisual en sí.
3. **Permitir a cualquier usuario registrado publicar recomendaciones** sobre contenido audiovisual, seguir a otros usuarios y puntuar las recomendaciones publicadas por los usuarios que sigue.
4. **Diseñar e implementar un sistema de ranking** que fomente la valoración de contenidos poco populares, ponderando la puntuación recibida de forma inversamente proporcional a la popularidad del contenido recomendado.
5. **Diseñar y evaluar experimentalmente el sistema de ranking** mediante datos sintéticos, analizando su capacidad para promover contenido de baja popularidad y reducir el sesgo de popularidad.
6. **Implementar un feed** donde los usuarios puedan visualizar las recomendaciones de los usuarios a los que siguen, ordenadas según criterios configurables.

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
| :--- | :--- |
| **Backend** | Java 21 / Spring Boot 3 (Spring Data JPA, Spring Security) |
| **Frontend** | React + TypeScript (Vite, Axios, React Router) |
| **Base de Datos** | PostgreSQL 16 |
| **Gestor de Construcción** | Apache Maven |
| **Contenerización** | Docker / Docker Compose |
| **API Externa** | TMDB (The Movie Database) API |

---

## 🚀 Cómo Ejecutar el Proyecto en Local

### Requisitos Previos
* Java 21+ instalado.
* Node.js (v18+) y npm.
* Docker Desktop instalado y en ejecución.
* Maven.

### 1. Clonar el Repositorio
```bash
git clone [https://github.com/TU_USUARIO/TFG-Plataforma_de_Critica_Audiovisiuall.git](https://github.com/TU_USUARIO/TFG-Plataforma_de_Critica_Audiovisiuall.git)
cd TFG-Plataforma_de_Critica_Audiovisiuall