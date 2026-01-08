package uk.gov.justice.laa.portal.landingpage.environment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessGuardTest {

    @Mock
    private Environment env;
    @Mock
    private Authentication authentication;
    @Mock
    private LoginService loginService;
    @Mock
    private UserService userService;

    @InjectMocks
    private AccessGuard accessGuard;

    @BeforeEach
    public void beforeEach() {
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev"});
    }

    @Test
    void isProdEnv() {
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});
        boolean result = accessGuard.isProdEnv();
        assertTrue(result);
    }

    @Test
    void isNonProdEnv() {
        boolean result = accessGuard.isProdEnv();
        assertFalse(result);
    }

    @Test
    void canDelegateInNonProdWithInternalUser() {
        EntraUser entraUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(userService.isInternal(entraUser.getId())).thenReturn(true);
        boolean result = accessGuard.canDelegateInNonProd(authentication);
        assertTrue(result);
    }

    @Test
    void canDelegateInNonProdWithNonInternalUser() {
        EntraUser entraUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(userService.isInternal(entraUser.getId())).thenReturn(false);
        boolean result = accessGuard.canDelegateInNonProd(authentication);
        assertFalse(result);
    }

    @Test
    void canDelegateInProdWithNonInternalUser() {
        EntraUser entraUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(userService.isInternal(entraUser.getId())).thenReturn(false);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});
        boolean result = accessGuard.canDelegateInNonProd(authentication);
        assertFalse(result);
    }

}