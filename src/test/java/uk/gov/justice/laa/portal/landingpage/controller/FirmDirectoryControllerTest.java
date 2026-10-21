package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
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
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.firmdirectory.FirmDirectoryProvider;
import uk.gov.justice.laa.portal.landingpage.service.firmdirectory.FirmDirectoryProviderResolver;

import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmDirectoryControllerTest {

    public static final String SEARCH_PAGE = "firm-directory/search-page";

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private Authentication authentication;

    @Mock
    private LoginService loginService;

    @Mock
    private HttpSession session;

    @Mock
    private FirmDirectoryProviderResolver firmDirectoryProviderResolver;

    @Mock
    private FirmDirectoryProvider firmDirectoryProvider;

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
        ReflectionTestUtils.setField(firmDirectoryController, "bulkUserDisableFeatureEnabled", true);

    }

    @Test
    void displayAllFirmDirectoryWithoutSearchCriteria() {
        //Arrange
        FirmDirectorySearchCriteria criteria = new FirmDirectorySearchCriteria();

        PaginatedFirmDirectory paginatedFirmDirectory = new PaginatedFirmDirectory();
        paginatedFirmDirectory.setFirmDirectories(List.of());
        paginatedFirmDirectory.setCurrentPage(criteria.getPage());

        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getFirmsPage(criteria.getFirmSearch(),
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
                        .firmId(firmId.toString())
                        .firmName("Firm Name 1")
                        .build(),
                FirmDirectoryDto.builder()
                        .firmType(FirmType.ADVOCATE.getValue())
                        .firmName("Firm Code 2")
                        .firmId(firmId.toString())
                        .firmName("Firm Name 2")
                        .build());

        PaginatedFirmDirectory paginatedFirmDirectory = new PaginatedFirmDirectory();
        paginatedFirmDirectory.setFirmDirectories(firmDirectoryDtos);
        paginatedFirmDirectory.setCurrentPage(criteria.getPage());

        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getFirmsPage(criteria.getFirmSearch(),
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
    void displayAllFirmDirectory_setsDataApiEnabledModelAttributeFromFeatureFlag() {
        FirmDirectorySearchCriteria criteria = new FirmDirectorySearchCriteria();
        criteria.setFirmSearch("abc");
        PaginatedFirmDirectory page = new PaginatedFirmDirectory();
        page.setFirmDirectories(List.of());
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getFirmsPage("abc", criteria.getSelectedFirmType(), criteria.getPage(),
                criteria.getSize(), criteria.getSort(), criteria.getDirection())).thenReturn(page);

        String result = firmDirectoryController.displayAllFirmDirectory(criteria, model);

        assertThat(result).isEqualTo(SEARCH_PAGE);
        verify(firmDirectoryProvider).getFirmsPage("abc", criteria.getSelectedFirmType(), criteria.getPage(),
                criteria.getSize(), criteria.getSort(), criteria.getDirection());
    }

    @Test
    void displayAllFirmDirectory_passesFirmTypeFilterToProvider() {
        FirmDirectorySearchCriteria criteria = new FirmDirectorySearchCriteria();
        criteria.setSelectedFirmType(String.valueOf(FirmType.ADVOCATE));
        PaginatedFirmDirectory page = new PaginatedFirmDirectory();
        page.setFirmDirectories(List.of());
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getFirmsPage(criteria.getFirmSearch(), criteria.getSelectedFirmType(),
                criteria.getPage(), criteria.getSize(), criteria.getSort(), criteria.getDirection()))
                .thenReturn(page);

        String result = firmDirectoryController.displayAllFirmDirectory(criteria, model);

        assertThat(result).isEqualTo(SEARCH_PAGE);
        verify(firmDirectoryProvider).getFirmsPage(criteria.getFirmSearch(), criteria.getSelectedFirmType(),
                criteria.getPage(), criteria.getSize(), criteria.getSort(), criteria.getDirection());
    }

    @Test
    void displayAllFirmDirectory_passesSortAndDirectionToProvider() {
        FirmDirectorySearchCriteria criteria = new FirmDirectorySearchCriteria();
        criteria.setSort("code");
        criteria.setDirection("desc");
        PaginatedFirmDirectory page = new PaginatedFirmDirectory();
        page.setFirmDirectories(List.of());
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getFirmsPage(null, criteria.getSelectedFirmType(), criteria.getPage(),
                criteria.getSize(), "code", "desc")).thenReturn(page);

        firmDirectoryController.displayAllFirmDirectory(criteria, model);

        verify(firmDirectoryProvider).getFirmsPage(null, criteria.getSelectedFirmType(), criteria.getPage(),
                criteria.getSize(), "code", "desc");
    }

    @Test
    void displayAllFirmDirectory_usesProviderReportedCurrentPageForView() {
        FirmDirectorySearchCriteria criteria = new FirmDirectorySearchCriteria();
        criteria.setPage(6);
        PaginatedFirmDirectory page = new PaginatedFirmDirectory();
        page.setFirmDirectories(List.of());
        page.setCurrentPage(2);
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getFirmsPage(null, criteria.getSelectedFirmType(), criteria.getPage(),
                criteria.getSize(), criteria.getSort(), criteria.getDirection())).thenReturn(page);

        firmDirectoryController.displayAllFirmDirectory(criteria, model);

        assertThat(model.getAttribute("page")).isEqualTo(2);
    }

    @Test
    void displayFirmDetailsWithoutDisableAllButtonWhenHasActiveUserByFirmIdIsFalse() {

        FirmOfficesCriteria criteria = new FirmOfficesCriteria();
        UUID id = UUID.randomUUID();

        PaginatedOffices paginatedOffices = new PaginatedOffices();
        FirmDto firm = FirmDto.builder().id(id).code("A123").name("TestName").build();

        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getOfficesPage(
                id.toString(),
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection()
        )).thenReturn(paginatedOffices);

        when(firmDirectoryProvider.getFirm(id.toString())).thenReturn(firm);
        when(accessControlService.authenticatedUserHasPermission(Permission.BULK_DISABLE_FIRM_USERS)).thenReturn(true);
        when(firmDirectoryProvider.hasActiveUsers(id.toString())).thenReturn(false);
        String result = firmDirectoryController.displayFirmDetails(id.toString(), model, criteria, authentication);

        assertThat(result).isEqualTo("firm-directory/firm-offices");
        assertThat(model.getAttribute("firmOffices")).isEqualTo(paginatedOffices);
        assertThat(model.getAttribute("criteria")).isEqualTo(criteria);
        assertThat(model.getAttribute("firm")).isEqualTo(firm);
        assertThat(model.getAttribute("showDisableAllButton")).isEqualTo(false);
    }

    @Test
    void displayFirmDetailsWithDisableAllButtonForPermissionBulkDisableFirmUsers() {

        FirmOfficesCriteria criteria = new FirmOfficesCriteria();
        UUID id = UUID.randomUUID();

        PaginatedOffices paginatedOffices = new PaginatedOffices();
        FirmDto firm = FirmDto.builder().id(id).code("A123").name("TestName").build();

        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getOfficesPage(
                id.toString(),
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection()
        )).thenReturn(paginatedOffices);

        when(firmDirectoryProvider.getFirm(id.toString())).thenReturn(firm);
        when(accessControlService.authenticatedUserHasPermission(Permission.BULK_DISABLE_FIRM_USERS)).thenReturn(true);
        when(firmDirectoryProvider.hasActiveUsers(id.toString())).thenReturn(true);
        String result = firmDirectoryController.displayFirmDetails(id.toString(), model, criteria, authentication);

        assertThat(result).isEqualTo("firm-directory/firm-offices");
        assertThat(model.getAttribute("firmOffices")).isEqualTo(paginatedOffices);
        assertThat(model.getAttribute("criteria")).isEqualTo(criteria);
        assertThat(model.getAttribute("firm")).isEqualTo(firm);
        assertThat(model.getAttribute("showDisableAllButton")).isEqualTo(true);
    }

    @Test
    void displayFirmDetailsWithoutDisableButtonAndForUserWithoutPermission() {

        FirmOfficesCriteria criteria = new FirmOfficesCriteria();
        UUID id = UUID.randomUUID();

        PaginatedOffices paginatedOffices = new PaginatedOffices();
        FirmDto firm = FirmDto.builder().id(id).code("A123").name("TestName").build();

        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getOfficesPage(
                id.toString(),
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection()
        )).thenReturn(paginatedOffices);

        when(firmDirectoryProvider.getFirm(id.toString())).thenReturn(firm);
        when(accessControlService.authenticatedUserHasPermission(any())).thenReturn(false);
        String result = firmDirectoryController.displayFirmDetails(id.toString(), model, criteria, authentication);

        assertThat(result).isEqualTo("firm-directory/firm-offices");
        assertThat(model.getAttribute("firmOffices")).isEqualTo(paginatedOffices);
        assertThat(model.getAttribute("criteria")).isEqualTo(criteria);
        assertThat(model.getAttribute("firm")).isEqualTo(firm);
        assertThat(model.getAttribute("showDisableAllButton")).isEqualTo(false);
    }

    @Test
    void displayFirmDetails_withFirmCode_usesProviderForButtonAndUserSearchLink() {
        String id = "25966";
        UUID localId = UUID.randomUUID();
        FirmDto firm = FirmDto.builder().code(id).name("Firm").build();
        PaginatedOffices offices = new PaginatedOffices();
        FirmOfficesCriteria criteria = new FirmOfficesCriteria();
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getFirm(id)).thenReturn(firm);
        when(firmDirectoryProvider.getOfficesPage(id, criteria.getPage(), criteria.getSize(),
                criteria.getSort(), criteria.getDirection())).thenReturn(offices);
        when(firmDirectoryProvider.findLocalFirmId(id)).thenReturn(Optional.of(localId));
        when(accessControlService.authenticatedUserHasPermission(Permission.BULK_DISABLE_FIRM_USERS)).thenReturn(true);
        when(firmDirectoryProvider.hasActiveUsers(id)).thenReturn(true);

        String result = firmDirectoryController.displayFirmDetails(id, model, criteria, authentication);

        assertThat(result).isEqualTo("firm-directory/firm-offices");
        assertThat(model.getAttribute("firmRef")).isEqualTo(id);
        assertThat(model.getAttribute("userSearchFirmId")).isEqualTo(localId);
        assertThat(model.getAttribute("showDisableAllButton")).isEqualTo(true);
        assertThat(model.getAttribute("firm")).isSameAs(firm);
        assertThat(model.getAttribute("firmOffices")).isSameAs(offices);
    }

    @Test
    void confirmationBulkDisablePostWithNoErrors() {
        UUID id = UUID.randomUUID();

        EntraUser disabledUser = EntraUser.builder()
                .id(id)
                .entraOid(String.valueOf(id))
                .build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(disabledUser);
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.validateFirmId(id.toString())).thenReturn(id.toString());
        doAnswer(invocation -> {
            Supplier<UUID> disabledByUserIdSupplier = invocation.getArgument(1);
            disabledByUserIdSupplier.get();
            return null;
        }).when(firmDirectoryProvider).bulkDeactivateFirmUsers(eq(id.toString()), any());

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String result = firmDirectoryController.confirmationBulkDisablePost(String.valueOf(id), model, redirectAttributes, authentication);

        assertThat(result).isEqualTo(String.format("redirect:/admin/firmDirectory/%s", id));

        verify(firmDirectoryProvider, times(1)).bulkDeactivateFirmUsers(eq(id.toString()), any());
        verify(loginService, times(1)).getCurrentEntraUser(authentication);
    }

    @Test
    void confirmationBulkDisablePostWithException() {
        UUID id = UUID.randomUUID();
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.validateFirmId(id.toString())).thenReturn(id.toString());
        doThrow(new RuntimeException("Test exception"))
                .when(firmDirectoryProvider).bulkDeactivateFirmUsers(eq(id.toString()), any());
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        Assertions.assertThrows(RuntimeException.class,
                () -> firmDirectoryController.confirmationBulkDisablePost(String.valueOf(id), model, redirectAttributes, authentication));

        verify(firmDirectoryProvider, times(1)).bulkDeactivateFirmUsers(eq(id.toString()), any());
    }

    @Test
    void cancelBulkUserDisable() {
        String result = firmDirectoryController.cancelBulkUserDisable();

        assertThat(result).isEqualTo("redirect:/admin/firmDirectory");
    }

    @Test
    void confirmationBulkDisablePostWithExceptionError() {
        UUID id = UUID.randomUUID();
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.validateFirmId(id.toString())).thenReturn(id.toString());
        doThrow(new RuntimeException("Test exception"))
                .when(firmDirectoryProvider).bulkDeactivateFirmUsers(eq(id.toString()), any());
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        Assertions.assertThrows(RuntimeException.class,
                () -> firmDirectoryController.confirmationBulkDisablePost(String.valueOf(id), model, redirectAttributes, authentication));

        verify(firmDirectoryProvider, times(1)).bulkDeactivateFirmUsers(eq(id.toString()), any());
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

        return List.of(reason,
                complianceBreach,
                contractEnded,
                cyberRisk,
                firmClosureMerger,
                investigationPending,
                userRequest);
    }

    @Test
    void confirmationBulkDisablePost_doesNotEagerlyResolveDisablingUserWhenProviderIgnoresSupplier() {
        UUID id = UUID.randomUUID();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.validateFirmId(id.toString())).thenReturn(id.toString());

        String result = firmDirectoryController.confirmationBulkDisablePost(
                id.toString(), model, redirectAttributes, authentication);

        assertThat(result).isEqualTo("redirect:/admin/firmDirectory/" + id);
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
        verify(firmDirectoryProvider).bulkDeactivateFirmUsers(eq(id.toString()), any());
        verifyNoInteractions(loginService);
    }

    @Test
    void bulkDisableConfirmation_worksForFirmCode() {
        String firmCode = "25966";
        UUID localId = UUID.randomUUID();
        FirmDto firm = FirmDto.builder().code(firmCode).name("TestName").build();
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getFirm(firmCode)).thenReturn(firm);
        when(firmDirectoryProvider.findLocalFirmId(firmCode)).thenReturn(Optional.of(localId));
        when(firmDirectoryProvider.getUserCounts(firmCode))
                .thenReturn(Map.of("totalOfSingleFirm", 1L, "totalOfMultiFirm", 2L));

        String result = firmDirectoryController.bulkDisableConfirmation(firmCode, model);

        assertThat(result).isEqualTo("firm-directory/bulk-confirmation");
        assertThat(model.getAttribute("firm")).isSameAs(firm);
        assertThat(model.getAttribute("firmRef")).isEqualTo(firmCode);
        assertThat(model.getAttribute("userSearchFirmId")).isEqualTo(localId);
        assertThat(model.getAttribute("totalOfSingleFirm")).isEqualTo(1L);
        assertThat(model.getAttribute("totalOfMultiFirm")).isEqualTo(2L);
    }

    @Test
    void bulkDisableConfirmation_withoutLocalFirmShowsZeroCounts() {
        String firmCode = "25966";
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.getFirm(firmCode))
                .thenReturn(FirmDto.builder().code(firmCode).name("TestName").build());
        when(firmDirectoryProvider.getUserCounts(firmCode)).thenReturn(Map.of());

        firmDirectoryController.bulkDisableConfirmation(firmCode, model);

        assertThat(model.getAttribute("userSearchFirmId")).isNull();
        assertThat(model.getAttribute("totalOfSingleFirm")).isEqualTo(0L);
        assertThat(model.getAttribute("totalOfMultiFirm")).isEqualTo(0L);
    }

    @Test
    void confirmationBulkDisablePost_usesFirmCodeOnApiPath() {
        String firmCode = "25966";
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.validateFirmId(firmCode)).thenReturn(firmCode);

        String result = firmDirectoryController.confirmationBulkDisablePost(
                firmCode, model, redirectAttributes, authentication);

        assertThat(result).isEqualTo("redirect:/admin/firmDirectory/" + firmCode);
        verify(firmDirectoryProvider).bulkDeactivateFirmUsers(eq(firmCode), any());
    }

    @Test
    void confirmationBulkDisablePost_propagatesReauthenticationRequired() {
        String firmCode = "25966";
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.validateFirmId(firmCode)).thenReturn(firmCode);
        doThrow(new ClientAuthorizationRequiredException("azure"))
                .when(firmDirectoryProvider).bulkDeactivateFirmUsers(eq(firmCode), any());

        assertThatThrownBy(() -> firmDirectoryController.confirmationBulkDisablePost(
                firmCode, model, redirectAttributes, authentication))
                .isInstanceOf(ClientAuthorizationRequiredException.class);
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("successMessage");
    }

    @Test
    void confirmationBulkDisablePost_returnsNotFoundForMalformedFirmId() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.validateFirmId("not-a-uuid"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Firm not found"));

        assertThatThrownBy(() -> firmDirectoryController.confirmationBulkDisablePost(
                "not-a-uuid", model, redirectAttributes, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(firmDirectoryProvider, never()).bulkDeactivateFirmUsers(any(), any());
    }

    @Test
    void confirmationBulkDisablePost_showsErrorBoxOnApiBadRequest() {
        UUID id = UUID.randomUUID();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.validateFirmId(id.toString())).thenReturn(id.toString());
        doThrow(new UserDataApiClientException("bad", 400))
                .when(firmDirectoryProvider).bulkDeactivateFirmUsers(eq(id.toString()), any());

        String result = firmDirectoryController.confirmationBulkDisablePost(
                id.toString(), model, redirectAttributes, authentication);

        assertThat(result).isEqualTo("redirect:/admin/firmDirectory/" + id);
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("successMessage");
    }

    @Test
    void confirmationBulkDisablePost_propagatesApiServerErrorsWithoutSuccess() {
        UUID id = UUID.randomUUID();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.validateFirmId(id.toString())).thenReturn(id.toString());
        doThrow(new UserDataApiClientException("down", 503))
                .when(firmDirectoryProvider).bulkDeactivateFirmUsers(eq(id.toString()), any());

        assertThatThrownBy(() -> firmDirectoryController.confirmationBulkDisablePost(
                id.toString(), model, redirectAttributes, authentication))
                .isInstanceOf(UserDataApiClientException.class);
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("successMessage");
    }

    @Test
    void handleDataApiException_forbiddenRedirectsToNotAuthorised() {
        Object result = firmDirectoryController.handleDataApiException(
                new UserDataApiClientException("x", 403), new MockHttpServletRequest("GET", "/admin/firmDirectory"));

        assertThat(result).isInstanceOf(RedirectView.class);
        assertThat(((RedirectView) result).getUrl()).isEqualTo("/not-authorised");
    }

    @Test
    void handleDataApiException_notFoundOnGetShowsNotFoundPage() {
        Object result = firmDirectoryController.handleDataApiException(
                new UserDataApiClientException("x", 404), new MockHttpServletRequest("GET", "/admin/firmDirectory/1"));

        ModelAndView mav = (ModelAndView) result;
        assertThat(mav.getViewName()).isEqualTo("errors/error-404");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleDataApiException_notFoundOnPostShowsFirmNotFoundMessage() {
        Object result = firmDirectoryController.handleDataApiException(
                new UserDataApiClientException("x", 404),
                new MockHttpServletRequest("POST", "/admin/firmDirectory/1/confirmation"));

        ModelAndView mav = (ModelAndView) result;
        assertThat(mav.getViewName()).isEqualTo("errors/error-generic");
        assertThat(mav.getModel().get("errorTitle")).isEqualTo("Firm not found");
    }

    @Test
    void handleDataApiException_downstreamFailuresShowGenericErrorWithoutTechnicalDetail() {
        for (int status : new int[] {502, 503, 504}) {
            Object result = firmDirectoryController.handleDataApiException(
                    new UserDataApiClientException("Data API returned status " + status, status),
                    new MockHttpServletRequest("POST", "/admin/firmDirectory/1/confirmation"));

            ModelAndView mav = (ModelAndView) result;
            assertThat(mav.getViewName()).isEqualTo("errors/error-generic");
            assertThat(mav.getModel().get("errorMessage").toString()).doesNotContain(String.valueOf(status));
        }
    }

    @Test
    void searchFirms_usesSelectedProvider() {
        List<FirmDto> expected = List.of(FirmDto.builder().code("25966").name("Firm A").build());
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.searchFirms("abc")).thenReturn(expected);

        assertThat(firmDirectoryController.searchFirms(" abc ").getBody()).isSameAs(expected);
    }

    @Test
    void searchFirms_blankQueryReturnsEmptyWithoutCallingProvider() {
        assertThat(firmDirectoryController.searchFirms("  ").getBody()).isEmpty();
        verifyNoInteractions(firmDirectoryProviderResolver);
    }

    @Test
    void searchFirms_returnsUnauthorizedWhenReauthenticationRequired() {
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
        when(firmDirectoryProvider.searchFirms("abc"))
                .thenThrow(new ClientAuthorizationRequiredException("azure"));

        assertThat(firmDirectoryController.searchFirms("abc").getStatusCode().value()).isEqualTo(401);
    }
}
