package uk.gov.justice.laa.portal.landingpage.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

class UserProfileDtoTest {

    private AppRoleDto createAppRole(String id, String name) {
        AppRoleDto appRole = new AppRoleDto();
        appRole.setId(id);
        appRole.setName(name);
        return appRole;
    }

    @Test
    void testDefaultConstructor() {
        // When
        UserProfileDto userProfile = new UserProfileDto();

        // Then
        assertThat(userProfile).isNotNull();
        assertThat(userProfile.getId()).isNull();
        assertThat(userProfile.isActiveProfile()).isFalse();
        assertThat(userProfile.getUserType()).isNull();
        assertThat(userProfile.getLegacyUserId()).isNull();
        assertThat(userProfile.getUserProfileStatus()).isNull();
        assertThat(userProfile.getEntraUser()).isNull();
        assertThat(userProfile.getFirm()).isNull();
        assertThat(userProfile.getOffices()).isNull();
        assertThat(userProfile.getAppRoles()).isNull();
        assertThat(userProfile.getCreatedDate()).isNull();
        assertThat(userProfile.getCreatedBy()).isNull();
        assertThat(userProfile.getLastModified()).isNull();
        assertThat(userProfile.getLastModifiedBy()).isNull();
    }

    @Test
    void testAllArgsConstructor() {
        // Given
        UUID id = UUID.randomUUID();
        boolean activeProfile = true;
        UserType userType = UserType.EXTERNAL;
        String legacyUserId = "legacy123";
        UserProfileStatus userProfileStatus = UserProfileStatus.COMPLETE;
        EntraUserDto entraUser = EntraUserDto.builder()
                .id("entraid123")
                .firstName("John")
                .lastName("Doe")
                .fullName("John Doe")
                .email("john.doe@example.com")
                .build();
        FirmDto firm = FirmDto.builder()
                .id(UUID.randomUUID())
                .name("Test Firm")
                .code("TF001")
                .build();
        List<OfficeDto> offices = List.of(
                OfficeDto.builder()
                        .id(UUID.randomUUID())
                        .code("Test Office")
                        .address(OfficeDto.AddressDto.builder().addressLine1("123 Test Street").build())
                        .build()
        );
        List<AppRoleDto> appRoles = List.of(
                createAppRole("role1", "Test Role")
        );
        LocalDateTime createdDate = LocalDateTime.now();
        String createdBy = "testuser";
        LocalDateTime lastModified = LocalDateTime.now();
        String lastModifiedBy = "admin";

        // When
        UserProfileDto userProfile = new UserProfileDto(
                id, activeProfile, userType, legacyUserId, userProfileStatus,
                entraUser, firm, offices, appRoles,
                createdDate, createdBy, lastModified, lastModifiedBy, true
        );

        // Then
        assertThat(userProfile.getId()).isEqualTo(id);
        assertThat(userProfile.isActiveProfile()).isEqualTo(activeProfile);
        assertThat(userProfile.getUserType()).isEqualTo(userType);
        assertThat(userProfile.getLegacyUserId()).isEqualTo(legacyUserId);
        assertThat(userProfile.getUserProfileStatus()).isEqualTo(userProfileStatus);
        assertThat(userProfile.getEntraUser()).isEqualTo(entraUser);
        assertThat(userProfile.getFirm()).isEqualTo(firm);
        assertThat(userProfile.getOffices()).isEqualTo(offices);
        assertThat(userProfile.getAppRoles()).isEqualTo(appRoles);
        assertThat(userProfile.getCreatedDate()).isEqualTo(createdDate);
        assertThat(userProfile.getCreatedBy()).isEqualTo(createdBy);
        assertThat(userProfile.getLastModified()).isEqualTo(lastModified);
        assertThat(userProfile.getLastModifiedBy()).isEqualTo(lastModifiedBy);
    }

    @Test
    void testBuilderPattern() {
        // Given
        UUID id = UUID.randomUUID();
        EntraUserDto entraUser = EntraUserDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .fullName("Jane Smith")
                .build();

        // When
        UserProfileDto userProfile = UserProfileDto.builder()
                .id(id)
                .activeProfile(true)
                .userType(UserType.INTERNAL)
                .legacyUserId("legacy456")
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .entraUser(entraUser)
                .createdBy("system")
                .build();

        // Then
        assertThat(userProfile.getId()).isEqualTo(id);
        assertThat(userProfile.isActiveProfile()).isTrue();
        assertThat(userProfile.getUserType()).isEqualTo(UserType.INTERNAL);
        assertThat(userProfile.getLegacyUserId()).isEqualTo("legacy456");
        assertThat(userProfile.getUserProfileStatus()).isEqualTo(UserProfileStatus.COMPLETE);
        assertThat(userProfile.getEntraUser()).isEqualTo(entraUser);
        assertThat(userProfile.getCreatedBy()).isEqualTo("system");
    }

    @Test
    void testGetFullNameWhenEntraUserExists() {
        // Given
        EntraUserDto entraUser = EntraUserDto.builder()
                .fullName("John Doe")
                .build();
        UserProfileDto userProfile = UserProfileDto.builder()
                .entraUser(entraUser)
                .build();

        // When
        String fullName = userProfile.getFullName();

        // Then
        assertThat(fullName).isEqualTo("John Doe");
    }

