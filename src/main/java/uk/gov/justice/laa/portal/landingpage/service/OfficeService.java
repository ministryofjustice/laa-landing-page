package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OfficeService
 */
@Service
@RequiredArgsConstructor
public class OfficeService {
    private final OfficeRepository officeRepository;
    private final ModelMapper mapper;

    public List<Office> getOffices() {
        return Optional.of(officeRepository.findAll()).orElse(Collections.emptyList());
    }

    public List<Office> getOfficesByFirms(List<UUID> firmIds) {
        return Optional.of(officeRepository.findOfficeByFirm_IdIn(firmIds)).orElse(Collections.emptyList());
    }

    public Office getOffice(UUID id) {
        return officeRepository.findById(id).orElse(null);
    }

    public List<OfficeDto> getUserOffices(EntraUser entraUser) {
        List<UUID> firms = entraUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .map(userProfile -> userProfile.getFirm().getId()).toList();
        return getOfficesByFirms(firms)
                .stream().map(office -> mapper.map(office, OfficeDto.class)).toList();
    }
}
