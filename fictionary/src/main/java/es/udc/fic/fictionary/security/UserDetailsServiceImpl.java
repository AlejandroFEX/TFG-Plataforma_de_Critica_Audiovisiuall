package es.udc.fic.fictionary.security;

import es.udc.fic.fictionary.user.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        es.udc.fic.fictionary.user.User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con id: " + userId));

        return User.builder()
                .username(String.valueOf(user.getId()))
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
    }
}