package es.udc.fic.fictionary.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    /** Ranking público: solo usuarios reales. */
    List<User> findBySimulatedFalseOrderByCriticPointsDesc(Pageable pageable);

    /** Buscador de usuarios: solo usuarios reales. */
    List<User> findByUsernameContainingIgnoreCaseAndSimulatedFalseOrderByCriticPointsDescUsernameAsc(
            String username, Pageable pageable);
}
