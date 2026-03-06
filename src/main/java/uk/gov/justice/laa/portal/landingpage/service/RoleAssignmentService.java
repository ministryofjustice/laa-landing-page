package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppRoleAssignRestrictionsAuditEvent;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.BaseEntity;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final EventService eventService;

    public boolean canAssignRole(Set<AppRole> editorRoles, Collection<String> targetRoles) {
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

            Set<AppRole> editorRoles = new HashSet<>(userProfile.getAppRoles());

            List<UUID> assigneeRoles = app.getAppRoles().stream()
                    .map(BaseEntity::getId)
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

    public Map<AppRoleDto, List<AppRoleDto>> getLaaAppRoleAssignmentRestrictions() {

        Map<AppRoleDto, List<AppRoleDto>> result = new HashMap<>();
        List<Object[]> rows = roleAssignmentRepository.findAssignableRolesWithAssigningRoles();

        for (Object[] row : rows) {
            AppRoleDto assignableDto = AppRoleDto.builder().id(String.valueOf(row[0])).name(String.valueOf(row[1]))
                    .description(String.valueOf(row[2])).build();
            AppRoleDto assigningDto  = AppRoleDto.builder().id(String.valueOf(row[3])).name(String.valueOf(row[4])).build();

            result.computeIfAbsent(assignableDto, k -> new ArrayList<>()).add(assigningDto);
        }

        return result;
    }

    public Map<AppRoleDto, List<AppRoleDto>> getLaaAppRoleAssignmentRestrictionsByAppName(String appName) {

        Map<AppRoleDto, List<AppRoleDto>> result = new HashMap<>();

        List<Object[]> rows = roleAssignmentRepository.findAssignableRolesWithAssigningRolesByAppName(appName);

        for (Object[] row : rows) {
            AppRoleDto assignableDto = AppRoleDto.builder().id(String.valueOf(row[0])).name(String.valueOf(row[1]))
                    .description(String.valueOf(row[2])).build();
            AppRoleDto assigningDto  = AppRoleDto.builder().id(String.valueOf(row[3])).name(String.valueOf(row[4])).build();

            result.computeIfAbsent(assignableDto, k -> new ArrayList<>()).add(assigningDto);
        }

        return result;
    }

    @Transactional
    public void updateRoleAssignmentRestrictions(CurrentUserDto currentUserDto, String appRoleId, List<String> selectedAssigningRoleIds) {
        UUID targetId = UUID.fromString(appRoleId);
        AppRole targetRole = appRoleRepository.findById(targetId).orElseThrow();

        Set<UUID> desiredAssignerIds = (selectedAssigningRoleIds == null)
                ? Collections.emptySet()
                : selectedAssigningRoleIds.stream()
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (desiredAssignerIds.contains(targetId)) {
            throw new IllegalArgumentException("A role cannot assign itself: " + targetId);
        }

        List<AppRole> assignerRoles = desiredAssignerIds.isEmpty()
                ? List.of()
                : appRoleRepository.findAllById(desiredAssignerIds);

        if (assignerRoles.size() != desiredAssignerIds.size()) {
            Set<UUID> found = assignerRoles.stream().map(AppRole::getId).collect(Collectors.toSet());
            Set<UUID> missing = new HashSet<>(desiredAssignerIds);
            missing.removeAll(found);
            throw new IllegalArgumentException("Assigning roles not found: " + missing);
        }

        roleAssignmentRepository.deleteByAssignableRole(targetRole);

        if (!assignerRoles.isEmpty()) {
            List<RoleAssignment> newRows = assignerRoles.stream()
                    .map(assigner -> RoleAssignment.builder().assigningRole(assigner)
                            .assignableRole(targetRole).build()).toList();

            roleAssignmentRepository.saveAll(newRows);
        }

        UpdateAppRoleAssignRestrictionsAuditEvent event =
                new UpdateAppRoleAssignRestrictionsAuditEvent(currentUserDto, targetRole.getId(), targetRole.getName());
        eventService.logEvent(event);

    }

}
