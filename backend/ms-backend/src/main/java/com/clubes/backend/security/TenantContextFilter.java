package com.clubes.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro que resuelve y expone el {@link TenantContext} por cada request autenticado con JWT,
 * limpiando el {@link TenantContextHolder} al finalizar.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantContextResolver tenantContextResolver;

    public TenantContextFilter(TenantContextResolver tenantContextResolver) {
        this.tenantContextResolver = tenantContextResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth && jwtAuth.isAuthenticated()) {
                TenantContext context = tenantContextResolver.resolve(jwtAuth);
                TenantContextHolder.set(context);
            }
            filterChain.doFilter(request, response);
        } catch (AccessDeniedException ex) {
            response.sendError(HttpStatus.FORBIDDEN.value(), ex.getMessage());
        } finally {
            TenantContextHolder.clear();
        }
    }
}
