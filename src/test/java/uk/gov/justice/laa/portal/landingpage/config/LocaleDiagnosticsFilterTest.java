package uk.gov.justice.laa.portal.landingpage.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.RequestContextFilter;

import java.time.Clock;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocaleDiagnosticsFilterTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(LocaleDiagnosticsFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final AtomicLong now = new AtomicLong(1_000);
    private LocaleDiagnosticsFilter filter;

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
        Clock clock = mock(Clock.class);
        when(clock.millis()).thenAnswer(invocation -> now.get());
        filter = new LocaleDiagnosticsFilter(clock);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void allowsTwentyLocalesWithoutLogging() throws Exception {
        for (int count : new int[] {0, 10, 20}) {
            MockHttpServletRequest request = request(count);
            filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });
        }
        assertTrue(appender.list.isEmpty());
    }

    @Test
    void logsBeforeLocaleParsingAndPreservesFailure() {
        MockHttpServletRequest request = request(21);
        request.setRequestURI("/login");
        request.setQueryString("secret=must-not-log");
        request.addHeader("User-Agent", "Browser");
        request.addHeader("Cookie", "private-cookie");
        request.addHeader("Authorization", "private-token");
        request.addHeader("X-Request-ID", "request-123");
        ServletException failure = new ServletException("locale parsing failed");
        FilterChain chain = (req, res) -> {
            assertEquals(1, appender.list.size());
            throw failure;
        };

        assertEquals(failure, assertThrows(ServletException.class,
                () -> filter.doFilter(request, new MockHttpServletResponse(), chain)));
        String message = appender.list.getFirst().getFormattedMessage();
        assertTrue(message.contains("limit=20, approximateEntries=21"));
        assertTrue(message.contains("path=/login"));
        assertTrue(message.contains("userAgent=Browser"));
        assertTrue(message.contains("requestId=request-123"));
        assertFalse(message.contains("must-not-log"));
        assertFalse(message.contains("private-cookie"));
        assertFalse(message.contains("private-token"));
    }

    @Test
    void countsMultipleHeadersAndIgnoresEmptyEntries() throws Exception {
        MockHttpServletRequest request = request(20);
        request.addHeader("Accept-Language", " , , fr;q=0.8, ");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });
        assertTrue(appender.list.getFirst().getFormattedMessage().contains("approximateEntries=21"));
    }

    @Test
    void sanitisesAndTruncatesUntrustedValues() throws Exception {
        MockHttpServletRequest request = request(21);
        request.addHeader("Accept-Language", "x".repeat(2000));
        request.addHeader("User-Agent", "browser\r\nforged\u2028" + "a".repeat(1000));
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });
        String message = appender.list.getFirst().getFormattedMessage();
        assertFalse(message.contains("\r"));
        assertFalse(message.contains("\n"));
        assertFalse(message.contains("\u2028"));
        assertTrue(message.contains("browser??forged?"));
        assertTrue(message.contains("[truncated]"));
        assertTrue(message.length() < 2200);
    }

    @Test
    void rateLimitsWarningsWithoutBlockingRequests() throws Exception {
        AtomicLong calls = new AtomicLong();
        FilterChain chain = (req, res) -> calls.incrementAndGet();
        filter.doFilter(request(21), new MockHttpServletResponse(), chain);
        filter.doFilter(request(21), new MockHttpServletResponse(), chain);
        assertEquals(1, appender.list.size());
        now.addAndGet(60_000);
        filter.doFilter(request(21), new MockHttpServletResponse(), chain);
        assertEquals(2, appender.list.size());
        assertEquals(3, calls.get());
    }

    @Test
    void runsBeforeSpringRequestContextFilter() throws Exception {
        int order = LocaleDiagnosticsFilter.class.getAnnotation(Order.class).value();
        assertTrue(order < Ordered.HIGHEST_PRECEDENCE + 100);
        assertTrue(order < -105); // Spring Boot's default RequestContextFilter order.
        MockHttpServletRequest request = request(21);
        RequestContextFilter contextFilter = new RequestContextFilter();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertEquals(1, appender.list.size());
            contextFilter.doFilter(req, res, (innerRequest, innerResponse) -> { });
        });
    }

    private MockHttpServletRequest request(int count) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        if (count > 0) {
            request.addHeader("Accept-Language", String.join(",", Collections.nCopies(count, "en-GB")));
        }
        return request;
    }
}
