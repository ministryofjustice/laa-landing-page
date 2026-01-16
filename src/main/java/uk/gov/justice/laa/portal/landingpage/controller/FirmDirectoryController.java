package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditTableSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDirectorySearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.AppRoleService;
import uk.gov.justice.laa.portal.landingpage.service.EmailValidationService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for handling firm-directory requests
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/firmDirectory")
public class FirmDirectoryController {

    private final LoginService loginService;
    private final UserService userService;
    private final OfficeService officeService;
    private final EventService eventService;
    private final FirmService firmService;
    private final ModelMapper mapper;
    private final AccessControlService accessControlService;
    private final RoleAssignmentService roleAssignmentService;
    private final EmailValidationService emailValidationService;
    private final AppRoleService appRoleService;

    public static final String SEARCH_PAGE = "/firm-directory/search-page";

    @GetMapping()
/*    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_EXTERNAL_USER,"
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_INTERNAL_USER)")*/
    public String displayAllFirmDirectory(@ModelAttribute FirmDirectorySearchCriteria criteria,
                                          Model model) {
        log.debug("FirmDirectoryController.displayAllFirmDirectory - {}", criteria);
        // Get audit users
        PaginatedAuditUsers paginatedUsers = PaginatedAuditUsers.builder()
                .users(List.of(AuditUserDto.builder()
                                .email("leo")
                        .build()))
                .build();

                /*userService.getAuditUsers(


                criteria.getSearch(), criteria.getSelectedFirmId(), criteria.getSilasRole(),
                criteria.getSelectedAppId(), criteria.getSelectedUserType(),
                criteria.getPage(), criteria.getSize(), criteria.getSort(), criteria.getDirection());*/
        // Build firm search form
        FirmSearchForm firmSearchForm = new FirmSearchForm(criteria.getFirmSearch(), criteria.getSelectedFirmId());
        // Add attributes to model
        buildDisplayAuditTableModel(criteria, model, paginatedUsers, firmSearchForm);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Firm Directory");

        return SEARCH_PAGE;
    }


    private void buildDisplayAuditTableModel(FirmDirectorySearchCriteria criteria, Model model,
                                             PaginatedAuditUsers paginatedUsers, FirmSearchForm firmSearchForm) {
        model.addAttribute("users", paginatedUsers.getUsers());
        model.addAttribute("requestedPageSize", criteria.getSize());
        model.addAttribute("actualPageSize", paginatedUsers.getUsers().size());
        model.addAttribute("page", criteria.getPage());
        model.addAttribute("totalUsers", paginatedUsers.getTotalUsers());
        model.addAttribute("totalPages", paginatedUsers.getTotalPages());
        model.addAttribute("search", criteria.getSearch());
        model.addAttribute("firmSearch", firmSearchForm);
        // Get all firm type
        List<FirmType> firmTypes = Collections.unmodifiableList(Arrays.asList(
                FirmType.LEGAL_SERVICES_PROVIDER,
                FirmType.CHAMBERS,
                FirmType.ADVOCATE
        ));
        model.addAttribute("firmTypes", firmTypes);

        List<AppDto> apps = userService.getApps();
        model.addAttribute("apps", apps);
/*        //model.addAttribute("selectedSilasRole", criteria.getSilasRole() != null ? criteria.getSilasRole() : "");
        //model.addAttribute("selectedAppId",
                criteria.getSelectedAppId() != null ? criteria.getSelectedAppId().toString() : "");
        model.addAttribute("selectedUserType",
                criteria.getSelectedUserType() != null ? criteria.getSelectedUserType().toString() : "");*/
        model.addAttribute("sort", criteria.getSort());
        model.addAttribute("direction", criteria.getDirection());
    }

    private Map<String, Object> processRequestFilters(int size, int page, String sort, String direction,
                                                      String firmType,
                                                      String search, boolean backButton,
                                                      HttpSession session, FirmSearchForm firmSearchForm) {

        if (backButton) {
            // Use session filters when back button is used
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionFilters = (Map<String, Object>) session.getAttribute("userListFilters");

            if (sessionFilters != null) {
                size = sessionFilters.containsKey("size") ? (Integer) sessionFilters.get("size") : size;
                page = sessionFilters.containsKey("page") ? (Integer) sessionFilters.get("page") : page;
                sort = sessionFilters.containsKey("sort") ? (String) sessionFilters.get("sort") : sort;
                direction = sessionFilters.containsKey("direction") ? (String) sessionFilters.get("direction")
                        : direction;
                //firmStatus = sessionFilters.containsKey("firmStatus") ? (String) sessionFilters.get("firmStatus") : firmStatus;
                firmType = sessionFilters.containsKey("firmType") ? (String) sessionFilters.get("firmType") : firmType;
                search = sessionFilters.containsKey("search") ? (String) sessionFilters.get("search") : search;
                firmSearchForm = sessionFilters.containsKey("firmSearchForm")
                        ? (FirmSearchForm) sessionFilters.get("firmSearchForm")
                        : firmSearchForm;

                // Handle empty strings for optional parameters
                if (sort != null && sort.isEmpty()) {
                    sort = null;
                }
                if (direction != null && direction.isEmpty()) {
                    direction = null;
                }
               /* if (firmStatus != null && firmStatus.isEmpty()) {
                    firmStatus = null;
                }*/
                if (firmType != null && firmType.isEmpty()) {
                    firmType = null;
                }
            }
        } else {
            // Clear session filters when not using back button (new filter request)
            session.removeAttribute("userListFilters");
        }

        Map<String, Object> result = Map.of(
                "size", size,
                "page", page,
                "sort", sort != null ? sort : "",
                "direction", direction != null ? direction : "",
                "search", search != null ? search : "",
                //"firmStatus", firmStatus != null ? firmStatus : "",
                "firmType", firmType != null ? firmType : "",
                "firmSearchForm", firmSearchForm != null ? firmSearchForm : FirmSearchForm.builder().build());

        // Store current filter state in session for future back navigation
        session.setAttribute("userListFilters", result);

        return result;
    }
}
