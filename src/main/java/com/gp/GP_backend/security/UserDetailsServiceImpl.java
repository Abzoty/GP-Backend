package com.gp.GP_backend.security;

import com.gp.GP_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Connects Spring Security's authentication mechanism to the application's
 * {@link UserRepository}.
 *
 * <p>
 * Spring Security calls {@link #loadUserByUsername(String)} during the login
 * flow
 * to fetch the persisted user record. Because
 * {@link com.gp.GP_backend.domain.user.entity.User}
 * implements {@link UserDetails} directly, this adapter simply delegates to the
 * repository
 * with no extra mapping.
 *
 * <p>
 * The "username" in Spring Security's terminology is the email address in this
 * application.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by their email address.
     *
     * @param email the login email submitted in the request
     * @return the matching {@link com.gp.GP_backend.domain.user.entity.User} entity
     * @throws UsernameNotFoundException if no user with the given email exists
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email));
    }
}