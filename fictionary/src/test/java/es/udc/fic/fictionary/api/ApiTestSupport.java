package es.udc.fic.fictionary.api;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Utilidades comunes de las pruebas de API: peticiones con sesión y flujos habituales. */
final class ApiTestSupport {

    static final String TEXT = "Una recomendacion de prueba con texto suficiente.";

    /** Usuario registrado en la prueba, con su token JWT. */
    record Session(long id, String username, String token) {}

    private final MockMvc mvc;

    private ApiTestSupport(MockMvc mvc) {
        this.mvc = mvc;
    }

    /** MockMvc sobre la aplicación completa, con la cadena real de Spring Security. */
    static ApiTestSupport create(WebApplicationContext context) {
        return new ApiTestSupport(
                MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build());
    }

    MockMvc mvc() {
        return mvc;
    }

    // ------------------------------------------------------------------ peticiones

    ResultActions get(String url, Session as) throws Exception {
        return send(MockMvcRequestBuilders.get(url), as);
    }

    ResultActions post(String url, Session as, String jsonBody) throws Exception {
        return send(withBody(MockMvcRequestBuilders.post(url), jsonBody), as);
    }

    ResultActions patch(String url, Session as, String jsonBody) throws Exception {
        return send(withBody(MockMvcRequestBuilders.patch(url), jsonBody), as);
    }

    ResultActions delete(String url, Session as) throws Exception {
        return send(MockMvcRequestBuilders.delete(url), as);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, Session as) throws Exception {
        if (as != null) {
            request.header("Authorization", "Bearer " + as.token());
        }
        return mvc.perform(request);
    }

    private static MockHttpServletRequestBuilder withBody(
            MockHttpServletRequestBuilder request, String jsonBody) {
        if (jsonBody != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(jsonBody);
        }
        return request;
    }

    // ------------------------------------------------------------------ flujos

    /** Registra un usuario (email = nombre en minúsculas + @example.com, clave Test1234). */
    Session register(String username) throws Exception {
        String body = """
                {"username": "%s", "email": "%s@example.com", "password": "Test1234"}
                """.formatted(username, username.toLowerCase());
        ResultActions result = post("/api/auth/register", null, body)
                .andExpect(status().isCreated());
        return new Session(
                longValue(result, "$.userId"),
                username,
                JsonPath.<String>read(body(result), "$.token"));
    }

    /** Publica una recomendación y devuelve su id. */
    long publish(Session as, long tmdbId, String mediaType) throws Exception {
        String body = """
                {"tmdbId": %d, "mediaType": "%s", "text": "%s"}
                """.formatted(tmdbId, mediaType, TEXT);
        return longValue(
                post("/api/recommendations", as, body).andExpect(status().isCreated()), "$.id");
    }

    ResultActions rateRaw(Session as, long recommendationId, int score) throws Exception {
        return post("/api/recommendations/" + recommendationId + "/ratings",
                as, "{\"score\": " + score + "}");
    }

    void rate(Session as, long recommendationId, int score) throws Exception {
        rateRaw(as, recommendationId, score).andExpect(status().isOk());
    }

    void follow(Session as, long userId) throws Exception {
        post("/api/users/" + userId + "/follow", as, null).andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------ lectura de JSON

    static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    static double number(ResultActions result, String path) throws Exception {
        Number value = JsonPath.read(body(result), path);
        return value.doubleValue();
    }

    static long longValue(ResultActions result, String path) throws Exception {
        Number value = JsonPath.read(body(result), path);
        return value.longValue();
    }
}
