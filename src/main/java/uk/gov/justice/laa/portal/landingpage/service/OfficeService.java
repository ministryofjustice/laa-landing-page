package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDirectoryDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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

    public List<OfficeDto> getOfficesByIds(List<String> officeIds) {
        if (officeIds == null || officeIds.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = officeIds.stream().map(UUID::fromString).toList();
        List<Office> offices = officeRepository.findOfficeByIdIn(ids);

        if (officeIds.size() != offices.size()) {
            throw new RuntimeException("Failed to load all offices from request: " + officeIds);
        }

        return offices.stream().map((element) -> mapper.map(element, OfficeDto.class)).toList();
    }

    public List<OfficeDto> getUserOffices(EntraUser entraUser) {
        List<UUID> firms = entraUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .map(userProfile -> userProfile.getFirm().getId()).toList();
        return getOfficesByFirms(firms)
                .stream().map(office -> mapper.map(office, OfficeDto.class)).toList();
    }

    public PaginatedOffices getOfficesPage(UUID id,
                                               int page, int pageSize, String sort, String direction) {
        Page<Office> officesPage = null;
        PageRequest pageRequest = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by(Sort.Direction.fromString(direction), sort));

        officesPage = officeRepository.findAllByFirmId(id, pageRequest);
        // Map to DTOs

        List<OfficeDto> officeDtos =
                officesPage.getContent().stream()
                        .map(office -> OfficeDto.builder()
                                .code(office.getCode())
                                .id(office.getId())
                                .address(office.getAddress() == null ? null :
                                        OfficeDto.AddressDto.builder()
                                                .addressLine1(office.getAddress().getAddressLine1())
                                                .addressLine2(office.getAddress().getAddressLine2())
                                                .addressLine3(office.getAddress().getAddressLine3())
                                                .city(office.getAddress().getCity())
                                                .postcode(office.getAddress().getPostcode())
                                                .build())
                                .build()).toList();

        return PaginatedOffices.builder()
                .offices(officeDtos)
                .totalPages(officesPage.getTotalPages())
                .totalElements(officesPage.getTotalElements())
                .currentPage(page).pageSize(pageSize).build();
    }
}
