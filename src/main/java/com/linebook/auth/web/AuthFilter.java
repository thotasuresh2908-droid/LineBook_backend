package com.linebook.auth.web;

import com.linebook.auth.service.AuthService;
import com.linebook.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the {@code Authorization: Bearer <token>} header, verifies it and makes the caller
 * available through {@link CurrentUserHolder} for the rest of the request.
 *
 * <p>Everything under {@code /api} needs a valid token except the paths in {@link #PUBLIC_PREFIXES}
 * (login, health, the API docs). A missing or bad token gets a plain 401 with a JSON body, which is
 * what the front end watches for to send the person back to the login page.
 *
 * <p>Browser pre-flight requests (OPTIONS) pass straight through: CORS is handled by the filter
 * registered before this one.
 */
public class AuthFilter extends OncePerRequestFilter {

    /** Open to everyone; the rest of /api needs a token. */
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth/login",
            "/api/info",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui");

    private final TokenService tokens;
    private final AuthService auth;

    public AuthFilter(TokenService tokens, AuthService auth) {
        this.tokens = tokens;
        this.auth = auth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Anything outside /api (the static front end, the root) is left alone.
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            reject(response, "Sign in to continue");
            return;
        }

        try {
            TokenService.Claims claims = tokens.verify(header.substring(7).trim());
            Optional<CurrentUser> user = auth.loadActive(claims.userId());
            if (user.isEmpty()) {
                reject(response, "This login is no longer active; please sign in again");
                return;
            }
            CurrentUserHolder.set(user.get());
            chain.doFilter(request, response);
        } catch (com.linebook.auth.exception.UnauthorizedException ex) {
            reject(response, ex.getMessage());
        } finally {
            CurrentUserHolder.clear();
        }
    }

    private static boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private static void reject(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":401,\"detail\":\"" + escape(message) + "\"}");
    }

    private static String escape(String value) {
        return value == null ? "Sign in to continue" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
