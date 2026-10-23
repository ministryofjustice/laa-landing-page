package uk.gov.justice.laa.portal.landingpage.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class LocaleDiagnosticsFilter extends OncePerRequestFilter {

    private static final int HEADER_SAMPLE_LIMIT = 1024;
    private static final long LOG_INTERVAL_MILLIS = 60_000;

    private final Clock clock;
    private final AtomicLong nextLogTime = new AtomicLong();

    public LocaleDiagnosticsFilter() {
        this(Clock.systemUTC());
    }

    LocaleDiagnosticsFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Enumeration<String> headers = request.getHeaders(HttpHeaders.ACCEPT_LANGUAGE);
        int entries = 0;
        long length = 0;
        StringBuilder sample = new StringBuilder();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            length += header.length();
            boolean inEntry = false;
            for (int i = 0; i < header.length(); i++) {
                char character = header.charAt(i);
                if (character == ',') {
                    inEntry = false;
                } else if (!Character.isWhitespace(character) && !inEntry) {
                    entries++;
                    inEntry = true;
                }
            }
            if (!sample.isEmpty() && sample.length() < HEADER_SAMPLE_LIMIT) {
                sample.append(',');
            }
            int remaining = HEADER_SAMPLE_LIMIT - sample.length();
            sample.append(header, 0, Math.min(header.length(), remaining));
        }

        // Inspect raw headers before RequestContextFilter calls getLocale(), which can throw.
        // This count is approximate: Tomcat decides which entries are valid locales.
        if (entries > ErrorPageConfig.MAX_LOCALE_COUNT && acquireLogSlot()) {
            log.warn("Accept-Language exceeds locale limit: limit={}, approximateEntries={}, headerLength={}, "
                            + "acceptLanguage={}, method={}, path={}, userAgent={}, requestId={}, traceparent={}. "
                            + "Diagnostics sampled at most once per minute per application instance.",
                    ErrorPageConfig.MAX_LOCALE_COUNT, entries, length,
                    safe(sample.toString(), HEADER_SAMPLE_LIMIT) + (length > sample.length() ? "[truncated]" : ""),
                    safe(request.getMethod(), 32), safe(request.getRequestURI(), 256),
                    safe(request.getHeader(HttpHeaders.USER_AGENT), 256),
                    safe(request.getHeader("X-Request-ID"), 128),
                    safe(request.getHeader("traceparent"), 128));
        }
        filterChain.doFilter(request, response);
    }

    private boolean acquireLogSlot() {
        long now = clock.millis();
        long next = nextLogTime.get();
        return now >= next && nextLogTime.compareAndSet(next, now + LOG_INTERVAL_MILLIS);
    }

    private static String safe(String value, int limit) {
        if (value == null) {
            return "-";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(value.length(), limit); i++) {
            char character = value.charAt(i);
            result.append(character >= ' ' && character <= '~' ? character : '?');
        }
        if (value.length() > limit) {
            result.append("[truncated]");
        }
        return result.toString();
    }
}
