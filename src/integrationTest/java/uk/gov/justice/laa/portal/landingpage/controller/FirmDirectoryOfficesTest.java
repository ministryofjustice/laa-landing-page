package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

public class FirmDirectoryOfficesTest extends BaseIntegrationTest {

    private static final String FIRM_OFFICES_PATH = "/admin/firmDirectory/";

    @Resource
    MockMvc mockMvc;

    @MockitoBean
    private FirmService firmService;

    @Mock
    OfficeService officeService;

    @Test
    public void testDisplayFirmDetails() throws Exception {
        UUID firmId = UUID.randomUUID();

        FirmDto firmDto = FirmDto.builder().id(firmId).name("Test Firm").build();

        PaginatedOffices paginatedOffices = new PaginatedOffices();

        when(firmService.getFirm(firmId)).thenReturn(firmDto);
        when(officeService.getOfficesPage(
                eq(firmId),
                anyInt(),
                anyInt(),
                anyString(),
                anyString()
        )).thenReturn(paginatedOffices);

        mockMvc.perform(get(FIRM_OFFICES_PATH + firmId)
                        .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("firm-directory/firm-offices"))
                .andExpect(model().attributeExists(ModelAttributes.PAGE_TITLE))
                .andReturn();
    }
}
