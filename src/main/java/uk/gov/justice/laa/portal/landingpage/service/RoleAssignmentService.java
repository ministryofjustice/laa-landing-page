package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleAssignmentService {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final AppRepository appRepository;
    private final AppRoleRepository appRoleRepository;
    private final ModelMapper mapper;

    public boolean canAssignRole(Set<AppRole> editorRoles, List<String> targetRoles) {
        List<UUID> targetRoleIds = targetRoles.stream().map(UUID::fromString).distinct().toList();
        List<AppRoleDto> validRoles = filterRoles(editorRoles, targetRoleIds);
        if (validRoles.size() < targetRoles.size()) {
            Set<String> targetSet = new HashSet<>(targetRoles);
            targetSet.removeAll(validRoles.stream().map(AppRoleDto::getId).collect(Collectors.toSet()));
            log.warn("Following roles can not be assigned : {}", targetSet);
            return false;
        }
        return true;
    }

    public List<AppRoleDto> filterRoles(Set<AppRole> editorRoles, List<UUID> targetRoleIds) {
        List<UUID> editorRoleIds = editorRoles.stream().map(AppRole::getId).toList();
        List<AppRole> authzRoles = appRoleRepository.findAllByIdInAndAuthzRoleIs(targetRoleIds, true);
        List<UUID> authzRoleIds = authzRoles.stream().map(AppRole::getId).toList();
        Set<AppRoleDto> validRoles = new HashSet<>();
        List<RoleAssignment>  roleAssignments = roleAssignmentRepository.findByAssigningRole_IdIn(editorRoleIds);
        for (RoleAssignment roleAssignment : roleAssignments) {
            if (authzRoleIds.contains(roleAssignment.getAssignableRole().getId())) {
                validRoles.add(mapper.map(roleAssignment.getAssignableRole(), AppRoleDto.class));
            }
        }
        List<AppRole> nonAuthzRoles = appRoleRepository.findAllByIdInAndAuthzRoleIs(targetRoleIds, false);
        for (AppRole appRole : nonAuthzRoles) {
            List<RoleAssignment> assignments = roleAssignmentRepository.findByAssignableRole_Id(appRole.getId());
            if (!assignments.isEmpty()) {
                List<AppRole> assigningRoles = assignments.stream().map(RoleAssignment::getAssigningRole).toList();
                if (editorRoles.stream().anyMatch(assigningRoles::contains)) {
                    validRoles.add(mapper.map(appRole, AppRoleDto.class));
                }
            } else {
                validRoles.add(mapper.map(appRole, AppRoleDto.class));
            }
        }
        return new ArrayList<>(validRoles);
    }

    public boolean canUserAssignRolesForApp(UserProfile userProfile, AppDto appDto) {
        Optional<App> appOptional = appRepository.findById(UUID.fromString(appDto.getId()));
        if (appOptional.isPresent()) {
            App app = appOptional.get();

            if (!isAuthzApp(app)) {
                return true;
            }

            Set<AppRole> editorRoles = userProfile.getAppRoles().stream()
                    .filter(appRole -> appRole.getApp().getId().equals(app.getId()))
                    .collect(Collectors.toSet());

            List<UUID> assigneeRoles = app.getAppRoles().stream()
                    .map(appRole -> appRole.getId())
                    .collect(Collectors.toList());

            if (assigneeRoles.isEmpty() || editorRoles.isEmpty()) {
                return false;
            }

            return !filterRoles(editorRoles, assigneeRoles).isEmpty();
        } else {
            log.warn("App not found : {}", appDto.getId());
            return false;
        }
    }

    private boolean isAuthzApp(App app) {
        return app.getAppRoles().stream().anyMatch(AppRole::isAuthzRole);
    }

}
