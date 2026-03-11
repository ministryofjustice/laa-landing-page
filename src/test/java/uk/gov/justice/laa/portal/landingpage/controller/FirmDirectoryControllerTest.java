package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.DisableUserReasonDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDirectoryDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDirectorySearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmOfficesCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserTypeReasonDisable;
import uk.gov.justice.laa.portal.landingpage.forms.DisableUserReasonForm;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmDirectoryControllerTest {

    public static final String SEARCH_PAGE = "firm-directory/search-page";

    @Mock
    private FirmService firmService;

    @Mock
    private OfficeService officeService;

    @Mock
    private UserAccountStatusService userAccountStatusService;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private Authentication authentication;

    @Mock
    private LoginService loginService;

    @Mock
    private HttpSession session;

    @Mock
    private ModelMapper mapper;

    private Model model;

    @InjectMocks
    private FirmDirectoryController firmDirectoryController;

    @BeforeEach
    void setUp() {
        mapper = new MapperConfig().modelMapper();
        model = new ExtendedModelMap();
        ReflectionTestUtils.setField(firmDirectoryController, "firmDirectoryEnabled", true);

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
        assertThat(model.getAttribute("firmTypes")).isEqualTo(FirmType.values());

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
        assertThat(model.getAttribute("firmTypes")).isEqualTo(FirmType.values());

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

    @Test
    void displayFirmDetails() {

        FirmOfficesCriteria criteria = new FirmOfficesCriteria();
        UUID id = UUID.randomUUID();

        PaginatedOffices paginatedOffices = new PaginatedOffices();
        FirmDto firm = FirmDto.builder().id(id).code("A123").name("TestName").build();

        when(officeService.getOfficesPage(
                id,
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection()
        )).thenReturn(paginatedOffices);

        when(firmService.getFirm(id)).thenReturn(firm);
        when(accessControlService.userHasAuthzRole(authentication, AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())).thenReturn(true);
        String result = firmDirectoryController.displayFirmDetails(id, model, criteria, authentication);

        assertThat(result).isEqualTo("firm-directory/firm-offices");
        assertThat(model.getAttribute("firmOffices")).isEqualTo(paginatedOffices);
        assertThat(model.getAttribute("criteria")).isEqualTo(criteria);
        assertThat(model.getAttribute("firm")).isEqualTo(firm);
        assertThat(model.getAttribute("showDisableAllButton")).isEqualTo(false);
    }

    @Test
    void displayFirmDetailsWithDisableButtonAndForExternalUser() {

        FirmOfficesCriteria criteria = new FirmOfficesCriteria();
        UUID id = UUID.randomUUID();

        PaginatedOffices paginatedOffices = new PaginatedOffices();
        FirmDto firm = FirmDto.builder().id(id).code("A123").name("TestName").build();

        when(officeService.getOfficesPage(
                id,
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection()
        )).thenReturn(paginatedOffices);

        when(firmService.getFirm(id)).thenReturn(firm);
        when(accessControlService.userHasAuthzRole(authentication, AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())).thenReturn(true);
        when(userAccountStatusService.hasActiveUserByFirmId(any())).thenReturn(true);
        String result = firmDirectoryController.displayFirmDetails(id, model, criteria, authentication);

        assertThat(result).isEqualTo("firm-directory/firm-offices");
        assertThat(model.getAttribute("firmOffices")).isEqualTo(paginatedOffices);
        assertThat(model.getAttribute("criteria")).isEqualTo(criteria);
        assertThat(model.getAttribute("firm")).isEqualTo(firm);
        assertThat(model.getAttribute("showDisableAllButton")).isEqualTo(true);
    }

    @Test
    void displayFirmDetailsWithDisableButtonAndForGlobalAdmin() {

        FirmOfficesCriteria criteria = new FirmOfficesCriteria();
        UUID id = UUID.randomUUID();

        PaginatedOffices paginatedOffices = new PaginatedOffices();
        FirmDto firm = FirmDto.builder().id(id).code("A123").name("TestName").build();

        when(officeService.getOfficesPage(
                id,
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection()
        )).thenReturn(paginatedOffices);

        when(firmService.getFirm(id)).thenReturn(firm);
        when(accessControlService.userHasAuthzRole(authentication, AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())).thenReturn(false);
        when(accessControlService.userHasAuthzRole(authentication, AuthzRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        when(userAccountStatusService.hasActiveUserByFirmId(any())).thenReturn(true);
        String result = firmDirectoryController.displayFirmDetails(id, model, criteria, authentication);

        assertThat(result).isEqualTo("firm-directory/firm-offices");
        assertThat(model.getAttribute("firmOffices")).isEqualTo(paginatedOffices);
        assertThat(model.getAttribute("criteria")).isEqualTo(criteria);
        assertThat(model.getAttribute("firm")).isEqualTo(firm);
        assertThat(model.getAttribute("showDisableAllButton")).isEqualTo(true);
    }

    @Test
    void displayFirmDetailsWithDisableButtonAndForSecurityResponse() {

        FirmOfficesCriteria criteria = new FirmOfficesCriteria();
        UUID id = UUID.randomUUID();

        PaginatedOffices paginatedOffices = new PaginatedOffices();
        FirmDto firm = FirmDto.builder().id(id).code("A123").name("TestName").build();

        when(officeService.getOfficesPage(
                id,
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection()
        )).thenReturn(paginatedOffices);

        when(firmService.getFirm(id)).thenReturn(firm);
        when(accessControlService.userHasAuthzRole(authentication, AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())).thenReturn(false);
        when(accessControlService.userHasAuthzRole(authentication, AuthzRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(accessControlService.userHasAuthzRole(authentication, AuthzRole.SECURITY_RESPONSE.getRoleName())).thenReturn(true);
        when(userAccountStatusService.hasActiveUserByFirmId(any())).thenReturn(true);
        String result = firmDirectoryController.displayFirmDetails(id, model, criteria, authentication);

        assertThat(result).isEqualTo("firm-directory/firm-offices");
        assertThat(model.getAttribute("firmOffices")).isEqualTo(paginatedOffices);
        assertThat(model.getAttribute("criteria")).isEqualTo(criteria);
        assertThat(model.getAttribute("firm")).isEqualTo(firm);
        assertThat(model.getAttribute("showDisableAllButton")).isEqualTo(true);
    }

    @Test
    void displayReasonForDisableGet() {

        UUID id = UUID.randomUUID();

        FirmDto firm = FirmDto.builder().id(id).code("A123").name("TestName").build();
        DisableUserReasonForm disableUserReasonForm = new DisableUserReasonForm();

        when(firmService.getFirm(String.valueOf(id))).thenReturn(firm);
        when(userAccountStatusService.getDisableUserReasons(UserTypeReasonDisable.BULK_DISABLE)).thenReturn(getDisableUserReasonDtos());

        String result = firmDirectoryController.reasonForDisableGet(String.valueOf(id), disableUserReasonForm, model, session, authentication);

        assertThat(result).isEqualTo("firm-directory/bulk-disable-user-reason");
        assertThat(model.getAttribute("firm")).isEqualTo(firm);
        assertThat(model.getAttribute("reasonIdSelected")).isNull();
        assertThat(model.getAttribute("disableUserReasonsForm")).isEqualTo(disableUserReasonForm);
    }

    @Test
    void displayReasonForDisableGetFromSessionInformation() {

        UUID id = UUID.randomUUID();

        FirmDto firm = FirmDto.builder().id(id).code("A123").name("TestName").build();
        List<DisableUserReasonDto> reasonDtos = getDisableUserReasonDtos();

        MockHttpSession httpSession = new MockHttpSession();
        ExtendedModelMap disableUserReasonModel = new ExtendedModelMap();
        disableUserReasonModel.addAttribute("reasonIdSelected", String.valueOf(reasonDtos.get(0).getId()));
        httpSession.setAttribute("disableUserReasonModel", disableUserReasonModel);

        when(firmService.getFirm(String.valueOf(id))).thenReturn(firm);
        when(userAccountStatusService.getDisableUserReasons(UserTypeReasonDisable.BULK_DISABLE)).thenReturn(reasonDtos);
        DisableUserReasonForm disableUserReasonForm = new DisableUserReasonForm();
        String result = firmDirectoryController.reasonForDisableGet(String.valueOf(id), disableUserReasonForm, model, httpSession, authentication);

        assertThat(result).isEqualTo("firm-directory/bulk-disable-user-reason");
        assertThat(model.getAttribute("firm")).isEqualTo(firm);
        assertThat(model.getAttribute("reasonIdSelected")).isEqualTo(reasonDtos.get(0).getId());
        assertThat(model.getAttribute("disableUserReasonsForm")).isEqualTo(disableUserReasonForm);
    }

    @Test
    void postReasonForDisableFromSessionInformationWithErrors() {
        List<DisableUserReasonDto> reasonDtos = getDisableUserReasonDtos();

        MockHttpSession httpSession = new MockHttpSession();
        ExtendedModelMap disableUserReasonModel = new ExtendedModelMap();
        disableUserReasonModel.addAttribute("reasonIdSelected", String.valueOf(reasonDtos.get(0).getId()));
        httpSession.setAttribute("disableUserReasonModel", disableUserReasonModel);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        DisableUserReasonForm disableUserReasonForm = new DisableUserReasonForm();
        disableUserReasonForm.setReasonId(String.valueOf(reasonDtos.get(0).getId()));
        UUID id = UUID.randomUUID();
        String result = firmDirectoryController.reasonForDisablePost(String.valueOf(id), disableUserReasonForm, bindingResult, authentication, model, httpSession);

        assertThat(result).isEqualTo("firm-directory/bulk-disable-user-reason");
        assertThat(model.getAttribute("disableUserReasonsForm")).isEqualTo(disableUserReasonForm);
        assertThat(model.getAttribute("totalOfSingleFirm")).isNull();
        assertThat(model.getAttribute("totalOfMultiFirm")).isNull();

    }

    @Test
    void postReasonForDisableFromSessionInformationWithNoErrors() {
        List<DisableUserReasonDto> reasonDtos = getDisableUserReasonDtos();

        MockHttpSession httpSession = new MockHttpSession();
        ExtendedModelMap disableUserReasonModel = new ExtendedModelMap();
        disableUserReasonModel.addAttribute("reasonIdSelected", String.valueOf(reasonDtos.get(0).getId()));
        httpSession.setAttribute("disableUserReasonModel", disableUserReasonModel);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        Map<String, Integer> totals = new HashMap<>();
        totals.put("totalOfSingleFirm", 1);
        totals.put("totalOfMultiFirm", 1);
        UUID id = UUID.randomUUID();
        when(userAccountStatusService.getUserCountsForFirm(String.valueOf(id))).thenReturn(totals);
        DisableUserReasonForm disableUserReasonForm = new DisableUserReasonForm();
        disableUserReasonForm.setReasonId(String.valueOf(reasonDtos.get(0).getId()));
        String result = firmDirectoryController.reasonForDisablePost(String.valueOf(id), disableUserReasonForm, bindingResult, authentication, model, httpSession);

        assertThat(result).isEqualTo("firm-directory/bulk-confirmation");
        assertThat(model.getAttribute("disableUserReasonsForm")).isEqualTo(disableUserReasonForm);
        assertThat(model.getAttribute("totalOfSingleFirm")).isEqualTo(1L);
        assertThat(model.getAttribute("totalOfMultiFirm")).isEqualTo(1L);

    }

    @Test
    void confirmationBulkDisablePostWithNoErrors() {
        UUID id = UUID.randomUUID();

        List<DisableUserReasonDto> reasonDtos = getDisableUserReasonDtos();

        MockHttpSession httpSession = new MockHttpSession();
        ExtendedModelMap disableUserReasonModel = new ExtendedModelMap();
        disableUserReasonModel.addAttribute("reasonIdSelected", String.valueOf(reasonDtos.get(0).getId()));
        httpSession.setAttribute("disableUserReasonModel", disableUserReasonModel);

        EntraUser disabledUser = EntraUser.builder()
                .id(id)
                .entraOid(String.valueOf(id))
                .build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(disabledUser);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String result = firmDirectoryController.confirmationBulkDisablePost(String.valueOf(id), model, redirectAttributes, httpSession, authentication);

        assertThat(result).isEqualTo(String.format("redirect:/admin/firmDirectory/%s", id));

        verify(userAccountStatusService, times(1)).disableUserAllUserByFirmId(String.valueOf(id), reasonDtos.get(0).getId(), id);

        assertThat(httpSession.getAttribute("disableUserReasonModel")).isNull();

    }

    @Test
    void cancelBulkUserDisable() {
        MockHttpSession httpSession = new MockHttpSession();
        ExtendedModelMap disableUserReasonModel = new ExtendedModelMap();
        disableUserReasonModel.addAttribute("reasonIdSelected", String.valueOf("1"));
        httpSession.setAttribute("disableUserReasonModel", disableUserReasonModel);

        String result = firmDirectoryController.cancelBulkUserDisable(httpSession);

        assertThat(result).isEqualTo(String.format("redirect:/admin/firmDirectory"));
        assertThat(httpSession.getAttribute("disableUserReasonModel")).isNull();

    }

    @Test
    void confirmationBulkDisablePostWithExceptionError() {
        UUID id = UUID.randomUUID();

        List<DisableUserReasonDto> reasonDtos = getDisableUserReasonDtos();

        MockHttpSession httpSession = new MockHttpSession();
        ExtendedModelMap disableUserReasonModel = new ExtendedModelMap();
        disableUserReasonModel.addAttribute("reasonIdSelected", String.valueOf(reasonDtos.get(0).getId()));
        httpSession.setAttribute("disableUserReasonModel", disableUserReasonModel);

        EntraUser disabledUser = EntraUser.builder()
                .id(id)
                .entraOid(String.valueOf(id))
                .build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(disabledUser);

        doThrow(new RuntimeException("Error processing bulk user"))
                .when(userAccountStatusService)
                .disableUserAllUserByFirmId(eq(String.valueOf(id)), eq(reasonDtos.get(0).getId()), eq(id));


        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        Assertions.assertThrows(RuntimeException.class,
                () -> firmDirectoryController.confirmationBulkDisablePost(String.valueOf(id), model, redirectAttributes, httpSession, authentication));

        verify(userAccountStatusService, times(1)).disableUserAllUserByFirmId(String.valueOf(id), reasonDtos.get(0).getId(), id);

    }

    private static @NotNull List<DisableUserReasonDto> getDisableUserReasonDtos() {
        DisableUserReasonDto reason = DisableUserReasonDto.builder()
                .id(UUID.randomUUID())
                .name("Test Reason")
                .description("A test reason.")
                .build();

        DisableUserReasonDto complianceBreach = DisableUserReasonDto.builder()
                .id(UUID.randomUUID())
                .name("Compliance Breach")
                .description("A compliance breach reason.")
                .build();

        DisableUserReasonDto contractEnded = DisableUserReasonDto.builder()
                .id(UUID.randomUUID())
                .name("Contract Ended")
                .description("A contract ended reason.")
                .build();

        DisableUserReasonDto cyberRisk = DisableUserReasonDto.builder()
                .id(UUID.randomUUID())
                .name("Cyber Risk")
                .description("A cyber risk reason.")
                .build();

        DisableUserReasonDto firmClosureMerger = DisableUserReasonDto.builder()
                .id(UUID.randomUUID())
                .name("Firm Closure / Merger")
                .description("A firm closure / merger reason.")
                .build();

        DisableUserReasonDto investigationPending = DisableUserReasonDto.builder()
                .id(UUID.randomUUID())
                .name("Investigation Pending")
                .description("An investigation pending reason.")
                .build();

        DisableUserReasonDto userRequest = DisableUserReasonDto.builder()
                .id(UUID.randomUUID())
                .name("User Request")
                .description("A user request reason.")
                .build();

        List<DisableUserReasonDto> reasons = List.of(reason,
                complianceBreach,
                contractEnded,
                cyberRisk,
                firmClosureMerger,
                investigationPending,
                userRequest);
        return reasons;
    }
}
