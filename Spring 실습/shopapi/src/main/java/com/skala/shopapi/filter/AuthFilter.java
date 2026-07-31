package com.skala.shopapi.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.shopapi.common.JwtTokenProvider;
import com.skala.shopapi.common.JwtTokenProvider.TokenClaims;
import com.skala.shopapi.common.Response;
import com.skala.shopapi.common.SessionHandler;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionHandler sessionHandler;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public AuthFilter(JwtTokenProvider jwtTokenProvider, SessionHandler sessionHandler) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionHandler = sessionHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (isPublic(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            TokenClaims claims = authenticate(request);
            if (claims == null) {
                writeError(response, HttpStatus.UNAUTHORIZED, "유효한 Bearer 토큰이 필요합니다.",
                        "NOT_AUTHENTICATED");
                return;
            }
            if (isAdminOnly(request) && !"ADMIN".equals(claims.role())) {
                writeError(response, HttpStatus.FORBIDDEN, "해당 API에 접근할 권한이 없습니다.",
                        "NOT_AUTHORIZED");
                return;
            }

            sessionHandler.setCurrentCustomer(claims.customerId(), claims.role());
            filterChain.doFilter(request, response);
        } finally {
            sessionHandler.clear();
        }
    }

    private TokenClaims authenticate(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return jwtTokenProvider.parseAndValidate(authorization.substring(7).trim());
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = normalizePath(request.getServletPath());
        String method = request.getMethod();
        return HttpMethod.OPTIONS.matches(method)
                || !path.startsWith("/api/")
                || (path.equals("/api/customers/login") && HttpMethod.POST.matches(method))
                || (path.equals("/api/customers") && HttpMethod.POST.matches(method))
                || (path.startsWith("/api/products") && HttpMethod.GET.matches(method));
    }

    private boolean isAdminOnly(HttpServletRequest request) {
        String path = normalizePath(request.getServletPath());
        String method = request.getMethod();
        return path.equals("/api/customers/list")
                || (path.equals("/api/customers") && HttpMethod.DELETE.matches(method))
                || (path.startsWith("/api/products") && !HttpMethod.GET.matches(method));
    }

    private String normalizePath(String path) {
        if (path != null && path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void writeError(
            HttpServletResponse response, HttpStatus status, String message, String error) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Response.builder()
                .success(false)
                .message(message)
                .errors(Map.of("error", error))
                .timestamp(LocalDateTime.now())
                .build());
    }
}
