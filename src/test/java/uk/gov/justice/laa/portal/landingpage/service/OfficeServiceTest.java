package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficeServiceTest {
    @InjectMocks
    private OfficeService officeService;
    @Mock
    private OfficeRepository officeRepository;

    @Test
    void getOffices() {
        Office office1 = Office.builder().build();
        Office office2 = Office.builder().build();
        List<Office> dbOffices = List.of(office1, office2);
        when(officeRepository.findAll()).thenReturn(dbOffices);
        List<Office> offices = officeService.getOffices();
        assertThat(offices).hasSize(2);
    }

    @Test
    void getOfficesByFirms() {
        UUID officeId = UUID.randomUUID();
        Firm firm1 = Firm.builder().id(officeId).build();
        Office office1 = Office.builder().firm(firm1).build();
        Office office2 = Office.builder().firm(firm1).build();
        List<Office> dbOffices = List.of(office1, office2);
        when(officeRepository.findOfficeByFirm_IdIn(List.of(officeId))).thenReturn(dbOffices);
        List<Office> offices = officeService.getOfficesByFirms(List.of(officeId));
        assertThat(offices).hasSize(2);
    }

    @Test
    public void getOfficeByIdWhenOfficeIsPresent() {
        // Given
        when(officeRepository.findById(any())).thenReturn(Optional.of(Office.builder().build()));
        // When
        Office office = officeService.getOffice(UUID.randomUUID());
        // Then
        assertThat(office).isNotNull();
    }

    @Test
    public void getOfficeByIdWhenOfficeIsNotPresent() {
        // Given
        when(officeRepository.findById(any())).thenReturn(Optional.empty());
        // When
        Office office = officeService.getOffice(UUID.randomUUID());
        // Then
        assertThat(office).isNull();
    }
}