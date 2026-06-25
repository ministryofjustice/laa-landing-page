package uk.gov.justice.laa.portal.landingpage.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HeaderResponseConfigTest {

    private HeaderResponseConfig filter;

    @Mock
    private ServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new HeaderResponseConfig();
    }

    @Test
    void doFilter_setsHeaders_andContinuesChain() throws Exception {
        filter.doFilter(request, response, chain);
        verify(response).setHeader("X-Powered-By", "");
        verify(response).setHeader("Server", "");
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_callsChainExactlyOnce() throws Exception {
        filter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilter_setsHeadersExactlyOnce() throws Exception {
        filter.doFilter(request, response, chain);
        verify(response, times(1)).setHeader("X-Powered-By", "");
        verify(response, times(1)).setHeader("Server", "");
    }
}
