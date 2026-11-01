package es.udc.fic.fictionary.user;

import es.udc.fic.fictionary.user.dto.AuthResponse;
import es.udc.fic.fictionary.user.dto.LoginRequest;
import es.udc.fic.fictionary.user.dto.RegisterRequest;
import es.udc.fic.fictionary.user.dto.UserProfileResponse;
import es.udc.fic.fictionary.user.dto.UserProfileResponse;
import es.udc.fic.fictionary.user.dto.RankingEntryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        User user = userService.register(
                request.username(), request.email(), request.password());
        String token = userService.login(request.email(), request.password());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toAuthResponse(user, token));
    }

   @PostMapping("/auth/login")
public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request) {
    String token = userService.login(request.email(), request.password());
    User user = userService.getProfileByEmail(request.email());
    return ResponseEntity.ok(toAuthResponse(user, token));
}
    @GetMapping("/users/{id}/profile")
public ResponseEntity<UserProfileResponse> getProfile(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails) {
    User user = userService.getProfile(id);
    UserProfileResponse full = toProfileResponse(user);
    boolean isOwner = userDetails != null
            && userDetails.getUsername().equals(String.valueOf(id));
    if (isOwner) {
        return ResponseEntity.ok(full);
    }
    return ResponseEntity.ok(new UserProfileResponse(
            full.id(), full.username(), null, full.bio(),
            full.criticPoints(), full.createdAt(), full.avatarUrl()));
}

    @GetMapping("/users/me")
    public ResponseEntity<UserProfileResponse> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        User user = userService.getProfile(userId);
        return ResponseEntity.ok(toProfileResponse(user));
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                token);
    }

    private UserProfileResponse toProfileResponse(User user) {
    return new UserProfileResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getBio(),
            user.getCriticPoints(),
            user.getCreatedAt(),
            user.getAvatarUrl());
}
    @GetMapping("/ranking")
public ResponseEntity<List<RankingEntryResponse>> getRanking(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(userService.getRanking(page, size));
}

    @PostMapping("/users/me/avatar")
public ResponseEntity<UserProfileResponse> uploadAvatar(
        @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
        @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.parseLong(userDetails.getUsername());
    User user = userService.uploadAvatar(userId, file);
    return ResponseEntity.ok(toProfileResponse(user));
}

@PatchMapping("/users/me")
public ResponseEntity<UserProfileResponse> updateProfile(
        @RequestBody java.util.Map<String, String> body,
        @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.parseLong(userDetails.getUsername());
    User user = userService.updateProfile(userId, body.get("bio"));
    return ResponseEntity.ok(toProfileResponse(user));
}



}