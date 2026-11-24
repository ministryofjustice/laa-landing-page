package uk.gov.justice.laa.portal.landingpage.client;

import uk.gov.justice.laa.datauserapi.contracts.domain.UserProfileSilasStatus;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserType;
import uk.gov.justice.laa.datauserapi.contracts.dto.PageMetadata;

import java.util.List;
import java.util.UUID;

/** Search projection uses the SiLAS status defined by the profile-search OpenAPI schema. */
public record UserProfileQueryPage(List<Profile> items, PageMetadata page) {

    public record Profile(UUID userProfileId, UserType userType, String fullName, String email,
                          String firmName, Boolean multiFirmUser, UserProfileSilasStatus profileStatus,
                          Boolean activeProfile, boolean hasAppRoles) {
    }
}
