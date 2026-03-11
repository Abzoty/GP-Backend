package com.gp.GP_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Per-request JWT authentication filter.
 *
 * <p>
 * Runs once per request (guaranteed by {@link OncePerRequestFilter}).
 * If a valid Bearer token is found, authenticates the user by populating
 * the {@link SecurityContextHolder} — which makes the user available via
 * {@code @AuthenticationPrincipal} in controllers.
 *
 * <p>
 * The filter never throws — invalid or missing tokens simply result in
 * no authentication being set, letting Spring Security apply its own 401
 * response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (StringUtils.hasText(token)) {
            try {
                String email = jwtTokenProvider.extractEmail(token);

                // Only authenticate if not already authenticated in this request
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    if (jwtTokenProvider.validateToken(token, userDetails)) {
                        // Build the Spring Security authentication object
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                        // Attach request metadata (IP, session ID) for audit logging
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("Authenticated user '{}' via JWT", email);
                    }
                }
            } catch (Exception e) {
                // Log and continue — SecurityContext remains empty → 401 returned by Spring
                log.debug("JWT processing failed for request to {}: {}", request.getRequestURI(), e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw token from the {@code Authorization: Bearer <token>} header.
     * Returns null if the header is absent or not in Bearer format.
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        log.info("Authorization header: {}", header);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
