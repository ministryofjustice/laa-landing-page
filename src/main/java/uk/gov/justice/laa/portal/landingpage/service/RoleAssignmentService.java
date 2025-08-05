package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.repository.RoleAssignmentRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleAssignmentService {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final ModelMapper mapper;

    public boolean canAssignRole(Set<AppRole> editorRoles, List<String> targetRoles) {
        List<UUID> editorRoleIds = editorRoles.stream().map(AppRole::getId).toList();
        List<UUID> targetRoleIds = targetRoles.stream().map(UUID::fromString).distinct().toList();
        Set<UUID> roles = new HashSet<>();
        List<RoleAssignment>  roleAssignments = roleAssignmentRepository.findByAssigningRole_IdIn(editorRoleIds);
        for (RoleAssignment roleAssignment : roleAssignments) {
            if (targetRoleIds.contains(roleAssignment.getAssignableRole().getId())) {
                roles.add(roleAssignment.getAssignableRole().getId());
            }
        }
        if (roles.size() < targetRoles.size()) {
            Set<UUID> targetSet = new HashSet<UUID>(targetRoleIds);
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
        return new ArrayList<>(roles);
    }
}
