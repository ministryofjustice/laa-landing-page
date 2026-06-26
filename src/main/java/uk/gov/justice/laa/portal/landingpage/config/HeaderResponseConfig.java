package uk.gov.justice.laa.portal.landingpage.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HeaderResponseConfig implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletResponse res = (HttpServletResponse) response;

        res.setHeader("X-Powered-By", "");
        res.setHeader("Server", "");

        chain.doFilter(request, response);
    }
}
