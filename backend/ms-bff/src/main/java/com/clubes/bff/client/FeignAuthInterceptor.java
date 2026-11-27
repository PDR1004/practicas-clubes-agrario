package com.clubes.bff.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void apply(RequestTemplate template) {
        if (template.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
            return;
        }

        String authorizationHeader = resolveFromCurrentRequest();
        if (!StringUtils.hasText(authorizationHeader)) {
            authorizationHeader = resolveFromSecurityContext();
        }

        if (StringUtils.hasText(authorizationHeader)) {
            template.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
    }

    private String resolveFromCurrentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(header)) {
                return header;
            }
        }
        return null;
    }

    private String resolveFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth && jwtAuth.getToken() != null) {
            String tokenValue = jwtAuth.getToken().getTokenValue();
            if (StringUtils.hasText(tokenValue)) {
                return BEARER_PREFIX + tokenValue;
            }
        }
        return null;
    }
}
