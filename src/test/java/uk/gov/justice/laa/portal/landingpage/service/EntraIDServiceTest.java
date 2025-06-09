package uk.gov.justice.laa.portal.landingpage.service;

import com.azure.identity.ClientSecretCredential;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntraIDServiceTest {

    @Mock
    private GraphApiService graphApiService;

    @Mock
    private ClientSecretCredential clientSecretCredential;

    @InjectMocks
    private EntraIdService entraIDService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(graphApiService, clientSecretCredential);
    }

    @Test
    void getUserByPrincipalName_WhenUserExists_ReturnsUser() {
        // Arrange
        String userPrincipalName = "test.user@example.com";
        // Create and configure expected user with proper setter methods
        User expectedUser = new User();
        expectedUser.setUserPrincipalName(userPrincipalName); // This is a public field in the SDK
        expectedUser.setDisplayName("Test User"); ; // This is a public field in the SDK
        expectedUser.setId("12345"); // This is a public field in the SDK

        when(graphApiService.getUserProfile(anyString())).thenReturn(expectedUser);

        // Act
        User result = entraIDService.getUserByPrincipalName(userPrincipalName).join();

        // Assert
        assertNotNull(result);
        assertEquals(userPrincipalName, result.getUserPrincipalName());
        assertEquals("Test User", result.getDisplayName());
        assertEquals("12345", result.getId());
        verify(graphApiService).getUserProfile(anyString());
    }

    @Test
    void getUserGroupMemberships_WhenUserHasGroups_ReturnsGroupIds() {
        // Arrange
        String userId = "user-123";
        // Create test roles with proper setter methods
        AppRole role1 = new AppRole();
        role1.setDisplayName("Admin"); // This is a public field in the SDK
        AppRole role2 = new AppRole();
        role2.setDisplayName("User"); // This is a public field in the SDK

        when(graphApiService.getUserAssignedApps(anyString()))
            .thenReturn(List.of(role1, role2));

        // Act
        List<String> result = entraIDService.getUserGroupMemberships(userId).join();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Admin"));
        assertTrue(result.contains("User"));
        verify(graphApiService).getUserAssignedApps(anyString());
    }

    @Test
    void validateToken_WithValidToken_ReturnsTrue() {
        // Arrange
        String validToken = "valid.token.here";
        // Create test user with proper setter methods
        User user = new User();
        user.setId("12345"); // This is a public field in the SDK

        when(graphApiService.getUserProfile(validToken)).thenReturn(user);

        // Act
        boolean result = entraIDService.validateToken(validToken);

        // Assert
        assertTrue(result);
        verify(graphApiService).getUserProfile(validToken);
    }

    @Test
    void validateToken_WithInvalidToken_ReturnsFalse() {
        // Arrange
        String invalidToken = "invalid.token.here";
        when(graphApiService.getUserProfile(invalidToken)).thenThrow(new RuntimeException("Invalid token"));

        // Act
        boolean result = entraIDService.validateToken(invalidToken);

        // Assert
        assertFalse(result);
        verify(graphApiService).getUserProfile(invalidToken);
    }
}