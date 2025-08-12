package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleAssignmentService {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final AppRoleRepository appRoleRepository;
    private final ModelMapper mapper;

    public boolean canAssignRole(Set<AppRole> editorRoles, List<String> targetRoles) {
        List<UUID> editorRoleIds = editorRoles.stream().map(AppRole::getId).toList();
        List<UUID> targetRoleIds = targetRoles.stream().map(UUID::fromString).distinct().toList();
        List<AppRole> authzRoles = appRoleRepository.findAllByIdInAndAuthzRoleIs(targetRoleIds, true);
        List<UUID> authzRoleIds = authzRoles.stream().map(AppRole::getId).toList();
        Set<UUID> roles = new HashSet<>();
        List<RoleAssignment>  roleAssignments = roleAssignmentRepository.findByAssigningRole_IdIn(editorRoleIds);
        for (RoleAssignment roleAssignment : roleAssignments) {
            if (authzRoleIds.contains(roleAssignment.getAssignableRole().getId())) {
                roles.add(roleAssignment.getAssignableRole().getId());
            }
        }
        if (roles.size() < authzRoles.size()) {
            Set<UUID> targetSet = new HashSet<UUID>(authzRoleIds);
            targetSet.removeAll(roles);
            String ids = targetSet.stream().map(UUID::toString).collect(Collectors.joining(","));
            log.warn("Following roles can not be assigned : {}", ids);
            return false;
        }
        return true;
    }

    public List<AppRoleDto> filterRoles(Set<AppRole> editorRoles, List<AppRoleDto> targetRoles) {
        List<UUID> editorRoleIds = editorRoles.stream().map(AppRole::getId).toList();
        List<UUID> targetRoleIds = targetRoles.stream().map(appRoleDto -> UUID.fromString(appRoleDto.getId())).distinct().toList();
        Set<AppRoleDto> roles = new HashSet<>();
        List<RoleAssignment>  roleAssignments = roleAssignmentRepository.findByAssigningRole_IdIn(editorRoleIds);
        for (RoleAssignment roleAssignment : roleAssignments) {
            if (targetRoleIds.contains(roleAssignment.getAssignableRole().getId())) {
                roles.add(mapper.map(roleAssignment.getAssignableRole(), AppRoleDto.class));
            }
        }
        List<AppRole> nonAuthzRoles = appRoleRepository.findAllByIdInAndAuthzRoleIs(targetRoleIds, false);
        if (!nonAuthzRoles.isEmpty()) {
            roles.addAll(nonAuthzRoles.stream().map(r -> mapper.map(r, AppRoleDto.class)).toList());
        }
        return new ArrayList<>(roles);
    }
}
