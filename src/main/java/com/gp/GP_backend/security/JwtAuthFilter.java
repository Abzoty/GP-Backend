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
 * Stateless JWT authentication filter — runs once per HTTP request.
 *
 * <h3>Filter flow</h3>
 * <ol>
 * <li>Extract the Bearer token from the {@code Authorization} header.</li>
 * <li>Parse the email (subject) from the token without touching the
 * database.</li>
 * <li>If the context is empty (not yet authenticated), load the full
 * {@link UserDetails}
 * from the database and validate the token's signature and expiry.</li>
 * <li>On success, populate the {@link SecurityContextHolder} so downstream
 * controllers can access the current user via
 * {@code @AuthenticationPrincipal}.</li>
 * <li>Always pass the request down the filter chain regardless of outcome —
 * unauthenticated requests are rejected by the security rules in
 * {@link com.gp.GP_backend.config.SecurityConfig}, not here.</li>
 * </ol>
 *
 * <p>
 * Extends {@link OncePerRequestFilter} to guarantee a single execution per
 * request,
 * even in async dispatch scenarios.
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

                // Only authenticate if there is no existing authentication in the context
                // (prevents re-processing on forwarded requests)
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    if (jwtTokenProvider.validateToken(token, userDetails)) {
                        // Build an authenticated token and attach request metadata (IP, session)
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("Authenticated user '{}' via JWT", email);
                    }
                }
            } catch (Exception e) {
                // Log the reason but do NOT short-circuit — let Spring Security handle the 401
                log.debug("JWT authentication failed: {}", e.getMessage());
            }
        }

        // Always continue the chain; unauthenticated requests hit the security rules
        // next
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw JWT string from the {@code Authorization: Bearer <token>}
     * header.
     *
     * @return the token string, or {@code null} if the header is absent or
     *         malformed
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7); // skip "Bearer "
        }
        return null;
    }
}