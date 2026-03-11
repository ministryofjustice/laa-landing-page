package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.annotation.Resource;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.ExtendedModelMap;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

public class FirmDirectoryTest extends BaseIntegrationTest {

    public static final String FIRM_DIRECTORY_PATH = "/admin/firmDirectory";

    @Resource
    private MockMvc mockMvc;

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
    public void accessFirmDrilldownView() throws Exception {
        Firm firm1 = buildFirm("Test Firm", "A123");
        Office office1 = buildOffice(firm1, "Test Office", "123 Test Street", "BT12 3AB", "O123");
        firmRepository.saveAndFlush(firm1);
        officeRepository.saveAndFlush(office1);

        UUID firmId = firm1.getId();

        mockMvc.perform(get(FIRM_DIRECTORY_PATH + "/" + firmId)
                        .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("firm-directory/firm-offices"))
                .andExpect(model().attributeExists("firm"))
                .andExpect(model().attributeExists("firmOffices"))
                .andExpect(model().attributeExists("criteria"))
                .andExpect(model().attribute(ModelAttributes.PAGE_TITLE, "Firm Details"))
                .andReturn();
    }

    @Test
    public void cancel() throws Exception {

        mockMvc.perform(get(FIRM_DIRECTORY_PATH + "/cancel")
                        .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/firmDirectory"))
                .andExpect(request().sessionAttributeDoesNotExist("disableUserReasonModel"));
    }

    @Test
    @Transactional
    public void reasonForDisableGet() throws Exception {

        Firm firm1 = buildFirm("Test Firm", "A123");
        Office office1 = buildOffice(firm1, "Test Office", "123 Test Street", "BT12 3AB", "O123");
        firmRepository.saveAndFlush(firm1);
        officeRepository.saveAndFlush(office1);

        UUID firmId = firm1.getId();

        mockMvc.perform(get(FIRM_DIRECTORY_PATH + "/" + firmId + "/reasonForDisable")
                        .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("firm-directory/bulk-disable-user-reason"))
                .andExpect(model().attributeExists("firm"))
                .andExpect(model().attributeExists("reasons"))
                .andExpect(model().attributeExists("disableUserReasonsForm"))
                .andExpect(model().attribute(ModelAttributes.PAGE_TITLE, "Choose a reason to disable access for - " + firm1.getName()));
    }

    @Test
    @Transactional
    void reasonForDisablePostWithoutError() throws Exception {
        // Arrange
        Firm firm1 = buildFirm("Test Firm", "A123");
        Office office1 = buildOffice(firm1, "Test Office", "123 Test Street", "BT12 3AB", "O123");
        firmRepository.saveAndFlush(firm1);
        officeRepository.saveAndFlush(office1);


        String selectedReasonId = String.valueOf(UUID.randomUUID());

        MockHttpSession httpSession = new MockHttpSession();
        ExtendedModelMap disableUserReasonModel = new ExtendedModelMap();
        disableUserReasonModel.addAttribute("reasonIdSelected", selectedReasonId);
        disableUserReasonModel.addAttribute("firm", FirmDto.builder()
                .id(firm1.getId())
                .name(firm1.getName())
                .code(firm1.getCode())
                .build());
        httpSession.setAttribute("disableUserReasonModel", disableUserReasonModel);
        String firmId = String.valueOf(UUID.randomUUID());
        // Act + Assert
        mockMvc.perform(post(FIRM_DIRECTORY_PATH + "/" + firmId + "/reasonForDisable")
                        .session(httpSession)
                        .with(defaultOauth2Login(defaultLoggedInUser))
                        .with(csrf())
                        .param("reasonId", String.valueOf(selectedReasonId)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("firm-directory/bulk-confirmation"))
                .andExpect(model().attribute(ModelAttributes.PAGE_TITLE, "Remove access for all - " + firm1.getName()));
    }
}
