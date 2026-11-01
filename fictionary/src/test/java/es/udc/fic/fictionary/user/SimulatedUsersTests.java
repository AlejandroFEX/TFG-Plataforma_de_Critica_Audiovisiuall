package es.udc.fic.fictionary.user;

import es.udc.fic.fictionary.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los usuarios simulados no deben aparecer en el ranking ni en el buscador de usuarios. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SimulatedUsersTests {

    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;

    private void saveRealAndSimulated() {
        users.saveAndFlush(new User("usuario_real", "real@example.com", "hash"));
        users.saveAndFlush(User.simulated("usuario_sim", "usuario_sim@sim.test", "hash"));
    }

    @Test
    void theSimulatedFlagIsPersisted() {
        saveRealAndSimulated();
        Boolean real = jdbc.queryForObject(
                "select simulated from app_user where username = ?", Boolean.class, "usuario_real");
        Boolean simulated = jdbc.queryForObject(
                "select simulated from app_user where username = ?", Boolean.class, "usuario_sim");
        assertFalse(real);
        assertTrue(simulated);
    }

    @Test
    void rankingExcludesSimulatedUsers() {
        saveRealAndSimulated();
        List<String> names = users
                .findBySimulatedFalseOrderByCriticPointsDesc(PageRequest.of(0, 50))
                .stream().map(User::getUsername).toList();
        assertTrue(names.contains("usuario_real"));
        assertFalse(names.contains("usuario_sim"));
    }

    @Test
    void userSearchExcludesSimulatedUsers() {
        saveRealAndSimulated();
        List<String> names = users
                .findByUsernameContainingIgnoreCaseAndSimulatedFalseOrderByCriticPointsDescUsernameAsc(
                        "usuario", PageRequest.of(0, 50))
                .stream().map(User::getUsername).toList();
        assertEquals(List.of("usuario_real"), names);
    }
}
