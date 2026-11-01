-- Los usuarios generados por el módulo de simulación se marcan para excluirlos
-- del ranking y del buscador de usuarios.
ALTER TABLE app_user ADD COLUMN simulated BOOLEAN NOT NULL DEFAULT FALSE;

-- Marca los usuarios simulados creados antes de existir esta columna.
UPDATE app_user SET simulated = TRUE WHERE lower(email) LIKE '%@sim.test';

-- Los títulos simulados usaban ids 90000+, que son ids reales de TMDB:
-- se pasan a negativos para que nunca colisionen con un título real.
UPDATE title SET tmdb_id = -tmdb_id
 WHERE name LIKE 'Título simulado %' AND tmdb_id BETWEEN 90000 AND 90999;

-- El ranking siempre filtra por usuarios no simulados y ordena por puntos.
CREATE INDEX ix_app_user_real_points ON app_user (critic_points DESC) WHERE NOT simulated;
