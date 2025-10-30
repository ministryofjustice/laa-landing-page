package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppRoleService {

    private final AppRoleRepository appRoleRepository;

    private final ModelMapper modelMapper;

    public List<AppRoleDto> getByIds(List<String> ids) {

        List<UUID> appRoleIds = ids.stream().map(UUID::fromString).toList();

        List<AppRole> appRoles = appRoleRepository.findAllById(appRoleIds);

        if (appRoles.size() != ids.size()) {
            throw new RuntimeException("Failed to load all app roles from request: " + ids);
        }

        return appRoles.stream().map((element) -> modelMapper.map(element, AppRoleDto.class)).toList();
    }

    public List<AppRoleDto> getByAppIdsAndUserRestriction(List<String> appIds, UserType userTypeRestriction) {

        List<UUID> appRoleIds = appIds.stream().map(UUID::fromString).toList();

        List<AppRole> appRoles = appRoleRepository.findByAppIdIUserTypeRestriction(appRoleIds, userTypeRestriction.name());

        if (appRoles.size() != appIds.size()) {
            throw new RuntimeException("Failed to load all app roles by app if from request: " + appIds);
        }

        return appRoles.stream().map((element) -> modelMapper.map(element, AppRoleDto.class)).toList();
    }

}
