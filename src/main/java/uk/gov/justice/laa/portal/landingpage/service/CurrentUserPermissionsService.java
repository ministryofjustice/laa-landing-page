package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.justice.laa.portal.landingpage.client.CurrentUserPermissionsClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserPermissions;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class CurrentUserPermissionsService {

    // Spring Session JDBC persists this attribute with the existing security session.
    private static final String SESSION_STATE = CurrentUserPermissionsService.class.getName() + ".state";
    private static final String RELOGIN_ATTEMPT = CurrentUserPermissionsService.class.getName() + ".reloginAttempt";

    private final OAuth2AuthorizedClientRepository authorizedClients;
    private final OboTokenService oboTokens;
    private final CurrentUserPermissionsClient permissionsClient;

    public CurrentUserPermissionsService(OAuth2AuthorizedClientRepository authorizedClients, OboTokenService oboTokens,
                                         CurrentUserPermissionsClient permissionsClient) {
        this.authorizedClients = authorizedClients;
        this.oboTokens = oboTokens;
        this.permissionsClient = permissionsClient;
    }

    public static void clearPermissions(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_STATE);
        }
    }

    static Optional<CurrentUserPermissions> cachedPermissions(Authentication authentication) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)
                || !(authentication instanceof OAuth2AuthenticationToken oauth) || !oauth.isAuthenticated()) {
            return Optional.empty();
        }
        HttpSession session = attributes.getRequest().getSession(false);
        return Optional.ofNullable(session == null ? null : sessionState(session, identityOf(oauth)))
                .map(SessionState::permissions);
    }

    public CurrentUserPermissions load(HttpServletRequest request, Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (!(authentication instanceof OAuth2AuthenticationToken oauth) || !oauth.isAuthenticated() || session == null) {
            clearPermissions(session);
            throw failure(401);
        }
        Identity identity = identityOf(oauth);
        if (!identity.equals(session.getAttribute(RELOGIN_ATTEMPT))) {
            session.removeAttribute(RELOGIN_ATTEMPT);
        }
        String sessionId = session.getId();
        SessionState state = sessionState(session, identity);
        if (state != null) {
            if (state.permissions() == null) {
                throw failure(403);
            }
            return state.permissions();
        }
        clearPermissions(session);

        OAuth2AuthorizedClient client = authorizedClients.loadAuthorizedClient(identity.registration(), oauth, request);
        if (client == null || client.getAccessToken() == null
                || (client.getAccessToken().getExpiresAt() != null
                    && !client.getAccessToken().getExpiresAt().isAfter(Instant.now().plusSeconds(30)))) {
            throw reauthenticate(session, identity);
        }

        try {
            String token = oboTokens.acquireOboToken(client.getAccessToken().getTokenValue(),
                    oauth.getPrincipal().getAttribute("oid"));
            CurrentUserPermissions permissions = permissionsClient.load(token);
            session.setAttribute(SESSION_STATE, new SessionState(sessionId, identity, permissions));
            session.removeAttribute(RELOGIN_ATTEMPT);
            return permissions;
        } catch (UserDataApiClientException e) {
            throw handleStatus(e.getStatusCode(), session, sessionId, identity);
        } catch (RestClientResponseException e) {
            // The existing OBO flow treats a rejected assertion (including invalid_grant) as a login failure.
            int status = e.getStatusCode().value();
            throw handleStatus(status == 400 ? 401 : status, session, sessionId, identity);
        } catch (RestClientException e) {
            throw failure(503);
        }
    }

    private static SessionState sessionState(HttpSession session, Identity identity) {
        if (session.getAttribute(SESSION_STATE) instanceof SessionState state
                && state.sessionId().equals(session.getId()) && state.identity().equals(identity)) {
            return state;
        }
        return null;
    }

    private static Identity identityOf(OAuth2AuthenticationToken oauth) {
        return new Identity(oauth.getAuthorizedClientRegistrationId(), oauth.getName(),
                Objects.toString(oauth.getPrincipal().getAttributes().get("iss"), null),
                oauth.getPrincipal().getAttribute("sub"));
    }

    private RuntimeException handleStatus(int status, HttpSession session, String sessionId, Identity identity) {
        if (status == 401) {
            return reauthenticate(session, identity);
        }
        if (status == 403) {
            session.setAttribute(SESSION_STATE, new SessionState(sessionId, identity, null));
            session.removeAttribute(RELOGIN_ATTEMPT);
            return failure(403);
        }
        return failure(503);
    }

    private RuntimeException reauthenticate(HttpSession session, Identity identity) {
        // Session-fixation protection migrates attributes on login; keep this guard through that redirect.
        if (identity.equals(session.getAttribute(RELOGIN_ATTEMPT))) {
            return failure(401);
        }
        session.setAttribute(RELOGIN_ATTEMPT, identity);
        return new ClientAuthorizationRequiredException(identity.registration());
    }

    private UserDataApiClientException failure(int status) {
        return new UserDataApiClientException("Unable to load current-user permissions", status);
    }

    private record Identity(String registration, String principal, String issuer, String subject) implements Serializable {
    }

    private record SessionState(String sessionId, Identity identity, CurrentUserPermissions permissions) implements Serializable {
    }
}
