package uk.gov.justice.laa.portal.landingpage.service;


import jakarta.persistence.Tuple;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class AuditExportService {

    private final FirmRepository firmRepository;

    public void downloadAuditCsv(String id) {
        List<Tuple> firmData = firmRepository.findFirmUsers();
    }
}
