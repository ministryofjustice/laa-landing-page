package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.InvitationStatus;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileSilasStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceOfficeAccessStatusTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void calculateSilasStatusForUserProfile_returnsNoAccessWhenExternalUserHasNoOffices() {
        UserProfile profile = entityProfile(UserType.EXTERNAL, false, Set.of());
        UserProfile profileWithNullOffices = entityProfile(UserType.EXTERNAL, false, null);

        assertEquals(UserProfileSilasStatus.NO_ACCESS_ASSIGNED,
                userService.calculateSilasStatusForUserProfile(profile));
        assertEquals(UserProfileSilasStatus.NO_ACCESS_ASSIGNED,
                userService.calculateSilasStatusForUserProfile(profileWithNullOffices));
    }

    @Test
    void calculateSilasStatusForUserProfile_allowsUnrestrictedAndInternalProfilesWithoutOffices() {
        UserProfile unrestrictedProfile = entityProfile(UserType.EXTERNAL, true, Set.of());
        UserProfile internalProfile = entityProfile(UserType.INTERNAL, false, Set.of());
        UserProfile assignedOfficeProfile =
                entityProfile(UserType.EXTERNAL, false, Set.of(mock(Office.class)));

        assertEquals(UserProfileSilasStatus.COMPLETE,
                userService.calculateSilasStatusForUserProfile(unrestrictedProfile));
        assertEquals(UserProfileSilasStatus.COMPLETE,
                userService.calculateSilasStatusForUserProfile(internalProfile));
        assertEquals(UserProfileSilasStatus.COMPLETE,
                userService.calculateSilasStatusForUserProfile(assignedOfficeProfile));
    }

    @Test
    void calculateSilasStatusForUserProfileDto_checksOfficeAccess() {
        UserProfileDto noOfficeProfile = dtoProfile(false, List.of());
        UserProfileDto nullOfficesProfile = dtoProfile(false, null);
        UserProfileDto assignedOfficeProfile = dtoProfile(false, List.of(OfficeDto.builder().build()));
        UserProfileDto unrestrictedProfile = dtoProfile(true, List.of());

        assertEquals(UserProfileSilasStatus.NO_ACCESS_ASSIGNED,
                userService.calculateSilasStatusForUserProfile(noOfficeProfile));
        assertEquals(UserProfileSilasStatus.NO_ACCESS_ASSIGNED,
                userService.calculateSilasStatusForUserProfile(nullOfficesProfile));
        assertEquals(UserProfileSilasStatus.COMPLETE,
                userService.calculateSilasStatusForUserProfile(unrestrictedProfile));
        assertEquals(UserProfileSilasStatus.COMPLETE,
                userService.calculateSilasStatusForUserProfile(assignedOfficeProfile));
    }

    @Test
    void determineStatusBadgeForAuditUser_checksOfficeRestrictions() {
        AuditUserDetailDto noOfficeUser =
                auditUser("External", List.of(auditProfile(null, null)));
        AuditUserDetailDto assignedOfficeUser =
                auditUser("External", List.of(auditProfile(List.of(OfficeDto.builder().build()), null)));
        AuditUserDetailDto unrestrictedUser =
                auditUser("External", List.of(auditProfile(List.of(), "Access to All Offices")));

        assertEquals(UserProfileSilasStatus.NO_ACCESS_ASSIGNED,
                userService.determineStatusBadgeForAuditUser(noOfficeUser));
        assertEquals(UserProfileSilasStatus.COMPLETE,
                userService.determineStatusBadgeForAuditUser(assignedOfficeUser));
        assertEquals(UserProfileSilasStatus.COMPLETE,
                userService.determineStatusBadgeForAuditUser(unrestrictedUser));
    }

    @Test
    void refreshAndUpdatedUserProfileStatus_savesNoAccessStatusForExternalUserWithoutOffices() {
        UserProfile profile = entityProfile(UserType.EXTERNAL, false, Set.of());

        userService.refreshAndUpdatedUserProfileStatus(InvitationStatus.VERIFICATION_SUCCESS, profile);

        assertEquals(UserProfileSilasStatus.NO_ACCESS_ASSIGNED, profile.getSilasStatus());
        verify(userProfileRepository).save(profile);
    }

    private UserProfile entityProfile(UserType userType, boolean unrestricted, Set<Office> offices) {
        return UserProfile.builder()
                .userType(userType)
                .entraUser(EntraUser.builder()
                        .invitationStatus(InvitationStatus.VERIFICATION_SUCCESS)
                        .build())
                .appRoles(Set.of(mock(AppRole.class)))
                .offices(offices)
                .unrestrictedOfficeAccess(unrestricted)
                .build();
    }

    private UserProfileDto dtoProfile(boolean unrestricted, List<OfficeDto> offices) {
        return UserProfileDto.builder()
                .userType(UserType.EXTERNAL)
                .entraUser(EntraUserDto.builder()
                        .invitationStatus(InvitationStatus.VERIFICATION_SUCCESS)
                        .build())
                .appRoles(List.of(mock(AppRoleDto.class)))
                .offices(offices)
                .unrestrictedOfficeAccess(unrestricted)
                .build();
    }

    private AuditUserDetailDto auditUser(String userType, List<AuditUserDetailDto.AuditProfileDto> profiles) {
        return AuditUserDetailDto.builder()
                .userType(userType)
                .activationStatus(InvitationStatus.VERIFICATION_SUCCESS.name())
                .profiles(profiles)
                .build();
    }

    private AuditUserDetailDto.AuditProfileDto auditProfile(List<OfficeDto> offices, String officeRestrictions) {
        return AuditUserDetailDto.AuditProfileDto.builder()
                .roles(List.of(mock(AppRoleDto.class)))
                .offices(offices)
                .officeRestrictions(officeRestrictions)
                .build();
    }
}
