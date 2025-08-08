package uk.gov.justice.laa.portal.landingpage.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;

/**
 * Test class for exercising CcmsRoleGroupsUtil
 */
public class CcmsRoleGroupsUtilTest {

    // ===== Tests for organizeCcmsRolesBySection =====

    @Test
    void organizeCcmsRolesBySection_shouldOrganizeProviderRoles() {
        // Given
        List<AppRoleDto> ccmsRoles = List.of(
            createAppRoleDto("role1", "XXCCMS_FIRM_ADMIN"),
            createAppRoleDto("role2", "XXCCMS_OFFICE_MANAGER"),
            createAppRoleDto("role3", "XXCCMS_CROSS_OFFICE_USER"),
            createAppRoleDto("role4", "XXCCMS_PROVIDER_ADMIN"),
            createAppRoleDto("role5", "XXCCMS_CASE_MANAGER"),
            createAppRoleDto("role6", "XXCCMS_BILL_REVIEWER")
        );

        // When
        Map<String, List<AppRoleDto>> result = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);

        // Then
        assertThat(result).containsKey(CcmsRoleGroupsUtil.PROVIDER_SECTION);
        assertThat(result.get(CcmsRoleGroupsUtil.PROVIDER_SECTION)).hasSize(6);
        assertThat(result.get(CcmsRoleGroupsUtil.PROVIDER_SECTION))
            .extracting(AppRoleDto::getCcmsCode)
            .containsExactlyInAnyOrder(
                "XXCCMS_FIRM_ADMIN", 
                "XXCCMS_OFFICE_MANAGER", 
                "XXCCMS_CROSS_OFFICE_USER",
                "XXCCMS_PROVIDER_ADMIN",
                "XXCCMS_CASE_MANAGER",
                "XXCCMS_BILL_REVIEWER"
            );
    }

    @Test
    void organizeCcmsRolesBySection_shouldOrganizeChambersRoles() {
        // Given
        List<AppRoleDto> ccmsRoles = List.of(
            createAppRoleDto("role1", "XXCCMS_CHAMBERS_ADMIN"),
            createAppRoleDto("role2", "XXCCMS_CHAMBERS_USER"),
            createAppRoleDto("role3", "XXCCMS_COUNSEL")
        );

        // When
        Map<String, List<AppRoleDto>> result = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);

        // Then
        assertThat(result).containsKey(CcmsRoleGroupsUtil.CHAMBERS_SECTION);
        assertThat(result.get(CcmsRoleGroupsUtil.CHAMBERS_SECTION)).hasSize(3);
        assertThat(result.get(CcmsRoleGroupsUtil.CHAMBERS_SECTION))
            .extracting(AppRoleDto::getCcmsCode)
            .containsExactlyInAnyOrder(
                "XXCCMS_CHAMBERS_ADMIN", 
                "XXCCMS_CHAMBERS_USER", 
                "XXCCMS_COUNSEL"
            );
    }

    @Test
    void organizeCcmsRolesBySection_shouldOrganizeAdvocateRoles() {
        // Given
        List<AppRoleDto> ccmsRoles = List.of(
            createAppRoleDto("role1", "XXCCMS_ADVOCATE"),
            createAppRoleDto("role2", "XXCCMS_ADVOCATE_ADMIN")
        );

        // When
        Map<String, List<AppRoleDto>> result = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);

        // Then
        assertThat(result).containsKey(CcmsRoleGroupsUtil.ADVOCATE_SECTION);
        assertThat(result.get(CcmsRoleGroupsUtil.ADVOCATE_SECTION)).hasSize(2);
        assertThat(result.get(CcmsRoleGroupsUtil.ADVOCATE_SECTION))
            .extracting(AppRoleDto::getCcmsCode)
            .containsExactlyInAnyOrder("XXCCMS_ADVOCATE", "XXCCMS_ADVOCATE_ADMIN");
    }

    @Test
    void organizeCcmsRolesBySection_shouldOrganizeOtherRoles() {
        // Given
        List<AppRoleDto> ccmsRoles = List.of(
            createAppRoleDto("role1", "XXCCMS_UNKNOWN_ROLE"),
            createAppRoleDto("role2", "XXCCMS_SPECIAL_ACCESS"),
            createAppRoleDto("role3", "XXCCMS_SYSTEM_ADMIN")
        );

        // When
        Map<String, List<AppRoleDto>> result = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);

        // Then
        assertThat(result).containsKey(CcmsRoleGroupsUtil.OTHER_SECTION);
        assertThat(result.get(CcmsRoleGroupsUtil.OTHER_SECTION)).hasSize(3);
        assertThat(result.get(CcmsRoleGroupsUtil.OTHER_SECTION))
            .extracting(AppRoleDto::getCcmsCode)
            .containsExactlyInAnyOrder(
                "XXCCMS_UNKNOWN_ROLE", 
                "XXCCMS_SPECIAL_ACCESS", 
                "XXCCMS_SYSTEM_ADMIN"
            );
    }

    @Test
    void organizeCcmsRolesBySection_shouldOrganizeAllSections() {
        // Given
        List<AppRoleDto> ccmsRoles = List.of(
            createAppRoleDto("provider1", "XXCCMS_FIRM_ADMIN"),
            createAppRoleDto("provider2", "XXCCMS_OFFICE_USER"),
            createAppRoleDto("chambers1", "XXCCMS_CHAMBERS_ADMIN"),
            createAppRoleDto("chambers2", "XXCCMS_COUNSEL"),
            createAppRoleDto("advocate1", "XXCCMS_ADVOCATE"),
            createAppRoleDto("other1", "XXCCMS_SYSTEM_ROLE")
        );

        // When
        Map<String, List<AppRoleDto>> result = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);

        // Then
        assertThat(result).hasSize(4);
        assertThat(result).containsKeys(
            CcmsRoleGroupsUtil.PROVIDER_SECTION,
            CcmsRoleGroupsUtil.CHAMBERS_SECTION,
            CcmsRoleGroupsUtil.ADVOCATE_SECTION,
            CcmsRoleGroupsUtil.OTHER_SECTION
        );
        
        assertThat(result.get(CcmsRoleGroupsUtil.PROVIDER_SECTION)).hasSize(2);
        assertThat(result.get(CcmsRoleGroupsUtil.CHAMBERS_SECTION)).hasSize(2);
        assertThat(result.get(CcmsRoleGroupsUtil.ADVOCATE_SECTION)).hasSize(1);
        assertThat(result.get(CcmsRoleGroupsUtil.OTHER_SECTION)).hasSize(1);
    }

    @Test
    void organizeCcmsRolesBySection_shouldHandleEmptyList() {
        // Given
        List<AppRoleDto> ccmsRoles = new ArrayList<>();

        // When
        Map<String, List<AppRoleDto>> result = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);

        // Then
        assertThat(result).hasSize(3); // Provider, Chambers, Advocate sections always present
        assertThat(result).containsKeys(
            CcmsRoleGroupsUtil.PROVIDER_SECTION,
            CcmsRoleGroupsUtil.CHAMBERS_SECTION,
            CcmsRoleGroupsUtil.ADVOCATE_SECTION
        );
        assertThat(result).doesNotContainKey(CcmsRoleGroupsUtil.OTHER_SECTION);
        
        assertThat(result.get(CcmsRoleGroupsUtil.PROVIDER_SECTION)).isEmpty();
        assertThat(result.get(CcmsRoleGroupsUtil.CHAMBERS_SECTION)).isEmpty();
        assertThat(result.get(CcmsRoleGroupsUtil.ADVOCATE_SECTION)).isEmpty();
    }

    @Test
    void organizeCcmsRolesBySection_shouldHandleRolesWithNullCcmsCode() {
        // Given
        List<AppRoleDto> ccmsRoles = List.of(
            createAppRoleDto("role1", "XXCCMS_FIRM_ADMIN"),
            createAppRoleDto("role2", null),
            createAppRoleDto("role3", "XXCCMS_ADVOCATE")
        );

        // When
        Map<String, List<AppRoleDto>> result = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);

        // Then
        assertThat(result.get(CcmsRoleGroupsUtil.PROVIDER_SECTION)).hasSize(1);
        assertThat(result.get(CcmsRoleGroupsUtil.ADVOCATE_SECTION)).hasSize(1);
        assertThat(result.get(CcmsRoleGroupsUtil.OTHER_SECTION)).hasSize(1); // Role with null ccmsCode goes to Other
    }

    @Test
    void organizeCcmsRolesBySection_shouldMaintainLinkedHashMapOrder() {
        // Given
        List<AppRoleDto> ccmsRoles = List.of(createAppRoleDto("role1", "XXCCMS_FIRM_ADMIN"));

        // When
        Map<String, List<AppRoleDto>> result = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);

        // Then
        assertThat(result.keySet()).containsExactly(
            CcmsRoleGroupsUtil.PROVIDER_SECTION,
            CcmsRoleGroupsUtil.CHAMBERS_SECTION,
            CcmsRoleGroupsUtil.ADVOCATE_SECTION
        );
    }

    // ===== Tests for isCcmsRole =====

    @Test
    void isCcmsRole_shouldReturnTrueForValidCcmsRoles() {
        // Given & When & Then
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("XXCCMS_FIRM_ADMIN")).isTrue();
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("XXCCMS_OFFICE_USER")).isTrue();
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("XXCCMS_CHAMBERS_ADMIN")).isTrue();
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("XXCCMS_ADVOCATE")).isTrue();
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("XXCCMS_UNKNOWN_ROLE")).isTrue();
    }

    @Test
    void isCcmsRole_shouldReturnFalseForNonCcmsRoles() {
        // Given & When & Then
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("REGULAR_ROLE")).isFalse();
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("ADMIN_ROLE")).isFalse();
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("USER_ROLE")).isFalse();
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("CCMS_ROLE")).isFalse(); // Missing XX prefix
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("XCCMS_ROLE")).isFalse(); // Missing one X
    }

    @Test
    void isCcmsRole_shouldReturnFalseForNullOrEmptyRoleCode() {
        // Given & When & Then
        assertThat(CcmsRoleGroupsUtil.isCcmsRole(null)).isFalse();
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("")).isFalse();
        assertThat(CcmsRoleGroupsUtil.isCcmsRole("   ")).isFalse();
    }

    // ===== Tests for getSectionForRoleCode =====

    @Test
    void getSectionForRoleCode_shouldReturnProviderSectionForProviderRoles() {
        // Given & When & Then
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_FIRM_ADMIN"))
            .isEqualTo(CcmsRoleGroupsUtil.PROVIDER_SECTION);
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_OFFICE_USER"))
            .isEqualTo(CcmsRoleGroupsUtil.PROVIDER_SECTION);
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_CROSS_OFFICE_ADMIN"))
            .isEqualTo(CcmsRoleGroupsUtil.PROVIDER_SECTION);
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_PROVIDER_MANAGER"))
            .isEqualTo(CcmsRoleGroupsUtil.PROVIDER_SECTION);
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_CASE_WORKER"))
            .isEqualTo(CcmsRoleGroupsUtil.PROVIDER_SECTION);
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_BILL_ADMIN"))
            .isEqualTo(CcmsRoleGroupsUtil.PROVIDER_SECTION);
    }

    @Test
    void getSectionForRoleCode_shouldReturnChambersSectionForChambersRoles() {
        // Given & When & Then
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_CHAMBERS_ADMIN"))
            .isEqualTo(CcmsRoleGroupsUtil.CHAMBERS_SECTION);
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_CHAMBERS_USER"))
            .isEqualTo(CcmsRoleGroupsUtil.CHAMBERS_SECTION);
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_COUNSEL"))
            .isEqualTo(CcmsRoleGroupsUtil.CHAMBERS_SECTION);
    }

    @Test
    void getSectionForRoleCode_shouldReturnAdvocateSectionForAdvocateRoles() {
        // Given & When & Then
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_ADVOCATE"))
            .isEqualTo(CcmsRoleGroupsUtil.ADVOCATE_SECTION);
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_ADVOCATE_ADMIN"))
            .isEqualTo(CcmsRoleGroupsUtil.ADVOCATE_SECTION);
    }

    @Test
    void getSectionForRoleCode_shouldReturnOtherSectionForUnknownCcmsRoles() {
        // Given & When & Then
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_UNKNOWN_ROLE"))
            .isEqualTo(CcmsRoleGroupsUtil.OTHER_SECTION);
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_SPECIAL_ACCESS"))
            .isEqualTo(CcmsRoleGroupsUtil.OTHER_SECTION);
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("XXCCMS_SYSTEM_ADMIN"))
            .isEqualTo(CcmsRoleGroupsUtil.OTHER_SECTION);
    }

    @Test
    void getSectionForRoleCode_shouldReturnNullForNonCcmsRoles() {
        // Given & When & Then
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("REGULAR_ROLE")).isNull();
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("ADMIN_ROLE")).isNull();
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("USER_ROLE")).isNull();
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode(null)).isNull();
        assertThat(CcmsRoleGroupsUtil.getSectionForRoleCode("")).isNull();
    }

    // ===== Edge Case Tests =====

    @Test
    void organizeCcmsRolesBySection_shouldHandleRolesThatMatchMultiplePatterns() {
        // Given - role that could theoretically match multiple patterns (though unlikely in practice)
        List<AppRoleDto> ccmsRoles = List.of(
            createAppRoleDto("role1", "XXCCMS_FIRM_CHAMBERS_HYBRID") // Contains both FIRM_ and CHAMBERS_
        );

        // When
        Map<String, List<AppRoleDto>> result = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);

        // Then - should be placed in the first matching section (Provider in this case)
        assertThat(result.get(CcmsRoleGroupsUtil.PROVIDER_SECTION)).hasSize(1);
        assertThat(result.get(CcmsRoleGroupsUtil.CHAMBERS_SECTION)).isEmpty();
    }

    @Test
    void organizeCcmsRolesBySection_shouldHandleCaseInsensitiveMatching() {
        // Given - testing that patterns are case sensitive as expected
        List<AppRoleDto> ccmsRoles = List.of(
            createAppRoleDto("role1", "xxccms_firm_admin"), // lowercase
            createAppRoleDto("role2", "XXCCMS_FIRM_ADMIN")  // uppercase
        );

        // When
        Map<String, List<AppRoleDto>> result = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);

        // Then - only the properly formatted uppercase role should match Provider pattern
        assertThat(result.get(CcmsRoleGroupsUtil.PROVIDER_SECTION)).hasSize(1);
        assertThat(result.get(CcmsRoleGroupsUtil.OTHER_SECTION)).hasSize(1);
    }

    @Test
    void organizeCcmsRolesBySection_shouldHandleDuplicateRoles() {
        // Given
        AppRoleDto role1 = createAppRoleDto("role1", "XXCCMS_FIRM_ADMIN");
        AppRoleDto role2 = createAppRoleDto("role1", "XXCCMS_FIRM_ADMIN"); // Same ID and code
        List<AppRoleDto> ccmsRoles = List.of(role1, role2);

        // When
        Map<String, List<AppRoleDto>> result = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);

        // Then - both roles should be included (they are different objects)
        assertThat(result.get(CcmsRoleGroupsUtil.PROVIDER_SECTION)).hasSize(2);
    }

    // ===== Helper Methods =====

    /**
     * Helper method to create AppRoleDto for testing
     */
    private AppRoleDto createAppRoleDto(String id, String ccmsCode) {
        AppRoleDto role = new AppRoleDto();
        role.setId(id);
        role.setCcmsCode(ccmsCode);
        role.setName("Test Role " + id);
        role.setDescription("Test description for role " + id);
        return role;
    }
}
