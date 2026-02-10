package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.utils.CcmsRoleGroupsUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {

    private final AppRepository appRepository;

    private final AppRoleRepository appRoleRepository;

    private final ModelMapper mapper;

    public Optional<App> getById(UUID id) {
        return appRepository.findById(id);
    }

    public List<AppDto> getAllLaaApps() {
        return appRepository.findAppsByAppType(AppType.LAA)
                .stream()
                .map(app -> mapper.map(app, AppDto.class))
                .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
                .toList();
    }

    public List<AppDto> getAllActiveLaaApps() {
        return appRepository.findAppsByAppTypeAndEnabled(AppType.LAA, true)
                .stream()
                .map(app -> mapper.map(app, AppDto.class))
                .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
                .toList();
    }

    public List<AppDto> getAllAdminApps() {
        return appRepository.findAppsByAppType(AppType.AUTHZ)
                .stream()
                .map(app -> mapper.map(app, AppDto.class))
                .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
                .collect(Collectors.toList());
    }

    /**
     * Get all admin apps for administration display
     */
    public List<AppDto> getAllActiveAdminApps() {
        return appRepository.findAppsByAppTypeAndEnabled(AppType.AUTHZ, true)
                .stream()
                .map(app -> mapper.map(app, AppDto.class))
                .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
                .collect(Collectors.toList());
    }

    /**
     * Get all app roles for administration display
     */
    public List<AppRoleDto> getAllAdminAppRoles() {
        return appRoleRepository.findByApp_AppType(AppType.AUTHZ).stream()
                .map(this::mapToAppRoleAdminDtoForView)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all app roles for administration display
     */
    public List<AppDto> getAllAppsForAdmin() {
        return appRepository.findAll().stream()
                .map(app -> mapper.map(app, AppDto.class))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all app roles for administration display
     */
    public List<AppRoleDto> getAllAppRolesForAdmin() {
        return appRoleRepository.findAll().stream()
                .map(this::mapToAppRoleAdminDtoForView)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get app roles filtered by app
     */
    public List<AppRoleDto> getAppRolesByApp(String appName) {
        return appRoleRepository.findByApp_Name(appName).stream()
                .map(this::mapToAppRoleAdminDtoForView)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Map AppRole entity to AppRoleAdminDto
     */
    private AppRoleDto mapToAppRoleAdminDtoForView(AppRole appRole) {
        AppRoleDto appRoleDto = mapper.map(appRole, AppRoleDto.class);

        appRoleDto.setUserTypeRestrictionStr(getUserTypeRestrictionsForView(appRole));
        appRoleDto.setRoleGroup(getRoleGroupFroView(appRole));

        return appRoleDto;
    }

    private String getUserTypeRestrictionsForView(AppRole appRole) {
        if (appRole == null || appRole.getUserTypeRestriction() == null || appRole.getUserTypeRestriction().length == 0) {
            return "";
        }

        return Arrays.stream(appRole.getUserTypeRestriction())
                .map(UserType::name)
                .collect(Collectors.joining(", "));
    }

    private String getRoleGroupFroView(AppRole appRole) {
        return appRole.isAuthzRole() ? "Authorization" :
                CcmsRoleGroupsUtil.isCcmsRole(appRole.getCcmsCode()) ? "CCMS" : "Default";
    }

}
