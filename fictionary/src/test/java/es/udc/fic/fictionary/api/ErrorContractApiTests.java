package es.udc.fic.fictionary.api;

import es.udc.fic.fictionary.TestcontainersConfiguration;
import es.udc.fic.fictionary.api.ApiTestSupport.Session;
import es.udc.fic.fictionary.catalog.TmdbClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de errores de la API: cada situación devuelve su código HTTP y un cuerpo JSON
 * {status, message}. 404 recurso inexistente, 400 petición inválida, 405 método no permitido.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ErrorContractApiTests {

    @Autowired WebApplicationContext context;
    @MockitoBean TmdbClient tmdbClient;

    private ApiTestSupport api;

    @BeforeEach
    void setUp() {
        api = ApiTestSupport.create(context);
    }

    @Test
    void unknownResourcesReturn404WithAJsonBody() throws Exception {
        Session ana = api.register("ana");

        api.get("/api/users/999999/profile", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message").isNotEmpty());
        api.get("/api/recommendations/999999", null).andExpect(status().isNotFound());
        // Una URL que no existe es 404, no un 500
        api.get("/api/no-existe", ana).andExpect(status().isNotFound());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        api.post("/api/auth/register", null, "{esto no es json")
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        Session ana = api.register("ana");

        api.delete("/api/auth/login", ana).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void ratingAnUnknownRecommendationReturns404() throws Exception {
        Session ana = api.register("ana");

        api.rateRaw(ana, 999999, 9).andExpect(status().isNotFound());
    }

    @Test
    void followingAnUnknownUserReturns404() throws Exception {
        Session ana = api.register("ana");

        api.post("/api/users/999999/follow", ana, null).andExpect(status().isNotFound());
    }

    @Test
    void unfollowingSomeoneYouDoNotFollowReturns404() throws Exception {
        Session ana = api.register("ana");
        Session bea = api.register("bea");

        api.delete("/api/users/" + bea.id() + "/follow", ana).andExpect(status().isNotFound());
    }

    @Test
    void publishingAnUnknownTitleReturns404() throws Exception {
        Session ana = api.register("ana");
        // El mock de TMDB devuelve null para el id 999: el título no existe
        String body = "{\"tmdbId\": 999, \"mediaType\": \"MOVIE\", \"text\": \""
                + ApiTestSupport.TEXT + "\"}";

        api.post("/api/recommendations", ana, body).andExpect(status().isNotFound());
    }

    @Test
    void uploadingAFileThatIsNotAnImageReturns400() throws Exception {
        Session ana = api.register("ana");
        MockMultipartFile notAnImage =
                new MockMultipartFile("file", "nota.txt", "text/plain", "hola".getBytes());

        api.mvc().perform(MockMvcRequestBuilders.multipart("/api/users/me/avatar")
                        .file(notAnImage)
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isBadRequest());
    }
}
