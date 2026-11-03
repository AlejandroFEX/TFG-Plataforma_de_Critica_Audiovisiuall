package es.udc.fic.fictionary.api;

import com.jayway.jsonpath.JsonPath;
import es.udc.fic.fictionary.TestcontainersConfiguration;
import es.udc.fic.fictionary.api.ApiTestSupport.Session;
import es.udc.fic.fictionary.catalog.TmdbClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static es.udc.fic.fictionary.api.ApiTestSupport.body;
import static es.udc.fic.fictionary.api.TmdbFixtures.DRAMA;
import static es.udc.fic.fictionary.api.TmdbFixtures.HORROR;
import static es.udc.fic.fictionary.api.TmdbFixtures.movie;
import static es.udc.fic.fictionary.api.TmdbFixtures.tv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-06, RF-09 y RF-10: seguir, listas de seguidores y feed con filtros y orden. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SocialApiTests {

    private static final long NICHE_MOVIE = 100;   // popularidad 5, terror
    private static final long POPULAR_MOVIE = 200; // popularidad 500, drama
    private static final long NICHE_TV = 300;      // popularidad 5, drama, serie

    @Autowired WebApplicationContext context;
    @MockitoBean TmdbClient tmdbClient;

    private ApiTestSupport api;

    @BeforeEach
    void setUp() {
        api = ApiTestSupport.create(context);
        when(tmdbClient.getMovie(NICHE_MOVIE))
                .thenReturn(movie(NICHE_MOVIE, "Joya oculta", 5.0, List.of(27), List.of(HORROR)));
        when(tmdbClient.getMovie(POPULAR_MOVIE))
                .thenReturn(movie(POPULAR_MOVIE, "Exito de taquilla", 500.0, List.of(18), List.of(DRAMA)));
        when(tmdbClient.getTv(NICHE_TV))
                .thenReturn(tv(NICHE_TV, "Serie nicho", 5.0, List.of(18), List.of(DRAMA)));
    }

    private void assertFeedSize(Session as, String query, int expected) throws Exception {
        ResultActions result = api.get("/api/feed" + query, as).andExpect(status().isOk());
        List<Object> items = JsonPath.read(body(result), "$");
        assertThat(items).as("feed" + query).hasSize(expected);
    }

    private long firstId(Session as, String query) throws Exception {
        ResultActions result = api.get("/api/feed" + query, as).andExpect(status().isOk());
        Number id = JsonPath.read(body(result), "$[0].id");
        return id.longValue();
    }

    // ------------------------------------------------------------------ seguir

    @Test
    void followRequiresLogin() throws Exception {
        Session ana = api.register("ana");

        api.post("/api/users/" + ana.id() + "/follow", null, null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void followAndUnfollow() throws Exception {
        Session ana = api.register("ana");
        Session bea = api.register("bea");

        api.follow(ana, bea.id());
        api.get("/api/users/" + bea.id() + "/follow", ana)
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        api.delete("/api/users/" + bea.id() + "/follow", ana).andExpect(status().isNoContent());
        api.get("/api/users/" + bea.id() + "/follow", ana)
                .andExpect(content().string("false"));
    }

    @Test
    void cannotFollowTwiceNorYourself() throws Exception {
        Session ana = api.register("ana");
        Session bea = api.register("bea");

        api.follow(ana, bea.id());
        api.post("/api/users/" + bea.id() + "/follow", ana, null)
                .andExpect(status().isConflict());
        api.post("/api/users/" + ana.id() + "/follow", ana, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void followersAreVisibleToEveryoneButFollowingIsPrivate() throws Exception {
        Session ana = api.register("ana");
        Session bea = api.register("bea");
        api.follow(ana, bea.id()); // ana sigue a bea

        api.get("/api/users/" + bea.id() + "/followers", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is("ana")))
                .andExpect(jsonPath("$[0].email").doesNotExist());

        api.get("/api/users/" + ana.id() + "/following", ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username", is("bea")));
        api.get("/api/users/" + ana.id() + "/following", bea)
                .andExpect(status().isForbidden());
        api.get("/api/users/" + ana.id() + "/following", null)
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ feed

    @Test
    void feedRequiresLoginAndStartsEmpty() throws Exception {
        Session ana = api.register("ana");

        api.get("/api/feed", null).andExpect(status().isUnauthorized());
        api.get("/api/feed", ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void feedOnlyShowsFollowedCurators() throws Exception {
        Session ana = api.register("ana");
        Session bea = api.register("bea");
        Session cora = api.register("cora");
        api.publish(bea, NICHE_MOVIE, "MOVIE");
        api.publish(cora, POPULAR_MOVIE, "MOVIE");
        api.follow(ana, bea.id());

        api.get("/api/feed", ana)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].curatorUsername", is("bea")));
    }

    @Test
    void feedFiltersByTypeGenrePopularityRatingAndDate() throws Exception {
        Session lector = api.register("lector");
        Session critico = api.register("critico");
        long nicheMovie = api.publish(critico, NICHE_MOVIE, "MOVIE");
        api.publish(critico, POPULAR_MOVIE, "MOVIE");
        api.publish(critico, NICHE_TV, "TV");
        api.follow(lector, critico.id());

        assertFeedSize(lector, "", 3);
        assertFeedSize(lector, "?mediaType=TV", 1);
        assertFeedSize(lector, "?mediaType=MOVIE", 2);
        assertFeedSize(lector, "?maxPopularity=20", 2);
        assertFeedSize(lector, "?minPopularity=50", 1);
        assertFeedSize(lector, "?genre=27", 1);
        assertFeedSize(lector, "?genre=18", 2);
        assertFeedSize(lector, "?mediaType=MOVIE&maxPopularity=20&genre=27", 1);
        assertFeedSize(lector, "?from=2999-01-01", 0);
        assertFeedSize(lector, "?to=2000-01-01", 0);
        assertFeedSize(lector, "?from=2000-01-01&to=2999-12-31", 3);

        // Valoración mínima: solo cuentan las recomendaciones ya valoradas
        assertFeedSize(lector, "?minRating=8", 0);
        Session fan = api.register("fan");
        api.rate(fan, nicheMovie, 9);
        assertFeedSize(lector, "?minRating=8", 1);
        assertFeedSize(lector, "?minRating=10", 0);
    }

    @Test
    void feedSortOrders() throws Exception {
        Session lector = api.register("lector");
        Session ana = api.register("ana");
        Session bea = api.register("bea");
        Session fan = api.register("fan");

        long first = api.publish(ana, POPULAR_MOVIE, "MOVIE");  // antigua y popular
        long second = api.publish(bea, NICHE_MOVIE, "MOVIE");   // reciente y poco popular
        api.follow(lector, ana.id());
        api.follow(lector, bea.id());
        api.rate(fan, first, 9); // solo ana tiene Critic Points y una recomendación valorada

        assertThat(firstId(lector, "?sortBy=recent")).as("recent").isEqualTo(second);
        assertThat(firstId(lector, "?sortBy=oldest")).as("oldest").isEqualTo(first);
        assertThat(firstId(lector, "?sortBy=criticPoints")).as("criticPoints").isEqualTo(first);
        assertThat(firstId(lector, "?sortBy=rating")).as("rating").isEqualTo(first);
        assertThat(firstId(lector, "?sortBy=leastPopular")).as("leastPopular").isEqualTo(second);
    }
}
