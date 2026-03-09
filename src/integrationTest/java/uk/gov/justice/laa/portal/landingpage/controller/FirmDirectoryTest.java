package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.annotation.Resource;

import jakarta.transaction.Transactional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.ui.ModelMap;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

import uk.gov.justice.laa.portal.landingpage.dto.DisableUserReasonDto;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.UserAccountStatusAuditRepository;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private UserAccountStatusService userAccountStatusService;

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
    public void reasonForDisablePostWithoutError() throws Exception {

        Firm firm1 = buildFirm("Test Firm", "A123");
        Office office1 = buildOffice(firm1, "Test Office", "123 Test Street", "BT12 3AB", "O123");
        firmRepository.saveAndFlush(firm1);
        officeRepository.saveAndFlush(office1);

        UUID firmId = firm1.getId();

        mockMvc.perform(post(FIRM_DIRECTORY_PATH + "/" + firmId + "/reasonForDisable")
                        .with(defaultOauth2Login(defaultLoggedInUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("firm-directory/bulk-confirmation"))
                .andExpect(model().attribute(ModelAttributes.PAGE_TITLE, "Choose a reason to disable access for - " + firm1.getName()));
    }

    @Test
    void post_reasonForDisable_success_renders_confirmation_view() throws Exception {
        // Arrange
        String firmId = String.valueOf(UUID.randomUUID());
        Long selectedReasonId = 7L;

        // Seed the session with a Model carrying "firm" and "reasons"
        ModelMap sessionModel = new ModelMap();
        Firm firm =  buildFirm("Test Firm", "A123");; // or your real Firm type
        sessionModel.addAttribute("firm", firm);
        List<DisableUserReasonDto> reasons = List.of(new DisableUserReasonDto(UUID.randomUUID(), "Fraud", "Fraud"), new DisableUserReasonDto(UUID.randomUUID(), "Other","Other" ));
        sessionModel.addAttribute("reasons", reasons);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("disableUserReasonModel", sessionModel);

        // Mock the counts service
        Map<String, Long> counts = Map.of(
                "totalOfSingleFirm", 5L,
                "totalOfMultiFirm", 2L
        );
        //when(userAccountStatusService.getUserCountsForFirm(any())).thenReturn(counts);

        // Act + Assert
        MvcResult result = mockMvc.perform(post(FIRM_DIRECTORY_PATH + "/" + firmId + "/reasonForDisable")
                        .session(session)
                        .with(defaultOauth2Login(defaultLoggedInUser))
                        .with(csrf())
                        .param("reasonId", String.valueOf(selectedReasonId)))
                .andExpect(status().is3xxRedirection())
                .andReturn();


        // Optional: verify service call
        verify(userAccountStatusService).getUserCountsForFirm(firmId);
    }
}