    @Test
    void testGetFullNameWhenEntraUserIsNull() {
        // Given
        UserProfileDto userProfile = UserProfileDto.builder()
                .entraUser(null)
                .build();

        // When
        String fullName = userProfile.getFullName();

        // Then
        assertThat(fullName).isNull();
    }

    @Test
    void testGetFullNameWhenEntraUserFullNameIsNull() {
        // Given
        EntraUserDto entraUser = EntraUserDto.builder()
                .fullName(null)
                .build();
        UserProfileDto userProfile = UserProfileDto.builder()
                .entraUser(entraUser)
                .build();

        // When
        String fullName = userProfile.getFullName();

        // Then
        assertThat(fullName).isNull();
    }

    @Test
    void testEqualsAndHashCodeWhenObjectsAreEqual() {
        // Given
        UUID id = UUID.randomUUID();
        UserProfileDto userProfile1 = UserProfileDto.builder()
                .id(id)
                .activeProfile(true)
                .userType(UserType.EXTERNAL)
                .legacyUserId("legacy123")
                .build();

        UserProfileDto userProfile2 = UserProfileDto.builder()
                .id(id)
                .activeProfile(true)
                .userType(UserType.EXTERNAL)
                .legacyUserId("legacy123")
                .build();

        // When & Then
        assertThat(userProfile1).isEqualTo(userProfile2);
        assertThat(userProfile1.hashCode()).isEqualTo(userProfile2.hashCode());
    }

    @Test
    void testEqualsAndHashCodeWhenObjectsAreDifferent() {
        // Given
        UserProfileDto userProfile1 = UserProfileDto.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .userType(UserType.EXTERNAL)
                .build();

        UserProfileDto userProfile2 = UserProfileDto.builder()
                .id(UUID.randomUUID())
                .activeProfile(false)
                .userType(UserType.INTERNAL)
                .build();

        // When & Then
        assertThat(userProfile1).isNotEqualTo(userProfile2);
        assertThat(userProfile1.hashCode()).isNotEqualTo(userProfile2.hashCode());
    }

    @Test
    void testToString() {
        // Given
        UUID id = UUID.randomUUID();
        UserProfileDto userProfile = UserProfileDto.builder()
                .id(id)
                .activeProfile(true)
                .userType(UserType.EXTERNAL)
                .legacyUserId("legacy123")
                .build();

        // When
        String toString = userProfile.toString();

        // Then
        assertThat(toString).contains("UserProfileDto");
        assertThat(toString).contains(id.toString());
        assertThat(toString).contains("activeProfile=true");
        assertThat(toString).contains("userType=EXTERNAL");
        assertThat(toString).contains("legacyUserId=legacy123");
    }

    @Test
    void testSettersAndGetters() {
        // Given
        UserProfileDto userProfile = new UserProfileDto();
        UUID id = UUID.randomUUID();
        EntraUserDto entraUser = EntraUserDto.builder().firstName("Test").build();
        FirmDto firm = FirmDto.builder().name("Test Firm").build();
        List<OfficeDto> offices = new ArrayList<>();
        List<AppRoleDto> appRoles = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // When
        userProfile.setId(id);
        userProfile.setActiveProfile(true);
        userProfile.setUserType(UserType.EXTERNAL);
        userProfile.setLegacyUserId("legacy789");
        userProfile.setUserProfileStatus(UserProfileStatus.PENDING);
        userProfile.setEntraUser(entraUser);
        userProfile.setFirm(firm);
        userProfile.setOffices(offices);
        userProfile.setAppRoles(appRoles);
        userProfile.setCreatedDate(now);
        userProfile.setCreatedBy("creator");
        userProfile.setLastModified(now);
        userProfile.setLastModifiedBy("modifier");

        // Then
        assertThat(userProfile.getId()).isEqualTo(id);
        assertThat(userProfile.isActiveProfile()).isTrue();
        assertThat(userProfile.getUserType()).isEqualTo(UserType.EXTERNAL);
        assertThat(userProfile.getLegacyUserId()).isEqualTo("legacy789");
        assertThat(userProfile.getUserProfileStatus()).isEqualTo(UserProfileStatus.PENDING);
        assertThat(userProfile.getEntraUser()).isEqualTo(entraUser);
        assertThat(userProfile.getFirm()).isEqualTo(firm);
        assertThat(userProfile.getOffices()).isEqualTo(offices);
        assertThat(userProfile.getAppRoles()).isEqualTo(appRoles);
        assertThat(userProfile.getCreatedDate()).isEqualTo(now);
        assertThat(userProfile.getCreatedBy()).isEqualTo("creator");
        assertThat(userProfile.getLastModified()).isEqualTo(now);
        assertThat(userProfile.getLastModifiedBy()).isEqualTo("modifier");
    }

    @Test
    void testBuilderWithComplexObjects() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        UUID officeId = UUID.randomUUID();

