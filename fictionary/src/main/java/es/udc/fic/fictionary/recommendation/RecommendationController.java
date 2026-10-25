package es.udc.fic.fictionary.recommendation;

import es.udc.fic.fictionary.recommendation.dto.RatingRequest;
import es.udc.fic.fictionary.recommendation.dto.RecommendationRequest;
import es.udc.fic.fictionary.recommendation.dto.RecommendationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ResponseEntity<RecommendationResponse> publish(
            @Valid @RequestBody RecommendationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long curatorId = Long.parseLong(userDetails.getUsername());
        RecommendationResponse response = recommendationService.publish(
                curatorId, request.tmdbId(), request.mediaType(), request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/ratings")
    public ResponseEntity<RecommendationResponse> rate(
            @PathVariable Long id,
            @Valid @RequestBody RatingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long raterId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(recommendationService.rate(raterId, id, request.score()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecommendationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recommendationService.getById(id));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<RecommendationResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(recommendationService.getByUser(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long curatorId = Long.parseLong(userDetails.getUsername());
        recommendationService.delete(curatorId, id);
        return ResponseEntity.noContent().build();
    }
}