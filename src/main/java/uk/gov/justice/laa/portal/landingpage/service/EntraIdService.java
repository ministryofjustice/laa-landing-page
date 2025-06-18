package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for interacting with Microsoft Entra ID (Azure AD).
 * This is a simplified implementation for demonstration purposes.
 * In a production environment, you would use the Microsoft Graph SDK.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntraIdService {

    private final GraphApiService graphApiService;

    /**
     * Get user details from Entra ID using the provided access token.
     * @param accessToken The access token for authentication
     * @return User details if found, null otherwise
     */
    public User getUserByPrincipalName(String accessToken) {
        return graphApiService.getUserProfile(accessToken);
    }

    /**
     * Get user's assigned application roles from Entra ID.
     * @param accessToken The access token for authentication
     * @return List of display names of the application roles assigned to the user
     */
    public List<String> getUserAssignedAppRoles(String accessToken) {
        return graphApiService.getUserAssignedApps(accessToken)
                .stream()
                .map(AppRole::getDisplayName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Validate an access token with Entra ID.
     * @param token The access token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            User user = graphApiService.getUserProfile(token);
            return user != null && user.getId() != null && !user.getId().trim().isEmpty();
        } catch (Exception ex) {
            log.error("Error validating token: {}", ex.getMessage(), ex);
            return false;
        }
    }
}