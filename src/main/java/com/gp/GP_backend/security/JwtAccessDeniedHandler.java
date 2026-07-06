package com.gp.GP_backend.security;

import com.gp.GP_backend.shared.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    // creates a custom ApiException with HTTP 403 FORBIDDEN and passes it to
    // Spring’s exception resolver.

    public JwtAccessDeniedHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handle(HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) {

        resolver.resolveException(request, response, null,
                new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to access this resource."));
    }
}