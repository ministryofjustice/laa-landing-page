package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

class AuditorAwareImplTest {

    @Test
    void getCurrentAuditor_currentUser() {
        OAuth2AuthenticationToken authentication = Mockito.mock(OAuth2AuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        OAuth2User principal = Mockito.mock(OAuth2User.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        Mockito.when(authentication.getPrincipal()).thenReturn(principal);
        Mockito.when(principal.getAttribute(anyString())).thenReturn("John Doe");
        SecurityContextHolder.setContext(securityContext);
        AuditorAwareImpl auditorAware = new AuditorAwareImpl();
        assertThat(auditorAware.getCurrentAuditor()).isPresent();
        assertThat(auditorAware.getCurrentAuditor().get()).isEqualTo("John Doe");
    }

    @Test
    void getCurrentAuditor_system() {
        OAuth2AuthenticationToken authentication = Mockito.mock(OAuth2AuthenticationToken.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        OAuth2User principal = Mockito.mock(OAuth2User.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        Mockito.when(authentication.getPrincipal()).thenReturn(principal);
        Mockito.when(principal.getAttribute(anyString())).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);
        AuditorAwareImpl auditorAware = new AuditorAwareImpl();
        assertThat(auditorAware.getCurrentAuditor()).isPresent();
        assertThat(auditorAware.getCurrentAuditor().get()).isEqualTo("System");
    }
}