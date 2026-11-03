package es.udc.fic.fictionary.api;

import es.udc.fic.fictionary.TestcontainersConfiguration;
import es.udc.fic.fictionary.api.ApiTestSupport.Session;
import es.udc.fic.fictionary.catalog.TmdbClient;
import es.udc.fic.fictionary.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-01 y RF-08: registro, inicio de sesión, perfil y privacidad del email. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthApiTests {

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @PersistenceContext EntityManager em;
    // Se mockea en todas las pruebas de API para que compartan un único contexto de Spring
    @MockitoBean TmdbClient tmdbClient;

    private ApiTestSupport api;

    @BeforeEach
    void setUp() {
        api = ApiTestSupport.create(context);
    }

    private static String registerBody(String username, String email, String password) {
        return """
                {"username": "%s", "email": "%s", "password": "%s"}
                """.formatted(username, email, password);
    }

    private static String loginBody(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    @Test
    void registerCreatesTheUserAndReturnsAToken() throws Exception {
        api.post("/api/auth/register", null, registerBody("ana", "ana@example.com", "Test1234"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.username", is("ana")))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerRejectsDuplicatedEmailAndUsername() throws Exception {
        api.register("ana");

        api.post("/api/auth/register", null, registerBody("otra", "ana@example.com", "Test1234"))
                .andExpect(status().isConflict());
        // El nombre de usuario es único sin distinguir mayúsculas
        api.post("/api/auth/register", null, registerBody("ANA", "otra@example.com", "Test1234"))
                .andExpect(status().isConflict());
    }

    @Test
    void registerValidatesTheFields() throws Exception {
        api.post("/api/auth/register", null, registerBody("ana", "ana@example.com", "corta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        api.post("/api/auth/register", null, registerBody("ana", "no-es-un-email", "Test1234"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
        api.post("/api/auth/register", null, registerBody("ab", "ana@example.com", "Test1234"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void loginReturnsATokenForValidCredentials() throws Exception {
        api.register("ana");

        api.post("/api/auth/login", null, loginBody("ana@example.com", "Test1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("ana")))
                .andExpect(jsonPath("$.token").isNotEmpty());
        // El email no distingue mayúsculas
        api.post("/api/auth/login", null, loginBody("ANA@example.com", "Test1234"))
                .andExpect(status().isOk());
    }

    @Test
    void loginFailsWithWrongPasswordOrUnknownEmail() throws Exception {
        api.register("ana");

        api.post("/api/auth/login", null, loginBody("ana@example.com", "Equivocada1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Credenciales incorrectas")));
        api.post("/api/auth/login", null, loginBody("nadie@example.com", "Test1234"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsReturn401WithoutAValidToken() throws Exception {
        api.get("/api/users/me", null).andExpect(status().isUnauthorized());
        api.get("/api/feed", null).andExpect(status().isUnauthorized());
        api.post("/api/recommendations", null, "{}").andExpect(status().isUnauthorized());

        api.mvc().perform(MockMvcRequestBuilders.get("/api/users/me")
                        .header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenOfADeletedUserIsRejected() throws Exception {
        Session ana = api.register("ana");
        users.deleteById(ana.id());
        users.flush();
        em.clear();

        api.get("/api/users/me", ana).andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsTheOwnProfileIncludingEmail() throws Exception {
        Session ana = api.register("ana");

        api.get("/api/users/me", ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("ana@example.com")))
                .andExpect(jsonPath("$.criticPoints", is(0.0)));
    }

    @Test
    void publicProfileHidesTheEmailFromEveryoneButTheOwner() throws Exception {
        Session ana = api.register("ana");
        Session luis = api.register("luis");
        String url = "/api/users/" + ana.id() + "/profile";

        api.get(url, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("ana")))
                .andExpect(jsonPath("$.email").doesNotExist());
        api.get(url, luis)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist());
        api.get(url, ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("ana@example.com")));
    }

    @Test
    void bioCanBeUpdated() throws Exception {
        Session ana = api.register("ana");

        api.patch("/api/users/me", null, "{\"bio\": \"Hola\"}")
                .andExpect(status().isUnauthorized());
        api.patch("/api/users/me", ana, "{\"bio\": \"Cinefilo de barrio\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio", is("Cinefilo de barrio")));
        api.get("/api/users/" + ana.id() + "/profile", null)
                .andExpect(jsonPath("$.bio", is("Cinefilo de barrio")));
    }
}
