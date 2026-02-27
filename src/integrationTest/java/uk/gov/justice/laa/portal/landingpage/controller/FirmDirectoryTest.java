package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.annotation.Resource;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    @Transactional
    public void testDisplayFirmDetails() throws Exception {

        Firm firm1 = buildFirm("Test Firm", "A123");
        firmRepository.saveAndFlush(firm1);

        UUID firmId = firm1.getId();

        PaginatedOffices paginatedOffices = new PaginatedOffices();

        when(officeService.getOfficesPage(
                eq(firmId),
                anyInt(),
                anyInt(),
                anyString(),
                anyString()
        )).thenReturn(paginatedOffices);

        mockMvc.perform(get(FIRM_DIRECTORY_PATH + "/" + firmId)
                        .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("firm-directory/firm-offices"))
                .andExpect(model().attributeExists(ModelAttributes.PAGE_TITLE))
                .andReturn();
    }
}
