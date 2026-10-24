# 0002. Instantánea de popularidad en cada recomendación

* **Estado:** aceptada

## Contexto
La reputación de un curador depende de la popularidad del título que recomienda. Según la documentación de TMDB, su popularidad se ve influida por métricas diarias y puede variar con el tiempo. Si el cálculo usara la popularidad actual, los Critic Points cambiarían sin que nadie valorase nada y los experimentos no serían reproducibles.

## Decisión
Cada recomendación guarda, de forma inmutable, la popularidad y el número de votos del título en el momento de publicarse (`popularity_snapshot`, `vote_count_snapshot`). Si el dato no está disponible se imputa (mediana del catálogo) y se marca con `popularity_imputed`.

## Consecuencias
* (+) El cálculo es determinista y reproducible (RNF-REP-01).
* (+) Quien recomienda un título cuando era poco conocido conserva el mérito aunque luego se popularice.
* (+) Guardar también el número de votos permite comparar medidas de popularidad en los experimentos.
* (−) Se duplican datos que existen en `title`.
