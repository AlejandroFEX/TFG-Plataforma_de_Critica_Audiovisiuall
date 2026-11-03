package es.udc.fic.fictionary.api;

import com.jayway.jsonpath.JsonPath;
import es.udc.fic.fictionary.TestcontainersConfiguration;
import es.udc.fic.fictionary.api.ApiTestSupport.Session;
import es.udc.fic.fictionary.catalog.TmdbClient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static es.udc.fic.fictionary.api.ApiTestSupport.number;
import static es.udc.fic.fictionary.api.TmdbFixtures.DRAMA;
import static es.udc.fic.fictionary.api.TmdbFixtures.HORROR;
import static es.udc.fic.fictionary.api.TmdbFixtures.movie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-03, RF-04, RF-05: publicar, valorar y Critic Points. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RecommendationApiTests {

    private static final long NICHE = 100;    // popularidad 5, terror
    private static final long POPULAR = 200;  // popularidad 500, drama

    @Autowired WebApplicationContext context;
    @PersistenceContext EntityManager em;
    @MockitoBean TmdbClient tmdbClient;

    private ApiTestSupport api;

    @BeforeEach
    void setUp() {
        api = ApiTestSupport.create(context);
        when(tmdbClient.getMovie(NICHE))
                .thenReturn(movie(NICHE, "Joya oculta", 5.0, List.of(27), List.of(HORROR)));
        when(tmdbClient.getMovie(POPULAR))
                .thenReturn(movie(POPULAR, "Exito de taquilla", 500.0, List.of(18), List.of(DRAMA)));
    }

    private static String publishBody(Long tmdbId, String mediaType, String text) {
        return """
                {"tmdbId": %s, "mediaType": %s, "text": %s}
                """.formatted(
                tmdbId, mediaType == null ? "null" : "\"" + mediaType + "\"", "\"" + text + "\"");
    }

    @Test
    void publishRequiresLogin() throws Exception {
        api.post("/api/recommendations", null, publishBody(NICHE, "MOVIE", ApiTestSupport.TEXT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publishStoresTheRecommendation() throws Exception {
        Session ana = api.register("ana");

        api.post("/api/recommendations", ana, publishBody(NICHE, "MOVIE", ApiTestSupport.TEXT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titleName", is("Joya oculta")))
                .andExpect(jsonPath("$.curatorUsername", is("ana")))
                .andExpect(jsonPath("$.popularitySnapshot", is(5.0)))
                .andExpect(jsonPath("$.hiddenGem", is(true)))
                .andExpect(jsonPath("$.ratingCount", is(0)))
                .andExpect(jsonPath("$.contribution", is(0.0)));
    }

    @Test
    void cannotRecommendTheSameTitleTwice() throws Exception {
        Session ana = api.register("ana");
        api.publish(ana, NICHE, "MOVIE");

        api.post("/api/recommendations", ana, publishBody(NICHE, "MOVIE", ApiTestSupport.TEXT))
                .andExpect(status().isConflict());
    }

    @Test
    void publishValidatesTheText() throws Exception {
        Session ana = api.register("ana");

        api.post("/api/recommendations", ana, publishBody(NICHE, "MOVIE", "corto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.text").exists());
        api.post("/api/recommendations", ana, publishBody(NICHE, null, ApiTestSupport.TEXT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.mediaType").exists());
    }

    @Test
    void recommendationsAreReadableWithoutLogin() throws Exception {
        Session ana = api.register("ana");
        long id = api.publish(ana, NICHE, "MOVIE");

        api.get("/api/recommendations/" + id, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleName", is("Joya oculta")));
        api.get("/api/recommendations/users/" + ana.id(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void ratingUpdatesAverageContributionAndCriticPoints() throws Exception {
        Session ana = api.register("ana");
        Session luis = api.register("luis");
        long id = api.publish(ana, NICHE, "MOVIE");

        ResultActions rated = api.rateRaw(luis, id, 9)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratingCount", is(1)))
                .andExpect(jsonPath("$.averageRating", is(9.0)))
                .andExpect(numberGreaterThan("$.contribution", 0.0))
                .andExpect(numberGreaterThan("$.curatorCriticPoints", 0.0));

        double contribution = number(rated, "$.contribution");
        api.get("/api/users/" + ana.id() + "/profile", null)
                .andExpect(numberCloseTo("$.criticPoints", contribution));
    }

    @Test
    void ratingAgainReplacesThePreviousScore() throws Exception {
        Session ana = api.register("ana");
        Session luis = api.register("luis");
        long id = api.publish(ana, NICHE, "MOVIE");

        api.rate(luis, id, 9);
        api.rateRaw(luis, id, 5)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratingCount", is(1)))
                .andExpect(jsonPath("$.averageRating", is(5.0)));
    }

    @Test
    void cannotRateYourOwnRecommendation() throws Exception {
        Session ana = api.register("ana");
        long id = api.publish(ana, NICHE, "MOVIE");

        api.rateRaw(ana, id, 10).andExpect(status().isForbidden());
        api.get("/api/recommendations/" + id, null)
                .andExpect(jsonPath("$.ratingCount", is(0)));
    }

    @Test
    void scoreMustBeBetweenOneAndTen() throws Exception {
        Session ana = api.register("ana");
        Session luis = api.register("luis");
        long id = api.publish(ana, NICHE, "MOVIE");

        api.rateRaw(luis, id, 0).andExpect(status().isBadRequest());
        api.rateRaw(luis, id, 11).andExpect(status().isBadRequest());
    }

    @Test
    void rateRequiresLogin() throws Exception {
        Session ana = api.register("ana");
        long id = api.publish(ana, NICHE, "MOVIE");

        api.rateRaw(null, id, 9).andExpect(status().isUnauthorized());
    }

    @Test
    void criticPointsNeverGoBelowZero() throws Exception {
        Session ana = api.register("ana");
        Session luis = api.register("luis");
        long id = api.publish(ana, NICHE, "MOVIE");

        // Una nota muy baja resta en la contribución de la recomendación...
        api.rateRaw(luis, id, 1)
                .andExpect(status().isOk())
                .andExpect(numberLessThan("$.contribution", 0.0))
                .andExpect(jsonPath("$.curatorCriticPoints", is(0.0)));
        // ...pero los Critic Points del curador no bajan de 0
        api.get("/api/users/" + ana.id() + "/profile", null)
                .andExpect(jsonPath("$.criticPoints", is(0.0)));
    }

    /**
     * Tesis del proyecto, de punta a punta: con las mismas notas, recomendar un título poco
     * popular aporta más Critic Points que recomendar uno muy popular (con la función de peso
     * por defecto, LOG_INVERSE; con NONE las dos contribuciones serían iguales).
     */
    @Test
    void nicheRecommendationsEarnMoreThanPopularOnes() throws Exception {
        Session ana = api.register("ana_curadora");
        Session bea = api.register("bea_curadora");
        Session uno = api.register("lector_uno");
        Session dos = api.register("lector_dos");

        long nicheRec = api.publish(ana, NICHE, "MOVIE");
        long popularRec = api.publish(bea, POPULAR, "MOVIE");
        for (Session lector : List.of(uno, dos)) {
            api.rate(lector, nicheRec, 9);
            api.rate(lector, popularRec, 9);
        }

        double niche = number(api.get("/api/recommendations/" + nicheRec, null), "$.contribution");
        double popular = number(api.get("/api/recommendations/" + popularRec, null), "$.contribution");

        assertThat(popular).isPositive();
        assertThat(niche).isGreaterThan(popular);
        api.get("/api/ranking", null)
                .andExpect(jsonPath("$[0].username", is("ana_curadora")));
    }

    @Test
    void onlyTheAuthorCanDeleteARecommendation() throws Exception {
        Session ana = api.register("ana");
        Session luis = api.register("luis");
        long id = api.publish(ana, NICHE, "MOVIE");
        api.rate(luis, id, 9);

        // Cada petición real usa su propia transacción: se limpia el contexto de persistencia
        em.flush();
        em.clear();

        api.delete("/api/recommendations/" + id, luis).andExpect(status().isForbidden());
        api.delete("/api/recommendations/" + id, ana).andExpect(status().isNoContent());
        api.get("/api/recommendations/" + id, null).andExpect(status().isNotFound());

        api.get("/api/recommendations/users/" + ana.id(), null)
                .andExpect(jsonPath("$", hasSize(0)));
        api.get("/api/users/" + ana.id() + "/profile", null)
                .andExpect(jsonPath("$.criticPoints", is(0.0)));
    }

    // JsonPath devuelve BigDecimal para decimales largos y Double para los cortos: los
    // comparadores de Hamcrest con Double fallarían, así que se comparan como double.
    private static double numberAt(MvcResult result, String path) throws Exception {
        Number value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return value.doubleValue();
    }

    private static ResultMatcher numberGreaterThan(String path, double bound) {
        return result -> assertThat(numberAt(result, path)).as(path).isGreaterThan(bound);
    }

    private static ResultMatcher numberLessThan(String path, double bound) {
        return result -> assertThat(numberAt(result, path)).as(path).isLessThan(bound);
    }

    private static ResultMatcher numberCloseTo(String path, double expected) {
        return result -> assertThat(numberAt(result, path)).as(path)
                .isCloseTo(expected, within(1e-9));
    }
}
