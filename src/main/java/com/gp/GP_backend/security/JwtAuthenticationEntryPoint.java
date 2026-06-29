package com.gp.GP_backend.security;

import com.gp.GP_backend.shared.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) {

        // If the JwtAuthFilter caught an error (like expired token), throw that
        // specific error.
        Exception jwtException = (Exception) request.getAttribute("jwt_exception");
        if (jwtException != null) {
            resolver.resolveException(request, response, null, jwtException);
            return;
        }

        // Otherwise, it is a request lacking a token entirely.
        resolver.resolveException(request, response, null,
                new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required to access this resource."));
    }
}