package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;


public class FirmDirectoryTest extends BaseIntegrationTest {

    public static final String FIRM_DIRECTORY_PATH = "/admin/firmDirectory";

    @Resource
    private MockMvc mockMvc;

    @Mock
    private FirmService firmService;

    @Mock
    private OfficeService officeService;


    @Test
    public void accessFirmDirectorySearchScreen() throws Exception {
        mockMvc.perform(get(FIRM_DIRECTORY_PATH)
                .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("firm-directory/search-page"))
                .andExpect(model().attributeExists(ModelAttributes.PAGE_TITLE))
                .andReturn();
    }

    @Test

    void displayFirmDetails_returnsViewAndModel() throws Exception {
        // Arrange
        UUID firmId = UUID.randomUUID();

        // Suppose your criteria binds page/size/sort/direction from query params:
        int page = 2;
        int size = 50;
        String sort = "code";
        String direction = "desc";

        FirmDto mockFirm = FirmDto.builder().id(firmId).name("testFirm").code("A123").build();
        // set fields on mockFirm as needed

        PaginatedOffices mockPaginated = new PaginatedOffices();
        // set fields on mockPaginated as needed

        when(firmService.getFirm(firmId)).thenReturn(mockFirm);
        when(officeService.getOfficesPage(eq(firmId), eq(page), eq(size), eq(sort), eq(direction)))
                .thenReturn(mockPaginated);

        // Act + Assert
        mockMvc.perform(get(FIRM_DIRECTORY_PATH+ "/" + firmId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("sort", sort)
                        .param("direction", direction)
                        .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("firm-directory/firm-offices"));


        // Additionally verify the service interaction with the exact arguments:
        verify(firmService).getFirm(firmId);
        verify(officeService).getOfficesPage(firmId, page, size, sort, direction);
        verifyNoMoreInteractions(firmService, officeService);
    }
}
