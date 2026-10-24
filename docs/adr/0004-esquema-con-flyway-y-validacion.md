# 0004. Esquema gestionado por Flyway y validado por Hibernate

* **Estado:** aceptada

## Contexto
Las reglas de integridad del dominio (unicidad, rangos, no seguirse a uno mismo, no valorar lo propio) deben garantizarse también en la base de datos (RNF-FIA-02), y el esquema debe evolucionar de forma versionada (RNF-FIA-03).

## Decisión
El esquema se define en migraciones Flyway (`db/migration`). Hibernate trabaja con `ddl-auto=validate`: valida que las entidades coincidan con el esquema, pero nunca lo modifica. La regla «no valorar las propias recomendaciones» se implementa con un *trigger* de PostgreSQL además de la comprobación en el dominio. Los escalares de puntuación usan `double precision`.

## Consecuencias
* (+) Las reglas se cumplen aunque se escriba en la base de datos por otra vía.
* (+) Una discrepancia entre entidad y migración impide arrancar, lo que se detecta pronto.
* (−) Cada cambio de modelo exige una migración nueva (nunca se editan las ya aplicadas).
