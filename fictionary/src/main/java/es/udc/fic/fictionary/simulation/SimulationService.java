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
        Random rng = new Random(seed);

        // 1. Crear títulos con popularidades variadas
        List<Title> titles = createTitles(numTitles, rng);

        // 2. Crear curadores
        List<User> curators = createUsers("curator", numCurators);

        // 3. Crear votantes
        List<User> voters = createUsers("voter", numVoters);

        // 4. Cada curador recomienda un título aleatorio
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

        // 5. Cada votante vota todas las recomendaciones con puntuación aleatoria 7-10
        for (User voter : voters) {
            for (Recommendation rec : recommendations) {
                int score = 7 + rng.nextInt(4);
                Rating rating = new Rating(voter, rec, score);
                ratingRepository.save(rating);
                rec.addRating(score);
            }
        }

        // 6. Recalcular contributions y Critic Points
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

        // 7. Construir resultado ordenado por Critic Points
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
                numCurators, numVoters, numTitles, seed,
                contributionCalculator.weightFunction().getClass().getSimpleName(),
                results);
    }

    private List<Title> createTitles(int count, Random rng) {
        List<Title> titles = new ArrayList<>();
        // Popularidades distribuidas: nicho (1-20), media (20-60), popular (60-200)
        double[] popularities = new double[count];
        for (int i = 0; i < count; i++) {
            if (i < count / 3) {
                popularities[i] = 1 + rng.nextDouble() * 19; // nicho
            } else if (i < 2 * count / 3) {
                popularities[i] = 20 + rng.nextDouble() * 40; // media
            } else {
                popularities[i] = 60 + rng.nextDouble() * 140; // popular
            }
        }
        for (int i = 0; i < count; i++) {
            Title title = new Title(
                    90000L + i, MediaType.MOVIE,
                    "Título simulado " + (i + 1),
                    popularities[i], rng.nextInt(10000));
            titleRepository.save(title);
            titles.add(title);
        }
        return titles;
    }

    private List<User> createUsers(String prefix, int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String suffix = prefix + "_sim_" + System.nanoTime() + "_" + i;
            User user = new User(suffix, suffix + "@sim.test",
                    "$2a$10$simulatedHashForTestingOnly123456");
            userRepository.save(user);
            users.add(user);
        }
        return users;
    }
}