package com.mishraachandan.booking_system.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that assigns a unique traceId to every incoming HTTP request.
 * - If the client sends an "X-Trace-Id" header, that value is used.
 * - Otherwise a new UUID is generated.
 * The traceId is placed in SLF4J MDC so it appears in every log line,
 * and is also returned to the client as a response header.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Use client-provided traceId or generate a new one
            String traceId = request.getHeader(TRACE_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }

            // Put into MDC so all loggers in this thread include it
            MDC.put(TRACE_ID_KEY, traceId);

            // Return traceId in response header so the client can reference it
            response.setHeader(TRACE_HEADER, traceId);

            filterChain.doFilter(request, response);

        } finally {
            // Always clean up MDC to prevent leaking to other requests
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
