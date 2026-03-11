package com.gp.GP_backend.security;

import com.gp.GP_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Bridges Spring Security's authentication mechanism with our
 * {@link UserRepository}.
 *
 * <p>
 * Spring Security calls {@link #loadUserByUsername} during the
 * {@code AuthenticationManager.authenticate()} flow (login). The returned
 * {@link com.gp.GP_backend.domain.user.entity.User} implements
 * {@link UserDetails}, so no adapter wrapper is needed.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by their email address (used as the Spring Security "username").
     *
     * @throws UsernameNotFoundException if no user exists with the given email.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email));
    }
}
