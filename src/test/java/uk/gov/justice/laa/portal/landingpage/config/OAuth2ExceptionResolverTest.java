package uk.gov.justice.laa.portal.landingpage.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.web.servlet.ModelAndView;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class OAuth2ExceptionResolverTest {

    private OAuth2ExceptionResolver resolver;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        resolver = new OAuth2ExceptionResolver();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    void resolveException_withClientAuthorizationRequiredException_redirectsToOAuth2Authorization() {
        // Given
        ClientAuthorizationRequiredException exception = 
                new ClientAuthorizationRequiredException("azure");
        
        // When
        ModelAndView mav = resolver.resolveException(request, response, null, exception);
        
        // Then
        assertThat(mav).isNotNull();
        assertThat(mav.getViewName()).isEqualTo("redirect:/oauth2/authorization/azure");
    }

    @Test
    void resolveException_withOtherException_returnsNull() {
        // Given
        Exception exception = new RuntimeException("Some other error");
        
        // When
        ModelAndView mav = resolver.resolveException(request, response, null, exception);
        
        // Then
        assertThat(mav).isNull();
    }

    @Test
    void resolveException_withDifferentClientRegistrationId_redirectsCorrectly() {
        // Given
        ClientAuthorizationRequiredException exception = 
                new ClientAuthorizationRequiredException("github");
        
        // When
        ModelAndView mav = resolver.resolveException(request, response, null, exception);
        
        // Then
        assertThat(mav).isNotNull();
        assertThat(mav.getViewName()).isEqualTo("redirect:/oauth2/authorization/github");
    }
}
