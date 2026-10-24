-- =====================================================================
-- Fictionary - Esquema inicial (ver capítulo "Modelo de dominio")
-- Reglas de integridad IN-01 a IN-09 (las garantizables en BD se definen aquí)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuarios                                                  (IN-01)
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(30)      NOT NULL,
    email         VARCHAR(254)     NOT NULL,
    password_hash VARCHAR(100)     NOT NULL,
    bio           VARCHAR(500),
    created_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    -- Derivado: suma de las contribuciones de sus recomendaciones
    critic_points DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_app_user_username ON app_user (lower(username));
CREATE UNIQUE INDEX ux_app_user_email    ON app_user (lower(email));

-- ---------------------------------------------------------------------
-- Catálogo: géneros y títulos (caché local de metadatos de TMDB)
-- ---------------------------------------------------------------------
CREATE TABLE genre (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tmdb_genre_id INTEGER     NOT NULL UNIQUE,
    name          VARCHAR(60) NOT NULL
);

CREATE TABLE title (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tmdb_id      BIGINT           NOT NULL,
    media_type   VARCHAR(10)      NOT NULL,
    name         VARCHAR(300)     NOT NULL,
    overview     TEXT,
    poster_path  VARCHAR(255),
    release_date DATE,
    popularity   DOUBLE PRECISION NOT NULL,
    vote_count   INTEGER          NOT NULL DEFAULT 0,
    fetched_at   TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT ck_title_media_type CHECK (media_type IN ('MOVIE', 'TV')),
    CONSTRAINT ck_title_popularity CHECK (popularity >= 0),
    CONSTRAINT ck_title_vote_count CHECK (vote_count >= 0),
    -- TMDB numera películas y series por separado
    CONSTRAINT uq_title_tmdb UNIQUE (tmdb_id, media_type)
);

CREATE TABLE title_genre (
    title_id BIGINT NOT NULL REFERENCES title (id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genre (id) ON DELETE CASCADE,
    PRIMARY KEY (title_id, genre_id)
);

-- ---------------------------------------------------------------------
-- Seguimiento entre usuarios                                (IN-02)
-- ---------------------------------------------------------------------
CREATE TABLE follow (
    follower_id BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    followed_id BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followed_id),
    CONSTRAINT ck_follow_not_self CHECK (follower_id <> followed_id)
);

CREATE INDEX ix_follow_followed ON follow (followed_id);

-- ---------------------------------------------------------------------
-- Recomendaciones                                  (IN-03, IN-08, IN-09)
-- ---------------------------------------------------------------------
CREATE TABLE recommendation (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    curator_id            BIGINT           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    title_id              BIGINT           NOT NULL REFERENCES title (id),
    body                  VARCHAR(2000)    NOT NULL,
    created_at            TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ      NOT NULL DEFAULT now(),
    -- Instantánea inmutable de la popularidad al publicar (RF-REC-04)
    popularity_snapshot   DOUBLE PRECISION NOT NULL,
    vote_count_snapshot   INTEGER          NOT NULL,
    popularity_imputed    BOOLEAN          NOT NULL DEFAULT FALSE,
    -- Agregados derivados que mantiene el motor de ranking (n_r, S_r, x_r)
    rating_count          INTEGER          NOT NULL DEFAULT 0,
    rating_sum            BIGINT           NOT NULL DEFAULT 0,
    contribution          DOUBLE PRECISION NOT NULL DEFAULT 0,
    CONSTRAINT uq_recommendation_curator_title UNIQUE (curator_id, title_id),
    CONSTRAINT ck_recommendation_body CHECK (length(btrim(body)) > 0),
    CONSTRAINT ck_recommendation_popularity CHECK (popularity_snapshot >= 0),
    CONSTRAINT ck_recommendation_votes CHECK (vote_count_snapshot >= 0),
    CONSTRAINT ck_recommendation_count CHECK (rating_count >= 0),
    -- Con puntuaciones de 1 a 10: n <= S <= 10n
    CONSTRAINT ck_recommendation_sum CHECK (rating_sum >= rating_count AND rating_sum <= 10 * rating_count)
);

CREATE INDEX ix_recommendation_curator_created ON recommendation (curator_id, created_at DESC);
CREATE INDEX ix_recommendation_title           ON recommendation (title_id);
CREATE INDEX ix_recommendation_created         ON recommendation (created_at DESC);

-- ---------------------------------------------------------------------
-- Valoraciones                                     (IN-04, IN-05, IN-06)
-- ---------------------------------------------------------------------
CREATE TABLE rating (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rater_id          BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    recommendation_id BIGINT      NOT NULL REFERENCES recommendation (id) ON DELETE CASCADE,
    score             INTEGER     NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_rating_score CHECK (score BETWEEN 1 AND 10),
    CONSTRAINT uq_rating_rater_recommendation UNIQUE (rater_id, recommendation_id)
);

CREATE INDEX ix_rating_recommendation ON rating (recommendation_id);

-- IN-06: un usuario no puede valorar sus propias recomendaciones
CREATE FUNCTION prevent_self_rating() RETURNS trigger AS $$
BEGIN
    IF EXISTS (SELECT 1
                 FROM recommendation r
                WHERE r.id = NEW.recommendation_id
                  AND r.curator_id = NEW.rater_id) THEN
        RAISE EXCEPTION 'Un usuario no puede valorar sus propias recomendaciones'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_rating_no_self
    BEFORE INSERT OR UPDATE ON rating
    FOR EACH ROW EXECUTE FUNCTION prevent_self_rating();
