package es.udc.fic.fictionary.simulation;

import es.udc.fic.fictionary.catalog.MediaType;
import es.udc.fic.fictionary.catalog.Title;
import es.udc.fic.fictionary.catalog.TitleRepository;
import es.udc.fic.fictionary.ranking.ContributionCalculator;
import es.udc.fic.fictionary.ranking.PopularityInfo;
import es.udc.fic.fictionary.recommendation.Rating;
import es.udc.fic.fictionary.recommendation.RatingRepository;
import es.udc.fic.fictionary.recommendation.Recommendation;
import es.udc.fic.fictionary.recommendation.RecommendationRepository;
import es.udc.fic.fictionary.user.User;
import es.udc.fic.fictionary.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class SimulationService {

    /** Tope por tipo de entidad, para que una llamada no pueda llenar la BD. */
    private static final int MAX_ENTITIES = 200;

    private final UserRepository userRepository;
    private final TitleRepository titleRepository;
    private final RecommendationRepository recommendationRepository;
    private final RatingRepository ratingRepository;
    private final ContributionCalculator contributionCalculator;

    public SimulationService(
            UserRepository userRepository,
            TitleRepository titleRepository,
            RecommendationRepository recommendationRepository,
            RatingRepository ratingRepository,
            ContributionCalculator contributionCalculator) {
        this.userRepository = userRepository;
        this.titleRepository = titleRepository;
        this.recommendationRepository = recommendationRepository;
        this.ratingRepository = ratingRepository;
        this.contributionCalculator = contributionCalculator;
    }

    public SimulationResult run(int numCurators, int numVoters, int numTitles, long seed) {
        int curatorsN = clamp(numCurators);
        int votersN = clamp(numVoters);
        int titlesN = clamp(numTitles);

        Random rng = new Random(seed);
        // Identificador corto de la ejecución: mantiene los usernames dentro de 30 caracteres
        String runId = Long.toString(Math.abs(System.nanoTime()) % 100_000_000L);

        // 1. Títulos con popularidades variadas
        List<Title> titles = createTitles(titlesN, rng);

        // 2. Curadores y votantes (marcados como simulados)
        List<User> curators = createUsers("curator", curatorsN, runId);
        List<User> voters = createUsers("voter", votersN, runId);

        // 3. Cada curador recomienda un título
        List<Recommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < curators.size(); i++) {
            User curator = curators.get(i);
            Title title = titles.get(i % titles.size());
            Recommendation rec = new Recommendation(curator, title,
                    "Recomendación simulada de " + curator.getUsername()
                    + " sobre " + title.getName());
            recommendationRepository.save(rec);
            recommendations.add(rec);
        }

        // 4. Cada votante puntúa todas las recomendaciones con una nota aleatoria 7-10
        for (User voter : voters) {
            for (Recommendation rec : recommendations) {
                int score = 7 + rng.nextInt(4);
                ratingRepository.save(new Rating(voter, rec, score));
                rec.addRating(score);
            }
        }

        // 5. Contribuciones y Critic Points
        for (Recommendation rec : recommendations) {
            double contribution = contributionCalculator.contribution(
                    rec.getRatingCount(),
                    rec.getRatingSum(),
                    PopularityInfo.of(rec.getPopularitySnapshot()));
            rec.setContribution(contribution);
        }

        for (User curator : curators) {
            double total = recommendations.stream()
                    .filter(r -> r.getCurator().getId().equals(curator.getId()))
                    .mapToDouble(Recommendation::getContribution)
                    .sum();
            curator.setCriticPoints(total);
        }

        // 6. Resultado ordenado por Critic Points
        List<SimulationResult.CuratorResult> results = curators.stream()
                .map(c -> {
                    Recommendation rec = recommendations.stream()
                            .filter(r -> r.getCurator().getId().equals(c.getId()))
                            .findFirst().orElseThrow();
                    return new SimulationResult.CuratorResult(
                            c.getUsername(),
                            rec.getTitle().getName(),
                            rec.getPopularitySnapshot(),
                            rec.getRatingCount(),
                            rec.averageRating(),
                            rec.getContribution(),
                            c.getCriticPoints()
                    );
                })
                .sorted((a, b) -> Double.compare(b.criticPoints(), a.criticPoints()))
                .toList();

        return new SimulationResult(
                curatorsN, votersN, titlesN, seed,
                contributionCalculator.weightFunction().getClass().getSimpleName(),
                results);
    }

    private List<Title> createTitles(int count, Random rng) {
        // Popularidades: nicho (1-20), media (20-60), popular (60-200)
        double[] popularities = new double[count];
        for (int i = 0; i < count; i++) {
            if (i < count / 3) {
                popularities[i] = 1 + rng.nextDouble() * 19;
            } else if (i < 2 * count / 3) {
                popularities[i] = 20 + rng.nextDouble() * 40;
            } else {
                popularities[i] = 60 + rng.nextDouble() * 140;
            }
        }

        // Ids NEGATIVOS y distintos en cada ejecución: TMDB solo usa ids positivos,
        // así que un título simulado nunca puede confundirse con uno real.
        long base = -(System.currentTimeMillis() * 1000L);

        List<Title> titles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Title title = new Title(
                    base - i, MediaType.MOVIE,
                    "Título simulado " + (i + 1),
                    popularities[i], rng.nextInt(10000));
            titleRepository.save(title);
            titles.add(title);
        }
        return titles;
    }

    private List<User> createUsers(String prefix, int count, String runId) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String username = prefix + "_sim_" + runId + "_" + i;
            users.add(userRepository.save(User.simulated(
                    username, username + "@sim.test",
                    "$2a$10$simulatedHashForTestingOnly123456")));
        }
        return users;
    }

    private static int clamp(int n) {
        return Math.min(Math.max(n, 1), MAX_ENTITIES);
    }
}
