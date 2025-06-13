package uk.gov.justice.laa.portal.landingpage.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

/**
 * FirmService
 */
@Service
@RequiredArgsConstructor
public class FirmService {

    private final FirmRepository firmRepository;
    private final ModelMapper mapper;

    public List<FirmDto> getFirms() {
        return firmRepository.findAll()
                .stream()
                .map(firm -> mapper.map(firm, FirmDto.class))
                .collect(Collectors.toList());
    }

    public FirmDto getFirm(String id) {
        return mapper.map(firmRepository.getReferenceById(UUID.fromString(id)), FirmDto.class);
    }
}
