package es.udc.fic.fictionary;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FictionaryApplicationTests {

    @Test
    void contextLoads() {
        // Comprueba que arranca el contexto, que Flyway aplica las migraciones
        // y que Hibernate valida el esquema contra las entidades.
    }

}
