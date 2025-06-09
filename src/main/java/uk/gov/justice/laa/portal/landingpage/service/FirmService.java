package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FirmService
 */
@Service
@RequiredArgsConstructor
public class FirmService {
    private final FirmRepository firmRepository;

    public List<Firm> getFirms() {
        return Optional.of(firmRepository.findAll()).orElse(Collections.emptyList());
    }

    public Firm getFirm(String id) {
        return firmRepository.getReferenceById(UUID.fromString(id));
    }
}
