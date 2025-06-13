package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmServiceTest {

    @InjectMocks
    private FirmService firmService;
    @Mock
    private FirmRepository firmRepository;

    @Test
    void getFirms() {
        Firm firm1 = Firm.builder().build();
        Firm firm2 = Firm.builder().build();
        List<Firm> dbFirms = List.of(firm1, firm2);
        when(firmRepository.findAll()).thenReturn(dbFirms);
        List<FirmDto> firms = firmService.getFirms();
        assertThat(firms).hasSize(2);
    }

    @Test
    void getFirm() {
        UUID firmId = UUID.randomUUID();
        Firm dbFirm = Firm.builder().id(firmId).build();
        when(firmRepository.getReferenceById(firmId)).thenReturn(dbFirm);
        Firm firm = firmService.getFirm(firmId.toString());
        assertThat(firm.getId()).isEqualTo(firmId);
    }
}