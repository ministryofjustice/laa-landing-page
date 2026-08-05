package uk.gov.justice.laa.portal.landingpage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class ReactivationTypeResolver {

    public ReactivationRoleType resolve(EntraUser actor) {
        if (actor == null) {
            log.warn("ReactivationTypeResolver.resolve called with null actor — returning NONE");
            return ReactivationRoleType.NONE;
        }

        if (actor.getUserProfiles() == null) {
            log.warn("ReactivationTypeResolver.resolve called with null userProfiles — returning NONE");
            return ReactivationRoleType.NONE;
        }

        List<String> roleNames = actor.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .map(profile -> Optional.ofNullable(profile.getAppRoles()).orElse(Set.of()).stream()
                        .map(AppRole::getName)
                        .toList())
                .orElse(List.of());

        return resolveFromRoles(roleNames);
    }

    public ReactivationRoleType resolveFromRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return ReactivationRoleType.NONE;
        }

        // Check from highest to lowest delegation
        if (roleNames.contains(AuthzRole.GLOBAL_ADMIN.getRoleName())
                || roleNames.contains(AuthzRole.SECURITY_RESPONSE.getRoleName())) {
            return ReactivationRoleType.LAA;
        }

        if (roleNames.contains(AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())) {
            return ReactivationRoleType.LAA_USER_REGISTRATION;
        }

        if (roleNames.contains(AuthzRole.EXTERNAL_USER_MANAGER.getRoleName())) {
            return ReactivationRoleType.LAA_OST;
        }

        if (roleNames.contains(AuthzRole.FIRM_USER_MANAGER.getRoleName())) {
            return ReactivationRoleType.PROVIDER_ADMIN;
        }

        return ReactivationRoleType.NONE;
    }
}
