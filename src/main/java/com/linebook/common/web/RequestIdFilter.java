package com.linebook.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Enumeration;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an X-Request-Id: the caller's own (when it is a safe token) or a new UUID. The id is put on the
 * response, in the log context (MDC "requestId") so one id
 * follows a request through the logs.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String id = incoming != null && SAFE.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();

        MDC.put("requestId", id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(new WithRequestId(request, id), response);
        } finally {
            MDC.remove("requestId");
        }
    }

    /** Presents the (possibly newly generated) id as the request's X-Request-Id header. */
    private static final class WithRequestId extends HttpServletRequestWrapper {

        private final String id;

        WithRequestId(HttpServletRequest request, String id) {
            super(request);
            this.id = id;
        }

        @Override
        public String getHeader(String name) {
            return HEADER.equalsIgnoreCase(name) ? id : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return HEADER.equalsIgnoreCase(name) ? Collections.enumeration(Set.of(id)) : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
            names.removeIf(HEADER::equalsIgnoreCase);
            names.add(HEADER);
            return Collections.enumeration(names);
        }
    }
}
