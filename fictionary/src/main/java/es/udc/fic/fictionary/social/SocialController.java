package es.udc.fic.fictionary.social;

import es.udc.fic.fictionary.catalog.MediaType;
import es.udc.fic.fictionary.recommendation.dto.RecommendationResponse;
import es.udc.fic.fictionary.social.dto.UserSummaryResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import java.time.LocalDate;
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

    @GetMapping("/users/{id}/followers")
    public ResponseEntity<List<UserSummaryResponse>> getFollowers(@PathVariable Long id) {
        return ResponseEntity.ok(socialService.getFollowers(id));
    }

    @GetMapping("/users/{id}/following")
    public ResponseEntity<List<UserSummaryResponse>> getFollowing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = Long.parseLong(userDetails.getUsername());
        if (!currentUserId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(socialService.getFollowing(id));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<RecommendationResponse>> getFeed(
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(required = false) Integer genre,
            @RequestParam(required = false) Double minPopularity,
            @RequestParam(required = false) Double maxPopularity,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "recent") String sortBy,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        FeedFilter filter = new FeedFilter(
                mediaType, genre, minPopularity, maxPopularity,
                minRating, from, to, sortBy);
        return ResponseEntity.ok(socialService.getFeed(userId, filter));
    }
}
