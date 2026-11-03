package es.udc.fic.fictionary.api;

import es.udc.fic.fictionary.TestcontainersConfiguration;
import es.udc.fic.fictionary.api.ApiTestSupport.Session;
import es.udc.fic.fictionary.catalog.TmdbClient;
import es.udc.fic.fictionary.user.User;
import es.udc.fic.fictionary.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static es.udc.fic.fictionary.api.TmdbFixtures.HORROR;
import static es.udc.fic.fictionary.api.TmdbFixtures.movie;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-11 (ranking), buscador de usuarios y módulo de simulación (RF-14). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RankingAndSearchApiTests {

    private static final long NICHE = 100;
    private static final long REAL_TMDB_ID_IN_THE_OLD_SIMULATION_RANGE = 90000;

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @MockitoBean TmdbClient tmdbClient;

    private ApiTestSupport api;

    @BeforeEach
    void setUp() {
        api = ApiTestSupport.create(context);
        when(tmdbClient.getMovie(NICHE))
                .thenReturn(movie(NICHE, "Joya oculta", 5.0, List.of(27), List.of(HORROR)));
    }

    private void saveSimulatedUser(String username, double criticPoints) {
        User simulated = User.simulated(username, username + "@sim.test", "hash");
        simulated.setCriticPoints(criticPoints);
        users.saveAndFlush(simulated);
    }

    // ------------------------------------------------------------------ ranking

    @Test
    void rankingIsPublicAndOrderedByCriticPoints() throws Exception {
        Session ana = api.register("ana_critica");
        Session luis = api.register("luis_lector");
        long id = api.publish(ana, NICHE, "MOVIE");
        api.rate(luis, id, 9);

        api.get("/api/ranking", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username", is("ana_critica")))
                .andExpect(jsonPath("$[0].criticPoints").isNumber());
    }

    @Test
    void rankingExcludesSimulatedUsersAndHidesEmails() throws Exception {
        api.register("ana_critica");
        saveSimulatedUser("sim_top", 99.0); // iría el primero si no se excluyera

        api.get("/api/ranking", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItem("ana_critica")))
                .andExpect(jsonPath("$[*].username", not(hasItem("sim_top"))))
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    // ------------------------------------------------------------------ buscador de usuarios

    @Test
    void userSearchIsCaseInsensitiveAndExcludesSimulatedUsers() throws Exception {
        api.register("pepe_critico");
        api.register("PEPA_CRITICA");
        api.register("juan");
        saveSimulatedUser("pep_sim", 0.0);

        api.get("/api/users/search?q=pep", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", containsInAnyOrder("pepe_critico", "PEPA_CRITICA")))
                .andExpect(jsonPath("$[0].email").doesNotExist());
        api.get("/api/users/search?q=PEP", null)
                .andExpect(jsonPath("$", hasSize(2)));
        // Menos de 2 caracteres, o sin coincidencias: lista vacía
        api.get("/api/users/search?q=p", null)
                .andExpect(jsonPath("$", hasSize(0)));
        api.get("/api/users/search?q=zzz", null)
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ------------------------------------------------------------------ simulación

    @Test
    void simulationRequiresLogin() throws Exception {
        api.post("/api/simulation/run", null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void simulationDoesNotPolluteTheRanking() throws Exception {
        Session evaluador = api.register("evaluador");

        api.post("/api/simulation/run?curators=3&voters=2&titles=3&seed=1", evaluador, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ranking", hasSize(3)));
        // Una segunda ejecución no choca con la primera (ids de título distintos)
        api.post("/api/simulation/run?curators=3&voters=2&titles=3&seed=2", evaluador, null)
                .andExpect(status().isOk());

        api.get("/api/ranking", null)
                .andExpect(jsonPath("$[*].username", hasItem("evaluador")))
                .andExpect(jsonPath("$[*].username", everyItem(not(startsWith("curator_sim")))))
                .andExpect(jsonPath("$[*].username", everyItem(not(startsWith("voter_sim")))));
    }

    /** Regresión: los títulos simulados usaban ids 90000+, que son ids reales de TMDB. */
    @Test
    void simulatedTitlesNeverCollideWithRealTmdbIds() throws Exception {
        when(tmdbClient.getMovie(REAL_TMDB_ID_IN_THE_OLD_SIMULATION_RANGE))
                .thenReturn(movie(REAL_TMDB_ID_IN_THE_OLD_SIMULATION_RANGE,
                        "Pelicula real 90000", 12.0, List.of(), List.of()));
        Session evaluador = api.register("evaluador");

        api.post("/api/simulation/run?curators=3&voters=2&titles=3&seed=1", evaluador, null)
                .andExpect(status().isOk());

        api.get("/api/catalog/" + REAL_TMDB_ID_IN_THE_OLD_SIMULATION_RANGE + "?type=MOVIE", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Pelicula real 90000")));
    }
}
