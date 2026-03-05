package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficeServiceTest {
    @InjectMocks
    private OfficeService officeService;
    @Mock
    private OfficeRepository officeRepository;

    private ModelMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MapperConfig().modelMapper();
        officeService = new OfficeService(officeRepository, mapper);
    }

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

    @Test
    void getUserOffices() {
        UserProfile up1 = UserProfile.builder().userProfileStatus(UserProfileStatus.COMPLETE).firm(Firm.builder().name("F1").build()).build();
        EntraUser entraUser = EntraUser.builder().userProfiles(Set.of(up1)).build();
        Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("post_code").build();
        Office office1 = Office.builder().code("firm_code").address(address).build();
        when(officeRepository.findOfficeByFirm_IdIn(any())).thenReturn(List.of(office1));
        List<OfficeDto> offices = officeService.getUserOffices(entraUser);
        assertThat(offices).hasSize(1);
        assertThat(offices.getFirst().getCode()).isEqualTo("firm_code");
        assertThat(offices.getFirst().getAddress()).isNotNull();
        OfficeDto.AddressDto addressDto = offices.getFirst().getAddress();
        assertThat(addressDto.getAddressLine1()).isEqualTo("addressLine1");
        assertThat(addressDto.getCity()).isEqualTo("city");
        assertThat(addressDto.getPostcode()).isEqualTo("post_code");
    }


    @Test
    void testGetOfficesByIds_withNullOrEmpty_shouldReturnEmptyList() {
        assertThat(officeService.getOfficesByIds(null)).isEmpty();
        assertThat(officeService.getOfficesByIds(List.of())).isEmpty();
    }

    @Test
    void testGetOfficesByIds_allFound_shouldReturnMappedDtos() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<String> officeIds = List.of(id1.toString(), id2.toString());

        Office office1 = Office.builder().id(id1).code("OFF1").build();
        Office office2 = Office.builder().id(id2).code("OFF2").build();
        OfficeDto dto1 = mapper.map(office1, OfficeDto.class);
        OfficeDto dto2 = mapper.map(office2, OfficeDto.class);

        when(officeRepository.findOfficeByIdIn(List.of(id1, id2))).thenReturn(List.of(office1, office2));

        List<OfficeDto> result = officeService.getOfficesByIds(officeIds);

        assertThat(result.size()).isEqualTo(2);
        assertThat(result).contains(dto1);
        assertThat(result).contains(dto2);
    }

    @Test
    void testGetOfficesByIds_someMissing_shouldThrowException() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<String> officeIds = List.of(id1.toString(), id2.toString());

        Office office1 = Office.builder().id(id1).code("OFF1").build();

        when(officeRepository.findOfficeByIdIn(List.of(id1, id2))).thenReturn(List.of(office1));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                officeService.getOfficesByIds(officeIds));

        assertThat(exception.getMessage()).contains("Failed to load all offices");
    }

    @Test
    void getOfficesPage_ShouldReturnPaginatedOffices() {
        // Arrange
        UUID firmId = UUID.randomUUID();
        int page = 2;
        int pageSize = 5;
        String sort = "code";
        String direction = "ASC";


        Office office = Office.builder()
                .id(UUID.randomUUID())
                .code("OFF123")
                .address(
                        Office.Address.builder()
                                .addressLine1("123 Main St")
                                .city("London")
                                .postcode("SW1A 1AA")
                                .build()
                )
                .build();

        Page<Office> mockPage = new PageImpl<>(
                List.of(office),
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.ASC, sort)),
                10
        );

        when(officeRepository.findAllByFirmId(eq(firmId), any(PageRequest.class)))
                .thenReturn(mockPage);

        // Act
        PaginatedOffices result =
                officeService.getOfficesPage(firmId, page, pageSize, sort, direction);

        // Assert
        // Verify repository was called correctly
        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(officeRepository).findAllByFirmId(eq(firmId), pageRequestCaptor.capture());

        PageRequest pr = pageRequestCaptor.getValue();
        assertThat(pr.getPageNumber()).isEqualTo(page - 1);
        assertThat(pr.getPageSize()).isEqualTo(pageSize);
        assertThat(pr.getSort().getOrderFor(sort).getDirection()).isEqualTo(Sort.Direction.ASC);

        // DTO mapping assertions
        assertThat(result.getOffices()).hasSize(1);

        OfficeDto dto = result.getOffices().get(0);
        assertThat(dto.getId()).isEqualTo(office.getId());
        assertThat(dto.getCode()).isEqualTo("OFF123");
        assertThat(dto.getAddress().getAddressLine1()).isEqualTo("123 Main St");
        assertThat(dto.getAddress().getCity()).isEqualTo("London");
        assertThat(dto.getAddress().getPostcode()).isEqualTo("SW1A 1AA");

        // Pagination metadata
        assertThat(result.getTotalPages()).isEqualTo(mockPage.getTotalPages());
        assertThat(result.getTotalElements()).isEqualTo(mockPage.getTotalElements());
        assertThat(result.getCurrentPage()).isEqualTo(page);
        assertThat(result.getPageSize()).isEqualTo(pageSize);
    }

}