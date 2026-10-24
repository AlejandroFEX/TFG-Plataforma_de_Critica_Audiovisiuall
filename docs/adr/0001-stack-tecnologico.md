# 0001. Stack tecnológico del backend

* **Estado:** aceptada

## Contexto
El proyecto necesita una API web, persistencia relacional (el anteproyecto exige un SGBD relacional) y pruebas fiables. La mención del grado es Ingeniería del Software, por lo que se valoran la arquitectura en capas, las pruebas y la trazabilidad.

## Decisión
Java 21, Spring Boot 4.1 con Spring Data JPA, PostgreSQL 16, Flyway para migraciones y Testcontainers para pruebas de integración. Maven como herramienta de construcción. Frontend en React con TypeScript (aún no iniciado). Spring Security con tokens JWT se añadirá en el módulo de autenticación.

## Consecuencias
* (+) Ecosistema maduro, buena documentación y encaje con la mención.
* (+) Pruebas de integración contra un PostgreSQL real, no contra una base de datos sustituta.
* (−) Spring Boot 4 modulariza los *starters* (p. ej. Flyway requiere `spring-boot-starter-flyway`), por lo que parte de la documentación antigua no aplica.
* (−) Las pruebas de integración requieren Docker.
