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
import java.util.regex.Pattern;

/**
 * Filter that assigns a unique traceId to every incoming HTTP request.
 * <ul>
 *   <li>If the client sends a well-formed {@code X-Trace-Id} header, that value is reused.</li>
 *   <li>Otherwise a new UUID-derived id is generated.</li>
 * </ul>
 * The traceId is placed in SLF4J MDC so it appears in every log line and is
 * echoed back to the client as a response header.
 *
 * <p>Client-supplied values are strictly validated against {@link #TRACE_ID_PATTERN}
 * to prevent log injection (CR/LF, control characters) and HTTP response-splitting
 * via the reflected response header.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_HEADER = "X-Trace-Id";

    /** Alphanumeric + dash, 1-64 chars. Rejects CR/LF and any control chars. */
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String traceId = request.getHeader(TRACE_HEADER);
            if (traceId == null || !TRACE_ID_PATTERN.matcher(traceId).matches()) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }

            MDC.put(TRACE_ID_KEY, traceId);
            response.setHeader(TRACE_HEADER, traceId);

            filterChain.doFilter(request, response);

        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
