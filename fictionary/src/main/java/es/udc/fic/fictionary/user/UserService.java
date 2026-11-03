package es.udc.fic.fictionary.user;

import es.udc.fic.fictionary.common.NotFoundException;
import es.udc.fic.fictionary.common.ConflictException;

import es.udc.fic.fictionary.security.JwtTokenProvider;
import es.udc.fic.fictionary.user.dto.UserProfileResponse;
import es.udc.fic.fictionary.user.dto.RankingEntryResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
private final AvatarStorageService avatarStorageService;

public UserService(UserRepository userRepository,
                   PasswordEncoder passwordEncoder,
                   JwtTokenProvider jwtTokenProvider,
                   AvatarStorageService avatarStorageService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.avatarStorageService = avatarStorageService;
}

    public User register(String username, String email, String rawPassword) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("El email ya está registrado: " + email);
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("El username ya está en uso: " + username);
        }
        User user = new User(username, email, passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }
        return jwtTokenProvider.generateToken(user.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + userId));
    }

    @Transactional(readOnly = true)
    public User getProfileByEmail(String email) {   
    return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + email));
}

    public String getUserIdFromToken(String token) {
    return String.valueOf(jwtTokenProvider.getUserIdFromToken(token));
}

@Transactional(readOnly = true)
public List<RankingEntryResponse> getRanking(int page, int size) {
    return userRepository
            .findBySimulatedFalseOrderByCriticPointsDesc(PageRequest.of(page, size))
            .stream()
            .map(u -> new RankingEntryResponse(
                    u.getId(), u.getUsername(), u.getBio(),
                    u.getAvatarUrl(), u.getCriticPoints()))
            .toList();
}

public User uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + userId));
    if (user.getAvatarUrl() != null) {
        avatarStorageService.delete(user.getAvatarUrl());
    }
    String url = avatarStorageService.store(file);
    user.setAvatarUrl(url);
    return userRepository.save(user);
}
public User updateProfile(Long userId, String bio) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + userId));
    user.setBio(bio);
    return userRepository.save(user);
}

@Transactional(readOnly = true)
public List<RankingEntryResponse> searchUsers(String query, int page, int size) {
    String q = query == null ? "" : query.trim();
    if (q.length() < 2) {
        return List.of();
    }
    int safeSize = Math.min(Math.max(size, 1), 50);
    int safePage = Math.max(page, 0);
    return userRepository
            .findByUsernameContainingIgnoreCaseAndSimulatedFalseOrderByCriticPointsDescUsernameAsc(
                    q, PageRequest.of(safePage, safeSize))
            .stream()
            .map(u -> new RankingEntryResponse(
                    u.getId(), u.getUsername(), u.getBio(),
                    u.getAvatarUrl(), u.getCriticPoints()))
            .toList();
}
}