package uk.gov.justice.laa.portal.landingpage.service;

import com.azure.identity.ClientSecretCredential;
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
    private final ClientSecretCredential clientSecretCredential;

    /**
     * Get user details from Entra ID by user principal name (email).
     * @param userPrincipalName The user's principal name (email)
     * @return User details if found
     */
    public CompletableFuture<User> getUserByPrincipalName(String userPrincipalName) {
        return CompletableFuture.completedFuture(graphApiService.getUserProfile(""));
    }

    /**
     * Get user's group memberships from Entra ID.
     * @param userId The user's ID in Entra ID
     * @return List of group IDs the user is a member of
     */
    public CompletableFuture<List<String>> getUserGroupMemberships(String userId) {
        return CompletableFuture.completedFuture(
            graphApiService.getUserAssignedApps("").stream()
                .map(role -> role.getDisplayName()) // This is a public field in the SDK
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
        );
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