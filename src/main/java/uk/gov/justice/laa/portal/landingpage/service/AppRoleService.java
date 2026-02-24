package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.DeleteAppRoleEvent;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppRoleService {

    private final AppRoleRepository appRoleRepository;

    private final RoleAssignmentRepository roleAssignmentRepository;

    private final UserProfileRepository userProfileRepository;

    private final EventService eventService;

    private final ModelMapper modelMapper;

    public List<AppRoleDto> getByIds(Collection<String> ids) {

        List<UUID> appRoleIds = ids.stream().map(UUID::fromString).toList();

        List<AppRole> appRoles = appRoleRepository.findAllById(appRoleIds);

        if (appRoles.size() != ids.size()) {
            throw new RuntimeException("Failed to load all app roles from request: " + ids);
        }

        return appRoles.stream().map((element) -> modelMapper.map(element, AppRoleDto.class)).toList();
    }

    public Optional<AppRoleDto> findById(UUID id) {
        return appRoleRepository.findById(id).map(app -> modelMapper.map(app, AppRoleDto.class));
    }

    public Optional<AppRoleDto> findById(String id) {
        return findById(UUID.fromString(id));
    }

    public Optional<AppRole> getById(UUID id) {
        return appRoleRepository.findById(id);
    }


    /**
     * Get all app roles for administration display
     */
    public List<AppRoleAdminDto> getAllLaaAppRoles() {
        return appRoleRepository.findByApp_AppType(AppType.LAA).stream()
                .map(this::mapToAppRoleAdminDto)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get app roles filtered by app
     */
    public List<AppRoleAdminDto> getLaaAppRolesByAppName(String appName) {
        return appRoleRepository.findByApp_NameAndApp_AppType(appName, AppType.LAA).stream()
                .map(this::mapToAppRoleAdminDto)
                .sorted()
                .collect(Collectors.toList());
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
                .ccmsCode(appRole.getCcmsCode() == null ? "" : appRole.getCcmsCode())
                .legacySync(appRole.isLegacySync() ? "Yes" : "No")
                .ordinal(appRole.getOrdinal())
                .authzRole(appRole.isAuthzRole())
                .build();
    }

    @Transactional
    public void deleteAppRole(UUID userProfileId, UUID entraOid, String appName, String roleId, String reason) {
        UUID id = UUID.fromString(roleId);
        AppRole appRole = getById(id).orElseThrow();

        // Delete User profile role assignments
        userProfileRepository.deleteAllByAppRoleId(id);

        // Delete role assignments for groups
        roleAssignmentRepository.deleteByRoleIdInEitherColumn(id);

        // Delete role permissions
        appRoleRepository.deleteRolePermissions(id);

        // Delete role
        appRoleRepository.delete(appRole);

        DeleteAppRoleEvent deleteAppRoleEvent =
                new DeleteAppRoleEvent(
                        userProfileId,
                        entraOid,
                        appName,
                        appRole.getName(),
                        reason.trim()
                );

        eventService.logEvent(deleteAppRoleEvent);

        log.info("User profile {} removed app role {} from app {} including role permissions and assignments (Reason: {})",
                userProfileId, appRole.getName(), appName, reason);

    }

    public long countNoOfRoleAssignments(String roleId) {
        UUID id = UUID.fromString(roleId);
        return userProfileRepository.countUserProfilesByAppRoleId(id);
    }

    public long countNoOfFirmsWithRoleAssignments(String roleId) {
        UUID id = UUID.fromString(roleId);
        return userProfileRepository.countFirmsWithRole(id);
    }

    @Transactional
    public AppRole save(AppRoleDto roleDto) {

        AppRole appRole = getById(UUID.fromString(roleDto.getId()))
                .orElseThrow(() -> new RuntimeException(String.format("App not found for the give app id: %s", roleDto.getId())));
        appRole.setName(roleDto.getName());
        appRole.setDescription(roleDto.getDescription());
        return appRoleRepository.save(appRole);

    }

    @Transactional
    public void updateAppRolesOrder(@Valid @NotNull List<AppRolesOrderForm.AppRolesOrderDetailsForm> appRoles) {
        for (AppRolesOrderForm.AppRolesOrderDetailsForm appRole : appRoles) {
            AppRole appRoleEntity = getById(UUID.fromString(appRole.getAppRoleId())).orElseThrow();
            appRoleEntity.setOrdinal(appRole.getOrdinal());
        }
    }
}
