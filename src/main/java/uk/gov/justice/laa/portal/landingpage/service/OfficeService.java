package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

import java.util.List;
import java.util.UUID;

/**
 * OfficeService
 */
@Service
@RequiredArgsConstructor
public class OfficeService {
    private final OfficeRepository officeRepository;

    public List<Office> getOffices() {
        return officeRepository.findAll();
    }

    public List<Office> getOfficesByFirms(List<UUID> firmIds) {
        return officeRepository.findOfficeByFirm_IdIn(firmIds);
    }
}
