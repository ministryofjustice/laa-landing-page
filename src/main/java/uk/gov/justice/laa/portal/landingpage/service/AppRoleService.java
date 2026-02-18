package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.utils.CcmsRoleGroupsUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppRoleService {

    private final AppRoleRepository appRoleRepository;

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

    public Optional<AppRole> getById(UUID id) {
        return appRoleRepository.findById(id);
    }


    /**
     * Get all app roles for administration display
     */
    public List<AppRoleAdminDto> getAllLaaAppRoles() {
        return appRoleRepository.findByApp_AppType(AppType.LAA).stream()
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
    public List<AppRoleAdminDto> getLaaAppRolesByAppName(String appName) {
        return appRoleRepository.findByApp_NameAndApp_AppType(appName, AppType.LAA).stream()
                .map(this::mapToAppRoleAdminDto)
                .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
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
                .roleGroup(appRole.isAuthzRole() ? "Authorization" :
                        CcmsRoleGroupsUtil.isCcmsRole(appRole.getCcmsCode()) ? "CCMS" : "Default")
                .ordinal(appRole.getOrdinal())
                .authzRole(appRole.isAuthzRole())
                .ccmsCode(appRole.getCcmsCode() != null ? appRole.getCcmsCode() : "")
                .build();
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
