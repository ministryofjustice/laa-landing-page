package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