        EntraUserDto entraUser = EntraUserDto.builder()
                .id("entra123")
                .firstName("Complex")
                .lastName("User")
                .fullName("Complex User")
                .email("complex.user@example.com")
                .entraOid("oid123")
                .lastLoggedIn("2023-01-01T10:00:00")
                .build();

        FirmDto firm = FirmDto.builder()
                .id(firmId)
                .name("Complex Firm")
                .code("CF001")
                .build();

        OfficeDto office = OfficeDto.builder()
                .id(officeId)
                .code("Complex Office")
                .address(OfficeDto.AddressDto.builder().addressLine1("123 Complex Street").build())
                .build();

        AppRoleDto appRole = createAppRole("complexrole", "Complex Role");

        // When
        UserProfileDto userProfile = UserProfileDto.builder()
                .id(userId)
                .activeProfile(true)
                .userType(UserType.EXTERNAL)
                .legacyUserId("complex123")
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .entraUser(entraUser)
                .firm(firm)
                .offices(List.of(office))
                .appRoles(List.of(appRole))
                .createdDate(LocalDateTime.of(2023, 1, 1, 10, 0))
                .createdBy("system")
                .lastModified(LocalDateTime.of(2023, 1, 2, 11, 0))
                .lastModifiedBy("admin")
                .build();

        // Then
        assertThat(userProfile.getId()).isEqualTo(userId);
        assertThat(userProfile.isActiveProfile()).isTrue();
        assertThat(userProfile.getUserType()).isEqualTo(UserType.EXTERNAL);
        assertThat(userProfile.getLegacyUserId()).isEqualTo("complex123");
        assertThat(userProfile.getUserProfileStatus()).isEqualTo(UserProfileStatus.COMPLETE);
        
        assertThat(userProfile.getEntraUser()).isEqualTo(entraUser);
        assertThat(userProfile.getEntraUser().getFirstName()).isEqualTo("Complex");
        assertThat(userProfile.getEntraUser().getLastName()).isEqualTo("User");
        assertThat(userProfile.getFullName()).isEqualTo("Complex User");
        
        assertThat(userProfile.getFirm()).isEqualTo(firm);
        assertThat(userProfile.getFirm().getName()).isEqualTo("Complex Firm");
        
        assertThat(userProfile.getOffices()).hasSize(1);
        assertThat(userProfile.getOffices().get(0)).isEqualTo(office);
        assertThat(userProfile.getOffices().get(0).getCode()).isEqualTo("Complex Office");
        
        assertThat(userProfile.getAppRoles()).hasSize(1);
        assertThat(userProfile.getAppRoles().get(0)).isEqualTo(appRole);
        assertThat(userProfile.getAppRoles().get(0).getName()).isEqualTo("Complex Role");
        
        assertThat(userProfile.getCreatedDate()).isEqualTo(LocalDateTime.of(2023, 1, 1, 10, 0));
        assertThat(userProfile.getCreatedBy()).isEqualTo("system");
        assertThat(userProfile.getLastModified()).isEqualTo(LocalDateTime.of(2023, 1, 2, 11, 0));
        assertThat(userProfile.getLastModifiedBy()).isEqualTo("admin");
    }

    @Test
    void testBuilderWithEmptyCollections() {
        // Given
        UUID userId = UUID.randomUUID();
        List<OfficeDto> emptyOffices = new ArrayList<>();
        List<AppRoleDto> emptyAppRoles = new ArrayList<>();

        // When
        UserProfileDto userProfile = UserProfileDto.builder()
                .id(userId)
                .offices(emptyOffices)
                .appRoles(emptyAppRoles)
                .build();

        // Then
        assertThat(userProfile.getId()).isEqualTo(userId);
        assertThat(userProfile.getOffices()).isNotNull();
        assertThat(userProfile.getOffices()).isEmpty();
        assertThat(userProfile.getAppRoles()).isNotNull();
        assertThat(userProfile.getAppRoles()).isEmpty();
    }

    @Test
    void testEqualsWithNull() {
        // Given
        UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.randomUUID())
                .build();

        // When & Then
        assertThat(userProfile).isNotEqualTo(null);
    }

    @Test
    void testEqualsWithDifferentClass() {
        // Given
        UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.randomUUID())
                .build();
        String differentObject = "not a UserProfileDto";

        // When & Then
        assertThat(userProfile).isNotEqualTo(differentObject);
    }

    @Test
    void testEqualsWithSameReference() {
        // Given
        UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.randomUUID())
                .build();

        // When & Then
        assertThat(userProfile).isEqualTo(userProfile);
    }

    @Test
    void testAllUserTypes() {
        // Test all enum values are supported
        for (UserType userType : UserType.values()) {
            UserProfileDto userProfile = UserProfileDto.builder()
                    .userType(userType)
                    .build();
            
            assertThat(userProfile.getUserType()).isEqualTo(userType);
        }
    }

    @Test
    void testAllUserProfileStatuses() {
        // Test all enum values are supported
        for (UserProfileStatus status : UserProfileStatus.values()) {
            UserProfileDto userProfile = UserProfileDto.builder()
                    .userProfileStatus(status)
                    .build();
            
            assertThat(userProfile.getUserProfileStatus()).isEqualTo(status);
        }
    }
}
