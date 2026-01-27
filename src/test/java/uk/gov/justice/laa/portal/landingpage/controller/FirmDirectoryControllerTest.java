package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDirectoryDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDirectorySearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmDirectoryControllerTest {

    public static final String SEARCH_PAGE = "/firm-directory/search-page";

    @Mock
    private FirmService firmService;

    private Model model;

    private final List<FirmType> firmTypes = List.of(
            FirmType.LEGAL_SERVICES_PROVIDER,
            FirmType.CHAMBERS,
            FirmType.ADVOCATE);

    @InjectMocks
    private FirmDirectoryController firmDirectoryController;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
    }

    @Test
    void displayAllFirmDirectoryWithoutSearchCriteria() {
        //Arrange
        FirmDirectorySearchCriteria criteria = new FirmDirectorySearchCriteria();

        PaginatedFirmDirectory paginatedFirmDirectory = new PaginatedFirmDirectory();
        paginatedFirmDirectory.setFirmDirectories(List.of());

        when(firmService.getFirmsPage(criteria.getFirmSearch(),
                criteria.getSelectedFirmType(),
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection())).thenReturn(paginatedFirmDirectory);

        //Act
        String result = firmDirectoryController.displayAllFirmDirectory(criteria, model);

        //Assert
        assertThat(result).isEqualTo(SEARCH_PAGE);
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Firm Directory");
        assertThat(model.getAttribute("firmTypes")).isEqualTo(firmTypes);

        assertThat(model.getAttribute("firmDirectories")).isEqualTo(paginatedFirmDirectory.getFirmDirectories());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(criteria.getSize());
        assertThat(model.getAttribute("actualPageSize")).isEqualTo(paginatedFirmDirectory.getFirmDirectories().size());
        assertThat(model.getAttribute("page")).isEqualTo(criteria.getPage());
        assertThat(model.getAttribute("totalRows")).isEqualTo(paginatedFirmDirectory.getTotalElements());
        assertThat(model.getAttribute("totalPages")).isEqualTo(paginatedFirmDirectory.getTotalPages());
        assertThat(model.getAttribute("search")).isEqualTo(criteria.getSearch());
        assertThat(model.getAttribute("firmSearch")).isEqualTo(FirmSearchForm.builder()
                .build());
        assertThat(model.getAttribute("sort")).isEqualTo(criteria.getSort());
        assertThat(model.getAttribute("direction")).isEqualTo(criteria.getDirection());
        assertThat(model.getAttribute("selectedFirmType")).isEqualTo("");
    }

    @Test
    void displayAllFirmDirectoryFindByType() {
        //Arrange
        FirmDirectorySearchCriteria criteria = new FirmDirectorySearchCriteria();
        criteria.setSelectedFirmType(String.valueOf(FirmType.ADVOCATE));
        UUID firmId = UUID.randomUUID();
        List<FirmDirectoryDto> firmDirectoryDtos = List.of(FirmDirectoryDto.builder()
                        .firmType(FirmType.ADVOCATE.getValue())
                        .firmName("Firm Code 1")
                        .firmId(firmId)
                        .firmName("Firm Name 1")
                        .build(),
                FirmDirectoryDto.builder()
                        .firmType(FirmType.ADVOCATE.getValue())
                        .firmName("Firm Code 2")
                        .firmId(firmId)
                        .firmName("Firm Name 2")
                        .build());

        PaginatedFirmDirectory paginatedFirmDirectory = new PaginatedFirmDirectory();
        paginatedFirmDirectory.setFirmDirectories(firmDirectoryDtos);

        when(firmService.getFirmsPage(criteria.getFirmSearch(),
                criteria.getSelectedFirmType(),
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection())).thenReturn(paginatedFirmDirectory);

        //Act
        String result = firmDirectoryController.displayAllFirmDirectory(criteria, model);

        //Assert
        assertThat(result).isEqualTo(SEARCH_PAGE);
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Firm Directory");
        assertThat(model.getAttribute("firmTypes")).isEqualTo(firmTypes);

        assertThat(model.getAttribute("firmDirectories")).isEqualTo(paginatedFirmDirectory.getFirmDirectories());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(criteria.getSize());
        assertThat(model.getAttribute("actualPageSize")).isEqualTo(paginatedFirmDirectory.getFirmDirectories().size());
        assertThat(model.getAttribute("page")).isEqualTo(criteria.getPage());
        assertThat(model.getAttribute("totalRows")).isEqualTo(paginatedFirmDirectory.getTotalElements());
        assertThat(model.getAttribute("totalPages")).isEqualTo(paginatedFirmDirectory.getTotalPages());
        assertThat(model.getAttribute("search")).isEqualTo(criteria.getSearch());
        assertThat(model.getAttribute("firmSearch")).isEqualTo(FirmSearchForm.builder()
                .build());
        assertThat(model.getAttribute("sort")).isEqualTo(criteria.getSort());
        assertThat(model.getAttribute("direction")).isEqualTo(criteria.getDirection());
        assertThat(model.getAttribute("selectedFirmType")).isEqualTo(String.valueOf(FirmType.ADVOCATE));
    }

}