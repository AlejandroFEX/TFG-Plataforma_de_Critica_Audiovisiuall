package es.udc.fic.fictionary.social;

import es.udc.fic.fictionary.catalog.MediaType;
import es.udc.fic.fictionary.recommendation.dto.RecommendationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @PostMapping("/users/{id}/follow")
    public ResponseEntity<Void> follow(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long followerId = Long.parseLong(userDetails.getUsername());
        socialService.follow(followerId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}/follow")
    public ResponseEntity<Void> unfollow(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long followerId = Long.parseLong(userDetails.getUsername());
        socialService.unfollow(followerId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}/follow")
    public ResponseEntity<Boolean> isFollowing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long followerId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(socialService.isFollowing(followerId, id));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<RecommendationResponse>> getFeed(
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(required = false) Double maxPopularity,
            @RequestParam(required = false, defaultValue = "recent") String sortBy,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                socialService.getFeed(userId, mediaType, maxPopularity, sortBy));
    }
}