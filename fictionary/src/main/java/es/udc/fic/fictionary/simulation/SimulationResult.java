package es.udc.fic.fictionary.simulation;

import java.util.List;

public record SimulationResult(
        int numCurators,
        int numVoters,
        int numTitles,
        long seed,
        String weightFunction,
        List<CuratorResult> ranking
) {
    public record CuratorResult(
            String curatorUsername,
            String titleName,
            double popularitySnapshot,
            int ratingCount,
            double averageRating,
            double contribution,
            double criticPoints
    ) {}
}