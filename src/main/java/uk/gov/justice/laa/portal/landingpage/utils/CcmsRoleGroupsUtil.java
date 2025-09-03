package uk.gov.justice.laa.portal.landingpage.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;

/**
 * Utility class for dynamically organizing CCMS roles based on their ccmsCode patterns
 * matching the government service UI layout.
 */
public class CcmsRoleGroupsUtil {
    
    public static final String PROVIDER_SECTION = "Provider";
    public static final String CHAMBERS_SECTION = "Chambers";
    public static final String ADVOCATE_SECTION = "Advocate";
    public static final String OTHER_SECTION = "Other";
    
    /**
     * Dynamically organize CCMS app roles by section based on role code patterns
     * @param ccmsRoles List of CCMS app roles to organize
     * @return Map with section names as keys and lists of app roles as values
     */
    public static Map<String, List<AppRoleDto>> organizeCcmsRolesBySection(List<AppRoleDto> ccmsRoles) {
        Map<String, List<AppRoleDto>> organizedRoles = new LinkedHashMap<>();
        
        // Filter and group roles by their ccmsCode patterns
        organizedRoles.put(PROVIDER_SECTION, filterRolesByPattern(ccmsRoles, getProviderPatterns()));
        organizedRoles.put(CHAMBERS_SECTION, filterRolesByPattern(ccmsRoles, getChambersPatterns()));
        organizedRoles.put(ADVOCATE_SECTION, filterRolesByPattern(ccmsRoles, getAdvocatePatterns()));
        
        // Collect any remaining CCMS roles that don't match the known patterns
        List<AppRoleDto> categorizedRoles = organizedRoles.values().stream()
                .flatMap(List::stream)
                .toList();
        
        List<AppRoleDto> otherRoles = ccmsRoles.stream()
                .filter(role -> !categorizedRoles.contains(role))
                .toList();
        
        if (!otherRoles.isEmpty()) {
            organizedRoles.put(OTHER_SECTION, otherRoles);
        }
        
        return organizedRoles;
    }
    
    /**
     * Check if a role code is a CCMS role
     * @param roleCode The role code to check
     * @return true if the role code starts with XXCCMS_
     */
    public static boolean isCcmsRole(String roleCode) {
        return roleCode != null && roleCode.startsWith("XXCCMS_");
    }
    
    /**
     * Get the section name for a given CCMS role code based on patterns
     * @param roleCode The role code to categorize
     * @return The section name (Provider, Chambers, Advocate, or Other) or null if not a CCMS role
     */
    public static String getSectionForRoleCode(String roleCode) {
        if (!isCcmsRole(roleCode)) {
            return null;
        }
        
        if (matchesAnyPattern(roleCode, getProviderPatterns())) {
            return PROVIDER_SECTION;
        } else if (matchesAnyPattern(roleCode, getChambersPatterns())) {
            return CHAMBERS_SECTION;
        } else if (matchesAnyPattern(roleCode, getAdvocatePatterns())) {
            return ADVOCATE_SECTION;
        } else {
            return OTHER_SECTION;
        }
    }
    
    /**
     * Get patterns that identify Provider roles
     */
    private static List<String> getProviderPatterns() {
        return List.of(
            "XXCCMS_FIRM_",
            "XXCCMS_OFFICE_",
            "XXCCMS_CROSS_OFFICE",
            "XXCCMS_PROVIDER_",
            "XXCCMS_CASE_",
            "XXCCMS_BILL_"
        );
    }
    
    /**
     * Get patterns that identify Chambers roles
     */
    private static List<String> getChambersPatterns() {
        return List.of(
            "XXCCMS_CHAMBERS_",
            "XXCCMS_COUNSEL"
        );
    }
    
    /**
     * Get patterns that identify Advocate roles
     */
    private static List<String> getAdvocatePatterns() {
        return List.of(
            "XXCCMS_ADVOCATE"
        );
    }
    
    /**
     * Filter app roles by matching any of the given patterns
     */
    private static List<AppRoleDto> filterRolesByPattern(List<AppRoleDto> roles, List<String> patterns) {
        return roles.stream()
            .filter(role -> matchesAnyPattern(role.getCcmsCode(), patterns))
            .toList();
    }
    
    /**
     * Check if a role code matches any of the given patterns
     */
    private static boolean matchesAnyPattern(String roleCode, List<String> patterns) {
        if (roleCode == null) {
            return false;
        }
        
        return patterns.stream()
            .anyMatch(pattern -> roleCode.contains(pattern));
    }
}
