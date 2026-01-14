package uk.gov.justice.laa.portal.landingpage.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.RoleAssignmentAdminDto;
import uk.gov.justice.laa.portal.landingpage.entity.AdminApp;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AdminAppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;

/**
 * Service for SiLAS Administration functionality
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final AppRepository appRepository;
    private final AppRoleRepository appRoleRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final AdminAppRepository adminAppRepository;

    /**
     * Get all admin apps for administration display
     */
    public List<AdminAppDto> getAllAdminApps() {
        return adminAppRepository.findAll().stream()
                .filter(AdminApp::isEnabled)
                .map(this::mapToAdminAppDto)
                .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
                .collect(Collectors.toList());
    }

    /**
     * Get all apps for administration display
     */
    public List<AppAdminDto> getAllApps() {
        return appRepository.findAll().stream()
                .map(this::mapToAppAdminDto)
                .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
                .collect(Collectors.toList());
    }

    /**
     * Get admin service apps only
     */
    public List<AppAdminDto> getAdminApps() {
        return appRepository.findAll().stream()
                .filter(app -> app.getAppType() != null && "AUTHZ".equals(app.getAppType().name()))
                .map(this::mapToAppAdminDto)
                .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
                .collect(Collectors.toList());
    }

    /**
     * Get all app roles for administration display
     */
    public List<AppRoleAdminDto> getAllAppRoles() {
        return appRoleRepository.findAll().stream()
                .map(this::mapToAppRoleAdminDto)
                .sorted((a, b) -> {
                    // Sort by parent app name, then by ordinal
                    int appCompare = a.getParentApp().compareTo(b.getParentApp());
                    if (appCompare != 0) {
                        return appCompare;
                    }
                    return Integer.compare(a.getOrdinal(), b.getOrdinal());
                })
                .collect(Collectors.toList());
    }

    /**
     * Get app roles filtered by app
     */
    public List<AppRoleAdminDto> getAppRolesByApp(String appName) {
        return appRoleRepository.findAll().stream()
                .filter(role -> role.getApp() != null && role.getApp().getName().equals(appName))
                .map(this::mapToAppRoleAdminDto)
                .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
                .collect(Collectors.toList());
    }

    /**
     * Get all role assignments for administration display
     */
    public List<RoleAssignmentAdminDto> getAllRoleAssignments() {
        return roleAssignmentRepository.findAll().stream()
                .map(this::mapToRoleAssignmentAdminDto)
                .sorted((a, b) -> a.getAssigningRoleName().compareTo(b.getAssigningRoleName()))
                .collect(Collectors.toList());
    }

    /**
     * Map App entity to AppAdminDto
     */
    private AppAdminDto mapToAppAdminDto(App app) {
        return AppAdminDto.builder()
                .id(app.getId().toString())
                .name(app.getName())
                .description(app.getDescription())
                .ordinal(app.getOrdinal())
                .url(app.getUrl())
                .enabled(app.isEnabled())
                .appType(app.getAppType() != null ? app.getAppType().name() : "")
                .build();
    }

    /**
     * Map AppRole entity to AppRoleAdminDto
     */
    private AppRoleAdminDto mapToAppRoleAdminDto(AppRole appRole) {
        String userTypeRestriction = "";
        if (appRole.getUserTypeRestriction() != null && appRole.getUserTypeRestriction().length > 0) {
            userTypeRestriction = Arrays.stream(appRole.getUserTypeRestriction())
                    .map(UserType::name)
                    .collect(Collectors.joining(", "));
        }

        return AppRoleAdminDto.builder()
                .id(appRole.getId().toString())
                .name(appRole.getName())
                .description(appRole.getDescription())
                .userTypeRestriction(userTypeRestriction)
                .parentApp(appRole.getApp() != null ? appRole.getApp().getName() : "")
                .parentAppId(appRole.getApp() != null ? appRole.getApp().getId().toString() : "")
                .roleGroup(appRole.isAuthzRole() ? "Authorization" : "CCMS")
                .ordinal(appRole.getOrdinal())
                .authzRole(appRole.isAuthzRole())
                .ccmsCode(appRole.getCcmsCode() != null ? appRole.getCcmsCode() : "")
                .build();
    }

    /**
     * Map RoleAssignment entity to RoleAssignmentAdminDto
     */
    private RoleAssignmentAdminDto mapToRoleAssignmentAdminDto(RoleAssignment roleAssignment) {
        return RoleAssignmentAdminDto.builder()
                .assigningRoleId(roleAssignment.getAssigningRole().getId().toString())
                .assigningRoleName(roleAssignment.getAssigningRole().getName())
                .assignableRoleId(roleAssignment.getAssignableRole().getId().toString())
                .assignableRoleName(roleAssignment.getAssignableRole().getName())
                .build();
    }

    /**
     * Map AdminApp entity to AdminAppDto
     */
    private AdminAppDto mapToAdminAppDto(AdminApp adminApp) {
        return AdminAppDto.builder()
                .id(adminApp.getId().toString())
                .name(adminApp.getName())
                .description(adminApp.getDescription())
                .ordinal(adminApp.getOrdinal())
                .enabled(adminApp.isEnabled())
                .build();
    }
}
