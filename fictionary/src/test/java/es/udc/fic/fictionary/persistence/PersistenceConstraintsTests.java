package es.udc.fic.fictionary.persistence;

import es.udc.fic.fictionary.TestcontainersConfiguration;
import es.udc.fic.fictionary.catalog.Genre;
import es.udc.fic.fictionary.catalog.GenreRepository;
import es.udc.fic.fictionary.catalog.MediaType;
import es.udc.fic.fictionary.catalog.Title;
import es.udc.fic.fictionary.catalog.TitleRepository;
import es.udc.fic.fictionary.recommendation.Rating;
import es.udc.fic.fictionary.recommendation.RatingRepository;
import es.udc.fic.fictionary.recommendation.Recommendation;
import es.udc.fic.fictionary.recommendation.RecommendationRepository;
import es.udc.fic.fictionary.social.Follow;
import es.udc.fic.fictionary.social.FollowRepository;
import es.udc.fic.fictionary.user.User;
import es.udc.fic.fictionary.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba contra un PostgreSQL real que el esquema aplica las reglas de integridad
 * IN-01 a IN-09 del modelo de dominio. Cada prueba se ejecuta en una transacción que se revierte.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PersistenceConstraintsTests {

    @Autowired UserRepository users;
    @Autowired TitleRepository titles;
    @Autowired GenreRepository genres;
    @Autowired RecommendationRepository recommendations;
    @Autowired RatingRepository ratings;
    @Autowired FollowRepository follows;
    @Autowired JdbcTemplate jdbc;
    @PersistenceContext EntityManager em;

    private User newUser(String name) {
        return users.saveAndFlush(new User(name, name + "@example.com", "hash"));
    }

    private Title newTitle(long tmdbId, double popularity) {
        return titles.saveAndFlush(new Title(tmdbId, MediaType.MOVIE, "Titulo " + tmdbId, popularity, 10));
    }

    private Recommendation newRecommendation(User curator, Title title) {
        return recommendations.saveAndFlush(new Recommendation(curator, title, "Recomendación de prueba"));
    }

    @Test
    void usernameIsUniqueIgnoringCase() {
        newUser("ana");
        assertThrows(DataIntegrityViolationException.class,
                () -> users.saveAndFlush(new User("ANA", "otra@example.com", "hash")));
    }

    @Test
    void emailIsUniqueIgnoringCase() {
        newUser("luis");
        assertThrows(DataIntegrityViolationException.class,
                () -> users.saveAndFlush(new User("otro", "LUIS@example.com", "hash")));
    }

    @Test
    void followingYourselfIsRejectedByTheDatabase() {
        User ana = newUser("ana");
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into follow (follower_id, followed_id) values (?, ?)",
                        ana.getId(), ana.getId()));
    }

    @Test
    void followingTwiceIsRejected() {
        User ana = newUser("ana");
        User luis = newUser("luis");
        follows.saveAndFlush(new Follow(ana, luis));
        assertTrue(follows.isFollowing(ana.getId(), luis.getId()));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into follow (follower_id, followed_id) values (?, ?)",
                        ana.getId(), luis.getId()));
    }

    @Test
    void onlyOneRecommendationPerCuratorAndTitle() {
        User ana = newUser("ana");
        Title title = newTitle(1, 5.0);
        newRecommendation(ana, title);
        assertThrows(DataIntegrityViolationException.class,
                () -> recommendations.saveAndFlush(new Recommendation(ana, title, "Otra más")));
    }

    @Test
    void ratingScoreMustBeBetweenOneAndTen() {
        User ana = newUser("ana");
        User luis = newUser("luis");
        Recommendation recommendation = newRecommendation(ana, newTitle(2, 5.0));
        for (int invalid : new int[] {0, 11}) {
           assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                    "insert into rating (rater_id, recommendation_id, score) values (?, ?, ?)",
                    luis.getId(), recommendation.getId(), invalid));
        }
    }

    @Test
    void onlyOneRatingPerUserAndRecommendation() {
        User ana = newUser("ana");
        User luis = newUser("luis");
        Recommendation recommendation = newRecommendation(ana, newTitle(3, 5.0));
        ratings.saveAndFlush(new Rating(luis, recommendation, 7));
        assertThrows(DataIntegrityViolationException.class,
                () -> ratings.saveAndFlush(new Rating(luis, recommendation, 9)));
    }

    @Test
    void selfRatingIsRejectedByTheDatabaseTrigger() {
        User ana = newUser("ana");
        Recommendation recommendation = newRecommendation(ana, newTitle(4, 5.0));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into rating (rater_id, recommendation_id, score) values (?, ?, ?)",
                ana.getId(), recommendation.getId(), 5));
    }

    @Test
    void deletingARecommendationDeletesItsRatings() {
        User ana = newUser("ana");
        User luis = newUser("luis");
        Recommendation recommendation = newRecommendation(ana, newTitle(5, 5.0));
        ratings.saveAndFlush(new Rating(luis, recommendation, 8));

        jdbc.update("delete from recommendation where id = ?", recommendation.getId());

        Long remaining = jdbc.queryForObject(
                "select count(*) from rating where recommendation_id = ?", Long.class, recommendation.getId());
        assertEquals(0L, remaining.longValue());
    }

    @Test
    void aggregatesMustBeConsistentWithTheRatingScale() {
        User ana = newUser("ana");
        Recommendation recommendation = newRecommendation(ana, newTitle(6, 5.0));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "update recommendation set rating_count = 1, rating_sum = 11 where id = ?",
                recommendation.getId()));
    }

    @Test
    void recommendationKeepsItsPopularitySnapshot() {
        User ana = newUser("ana");
        Title title = newTitle(7, 50.0);
        Recommendation saved = newRecommendation(ana, title);

        title.refreshPopularity(500.0, 99);
        titles.saveAndFlush(title);
        em.clear();

        Recommendation reloaded = recommendations.findById(saved.getId()).orElseThrow();
        assertEquals(50.0, reloaded.getPopularitySnapshot(), 0.0);
    }

    @Test
    void aggregatesAndContributionArePersisted() {
        User ana = newUser("ana");
        Recommendation recommendation = newRecommendation(ana, newTitle(8, 5.0));
        recommendation.addRating(8);
        recommendation.addRating(6);
        recommendation.setContribution(0.5);
        recommendations.saveAndFlush(recommendation);
        em.clear();

        Recommendation reloaded = recommendations.findById(recommendation.getId()).orElseThrow();
        assertEquals(2, reloaded.getRatingCount());
        assertEquals(14L, reloaded.getRatingSum());
        assertEquals(0.5, reloaded.getContribution(), 1e-12);
    }

    @Test
    void titleGenresArePersisted() {
        Genre drama = genres.saveAndFlush(new Genre(18, "Drama"));
        Title title = newTitle(9, 5.0);
        title.getGenres().add(drama);
        titles.saveAndFlush(title);
        em.clear();

        Title reloaded = titles.findByTmdbIdAndMediaType(9, MediaType.MOVIE).orElseThrow();
        assertEquals(1, reloaded.getGenres().size());
    }
}
