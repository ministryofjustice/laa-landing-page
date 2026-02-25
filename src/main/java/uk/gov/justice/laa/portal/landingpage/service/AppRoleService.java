package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppRoleService {

    private final AppRoleRepository appRoleRepository;
    private final AppRepository appRepository;
    private final EventService eventService;
    private final LoginService loginService;
    private final ModelMapper modelMapper;

    public List<AppRoleDto> getByIds(Collection<String> ids) {

        List<UUID> appRoleIds = ids.stream().map(UUID::fromString).toList();

        List<AppRole> appRoles = appRoleRepository.findAllById(appRoleIds);

        if (appRoles.size() != ids.size()) {
            throw new RuntimeException("Failed to load all app roles from request: " + ids);
        }

        return appRoles.stream().map((element) -> modelMapper.map(element, AppRoleDto.class)).toList();
    }

    public boolean isRoleNameExistsInApp(String roleName, UUID appId) {
        return appRoleRepository.findAll().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName) 
                    && role.getApp().getId().equals(appId));
    }

    public RoleCreationDto enrichRoleCreationDto(RoleCreationDto dto) {
        dto.setId(UUID.randomUUID());

        Optional<App> app = appRepository.findById(dto.getParentAppId());
        String parentAppName = app.map(App::getName).orElse("");
        dto.setAuthzRole(setAuthzRoleFlag(dto.getName(), parentAppName));

        dto.setOrdinal(generateNextOrdinal());

        app.ifPresent(value -> dto.setParentAppName(value.getName()));
        
        return dto;
    }

    @Transactional
    public void createRole(RoleCreationDto dto) {
        // Validate unique role name within app
        validateUniqueRoleName(dto.getName(), dto.getParentAppId());
        
        // Get parent app
        App parentApp = appRepository.findById(dto.getParentAppId())
                .orElseThrow(() -> new IllegalArgumentException("Parent app not found with ID: " + dto.getParentAppId()));

        UserType[] userTypeArray = dto.getUserTypeRestriction() != null 
            ? dto.getUserTypeRestriction().toArray(new UserType[0]) 
            : null;
            
        FirmType[] firmTypeArray = dto.getFirmTypeRestriction() != null 
            ? dto.getFirmTypeRestriction().toArray(new FirmType[0]) 
            : null;

        // Convert empty CCMS code to null to avoid unique constraint violations
        String ccmsCode = dto.getCcmsCode();
        if (ccmsCode != null && ccmsCode.trim().isEmpty()) {
            ccmsCode = null;
        }

        AppRole appRole = AppRole.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .ccmsCode(ccmsCode)
                .legacySync(dto.getLegacySync() != null ? dto.getLegacySync() : false)
                .authzRole(dto.isAuthzRole())
                .ordinal(dto.getOrdinal())
                .userTypeRestriction(userTypeArray)
                .firmTypeRestriction(firmTypeArray)
                .app(parentApp)
                .build();

        AppRole savedRole = appRoleRepository.save(appRole);

        String userTypeRestrictionStr = String.join(", ", 
            stream(savedRole.getUserTypeRestriction())
                .map(Enum::name)
                .toArray(String[]::new));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CurrentUserDto currentUserDto = loginService.getCurrentUser(auth);
        EntraUser currentEntraUser = loginService.getCurrentEntraUser(auth);

        String activeUserProfileId = currentEntraUser.getUserProfiles().stream()
            .filter(UserProfile::isActiveProfile)
            .findFirst()
            .map(profile -> profile.getId().toString())
            .orElse("N/A");
            
        RoleCreationAuditEvent auditEvent = new RoleCreationAuditEvent(
            savedRole.getName(),
            parentApp.getName(),
            savedRole.getDescription(),
            savedRole.getCcmsCode(),
            userTypeRestrictionStr,
            currentUserDto,
            activeUserProfileId,
            currentEntraUser.getEntraOid()
        );
        eventService.logEvent(auditEvent);
        
        log.info("Created new role: {} in app: {}", savedRole.getName(), parentApp.getName());
    }

    private void validateUniqueRoleName(String roleName, UUID appId) {
        Optional<AppRole> existingRole = appRoleRepository.findByName(roleName);
        if (existingRole.isPresent() && existingRole.get().getApp().getId().equals(appId)) {
            throw new IllegalArgumentException("Role name '" + roleName + "' already exists in this application");
        }
    }

    private boolean setAuthzRoleFlag(String roleName, String parentAppName) {
        return parentAppName.equalsIgnoreCase("Manage your users");
    }

    private int generateNextOrdinal() {
        int maxOrdinal = appRoleRepository.findAll()
                .stream()
                .mapToInt(AppRole::getOrdinal)
                .max()
                .orElse(0);
        return maxOrdinal + 1;
    }
}
