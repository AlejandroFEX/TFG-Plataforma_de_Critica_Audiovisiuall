# 0003. Fórmula de Critic Points con ponderación acotada y confianza

* **Estado:** aceptada (los parámetros por defecto se revisarán con los experimentos)

## Contexto
El anteproyecto pide ponderar la puntuación de forma inversamente proporcional a la popularidad. Una función literal `1/π` no está acotada: un título con popularidad casi nula, o un dato erróneo, otorgaría puntos sin límite y sería trivial de explotar. Además, una media con pocas valoraciones no es fiable.

## Decisión
`x_r = w(π_r) · (S_r − n_r·s₀) / (n_r + k)` y `CP = Σ x_r`, con `w` no creciente y acotada en (0, 1] (variantes W0–W3), `s₀ = 5,5` y `k = 5`. La variante por defecto es W1: `w(π) = 1 / (1 + ln(1 + π))`. El cálculo vive en un servicio de dominio puro (paquete `ranking`) y se mantiene de forma incremental mediante los agregados `rating_count`, `rating_sum` y `contribution` de cada recomendación.

## Consecuencias
* (+) Contribución acotada por recomendación: `|x_r| < 4,5 · w`.
* (+) Coste constante por valoración (RNF-REN-03) y reutilización en el simulador.
* (−) Premiar lo poco popular puede aumentar el incentivo a manipular títulos oscuros; se mitiga con el factor de confianza y se evaluará con curadores manipuladores simulados.
* (−) Los Critic Points pueden ser negativos.
