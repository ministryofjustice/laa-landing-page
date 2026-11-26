package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import uk.gov.justice.laa.datauserapi.contracts.response.UserProfileDetailView;
import uk.gov.justice.laa.portal.landingpage.client.RoleAssignmentContract;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.client.UserProfileQuery;
import uk.gov.justice.laa.portal.landingpage.client.UserProfileQueryPage;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchResultsDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileSilasStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileQueryService {

    private final UserDataApiClient userDataApiClient;
    private final OAuth2AuthorizedClientRepository authorizedClients;
    private final ObjectProvider<HttpServletRequest> requests;

    public PaginatedUsers search(UserProfileQuery query) {
        var result = call((token, oid, cid) -> userDataApiClient.queryUserProfiles(token, oid, cid, query));
        if (result == null || result.page() == null || result.items() == null
                || result.page().pageSize() < 1 || result.page().totalPages() < 0 || result.page().totalElements() < 0) {
            throw unavailable();
        }
        PaginatedUsers page = new PaginatedUsers();
        page.setUsers(result.items().stream().map(this::toSearchResult).toList());
        page.setTotalUsers(result.page().totalElements());
        page.setTotalPages(result.page().totalPages());
        return page;
    }

    public UserProfileDetailView getProfile(UUID profileId) {
        var profile = call((token, oid, cid) -> userDataApiClient.queryUserProfileById(token, oid, cid, profileId));
        if (profile == null || !profileId.equals(profile.userProfileId()) || profile.fullName() == null
                || profile.email() == null || profile.userType() == null || profile.profileStatus() == null
                || profile.roles() == null || profile.roles().stream().anyMatch(Objects::isNull)
                || (profile.offices() != null && profile.offices().stream().anyMatch(Objects::isNull))) {
            throw unavailable();
        }
        return profile;
    }

    public List<RoleAssignmentContract.Role> assignableRoles(UUID profileId) {
        var result = call((token, oid, cid) -> userDataApiClient.queryAssignableRoles(token, oid, cid, profileId));
        if (result == null || !validRoles(result.items())) {
            throw unavailable();
        }
        return result.items();
    }

    public void assignRoles(UUID profileId, List<UUID> roleIds) {
        var command = new RoleAssignmentContract.Command(profileId, roleIds);
        var result = call((token, oid, cid) -> userDataApiClient.assignAppRoles(token, oid, cid, command));
        if (result == null || !profileId.equals(result.userProfileId()) || !validRoles(result.assignedRoles())
                || !new HashSet<>(result.assignedRoles().stream().map(RoleAssignmentContract.Role::appRoleId).toList())
                        .containsAll(roleIds)) {
            throw unavailable();
        }
    }

    private boolean validRoles(List<RoleAssignmentContract.Role> roles) {
        var ids = new HashSet<UUID>();
        return roles != null && roles.stream().allMatch(role -> role != null && role.appRoleId() != null
                && role.appRoleName() != null && !role.appRoleName().isBlank() && ids.add(role.appRoleId()));
    }

    private UserSearchResultsDto toSearchResult(UserProfileQueryPage.Profile profile) {
        if (profile == null || profile.userProfileId() == null || profile.userType() == null
                || profile.fullName() == null || profile.email() == null || profile.profileStatus() == null) {
            throw unavailable();
        }
        return new UserSearchResultsDto(profile.userProfileId(), Boolean.TRUE.equals(profile.activeProfile()),
                UserType.valueOf(profile.userType().name()), null, null, UserProfileSilasStatus.valueOf(profile.profileStatus().name()),
                Boolean.TRUE.equals(profile.multiFirmUser()), null, null,
                profile.fullName(), profile.email(), null, profile.firmName(), null, false, profile.hasAppRoles());
    }

    private <T> T call(UserDataApiSession.ApiCall<T> query) {
        try {
            return UserDataApiSession.call(authorizedClients, requests.getObject(), query);
        } catch (RestClientException e) {
            throw unavailable();
        }
    }

    private UserDataApiClientException unavailable() {
        return new UserDataApiClientException("User profile operation unavailable", 503);
    }
}
