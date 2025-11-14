package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.io.Serializable;
import java.util.UUID;

public record UserSearchResultsDto(UUID id, boolean activeProfile, UserType userType, UUID legacyUserId,
                                   UserProfileStatus userProfileStatus, boolean multiFirmUser, String firstName,
                                   String lastName, String fullName, String email, UserStatus userStatus,
                                   String firmName) implements Serializable {}
