package uk.gov.justice.laa.portal.landingpage.controller;

import com.microsoft.graph.models.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.dto.*;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.model.OfficeModel;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.service.*;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppViewModel;

import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getListFromHttpSession;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;

/**
 * User Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class UserController {

    private final LoginService loginService;
    private final UserService userService;
    private final OfficeService officeService;
    private final EventService eventService;
    private final FirmService firmService;
    private final ModelMapper mapper;

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/users")
    public String displayAllUsers(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String search,
            Model model) {

        PaginatedUsers paginatedUsers;
        if (search != null && !search.isEmpty()) {
            paginatedUsers = userService.getPageOfUsersByNameOrEmail(page, size, search);
        } else {
            search = null;
            paginatedUsers = userService.getPageOfUsers(page, size);
        }

        model.addAttribute("users", paginatedUsers.getUsers());
        model.addAttribute("requestedPageSize", size);
        model.addAttribute("actualPageSize", paginatedUsers.getUsers().size());
        model.addAttribute("page", page);
        model.addAttribute("totalUsers", paginatedUsers.getTotalUsers());
        model.addAttribute("totalPages", paginatedUsers.getTotalPages());
        model.addAttribute("search", search);

        return "users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUser(@PathVariable String id, Model model) {
        Optional<EntraUserDto> optionalUser = userService.getEntraUserById(id);
        if (optionalUser.isPresent()) {
            EntraUserDto user = optionalUser.get();
            List<AppRoleDto> roles = userService.getUserAppRolesByUserId(user.getId());
            model.addAttribute("user", user);
            model.addAttribute("roles", roles);
        }
        return "edit-user";
    }

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/userlist")
    public String displaySavedUsers(Model model) {
        List<EntraUserDto> users = userService.getSavedUsers();
        model.addAttribute("users", users);
        return "users";
    }

    /**
     * Disable group of users via graph SDK
     */
    @PreAuthorize("hasAuthority('SCOPE_User.EnableDisableAccount.All')")
    @PostMapping("/users/disable")
    public String disableUsers(@RequestParam("disable-user") List<String> id) throws IOException {
        userService.disableUsers(id);
        return "redirect:/users";
    }

    /**
     * Manage user via graph SDK
     */
    @GetMapping("/users/manage/{id}")
    public String manageUser(@PathVariable String id, Model model) {
        Optional<EntraUserDto> optionalUser = userService.getEntraUserById(id);
        List<AppRoleDto> userAppRoles = userService.getUserAppRolesByUserId(id);
        List<Office> offices = officeService.getOffices();
        optionalUser.ifPresent(user -> model.addAttribute("user", user));
        model.addAttribute("userAppRoles", userAppRoles);
        model.addAttribute("offices", offices);
        return "manage-user";
    }

    @GetMapping("/user/create/details")
    public String createUser(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new User();
        }
        List<FirmDto> firms = firmService.getFirms();
        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        model.addAttribute("firms", firms);
        model.addAttribute("selectedFirm", selectedFirm);
        model.addAttribute("user", user);
        return "user/user-details";
    }

    @PostMapping("/user/create/details")
    public RedirectView postUser(@RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String firmId,
            @RequestParam(required = false) String isFirmAdmin,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new User();
        }
        user.setGivenName(firstName);
        user.setSurname(lastName);
        user.setDisplayName(firstName + " " + lastName);
        user.setMail(email);
        session.setAttribute("user", user);

        FirmDto firm = firmService.getFirm(firmId);

        session.setAttribute("firm", firm);
        session.setAttribute("isFirmAdmin",  Boolean.parseBoolean(isFirmAdmin));

        return new RedirectView("/user/create/services");
    }

    @GetMapping("/user/create/services")
    public String selectUserApps(Model model, HttpSession session) {
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
        List<AppViewModel> apps = userService.getApps().stream()
                .map(appDto -> {
                    AppViewModel appViewModel = mapper.map(appDto, AppViewModel.class);
                    appViewModel.setSelected(selectedApps.contains(appDto.getId()));
                    return appViewModel;
                }).toList();
        model.addAttribute("apps", apps);
        User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
        model.addAttribute("user", user);
        return "add-user-apps";
    }

    @PostMapping("/user/create/services")
    public RedirectView setSelectedApps(@RequestParam List<String> apps,
            HttpSession session) {
        session.setAttribute("apps", apps);
        return new RedirectView("/user/create/roles");
    }

    @GetMapping("/user/create/roles")
    public String getSelectedRoles(Model model, HttpSession session) {
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
        List<AppRoleDto> roles = userService.getAllAvailableRolesForApps(selectedApps);
        List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
        List<AppRoleViewModel> appRoleViewModels = roles.stream()
                .map(appRoleDto -> {
                    AppRoleViewModel viewModel = mapper.map(appRoleDto, AppRoleViewModel.class);
                    viewModel.setSelected(selectedRoles.contains(appRoleDto.getId()));
                    return viewModel;
                }).toList();
        model.addAttribute("roles", appRoleViewModels);
        return "add-user-roles";
    }

    @PostMapping("/user/create/roles")
    public RedirectView setSelectedRoles(@RequestParam("selectedRoles") List<String> roles,
            HttpSession session) {
        session.setAttribute("roles", roles);
        return new RedirectView("/user/create/offices");
    }

    @GetMapping("/user/create/offices")
    public String offices(HttpSession session, Model model) {
        OfficeData selectedOfficeData = getObjectFromHttpSession(session, "officeData", OfficeData.class).orElseGet(OfficeData::new);
        //if user has firms, use officeService.getOfficesByFirms();
        List<Office> offices = officeService.getOffices();
        List<OfficeModel> officeData = offices.stream()
                .map(office -> new OfficeModel(office.getName(), office.getAddress(),
                office.getId().toString(), Objects.nonNull(selectedOfficeData.getSelectedOffices())
                && selectedOfficeData.getSelectedOffices().contains(office.getId().toString())))
                .collect(Collectors.toList());
        model.addAttribute("officeData", officeData);
        User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
        model.addAttribute("user", user);
        return "user/offices";
    }

    @PostMapping("/user/create/offices")
    public RedirectView postOffices(HttpSession session, @RequestParam(value = "offices") List<String> selectedOffices) {
        OfficeData officeData = new OfficeData();
        officeData.setSelectedOffices(selectedOffices);
        //if user has firms, use officeService.getOfficesByFirms();
        List<Office> offices = officeService.getOffices();
        List<String> selectedDisplayNames = new ArrayList<>();
        for (Office office : offices) {
            if (Objects.nonNull(selectedOffices)
                    && selectedOffices.contains(office.getId().toString())) {
                selectedDisplayNames.add(office.getName());
            }
        }
        officeData.setSelectedOfficesDisplay(selectedDisplayNames);
        session.setAttribute("officeData", officeData);
        return new RedirectView("/user/create/check-answers");
    }

    @GetMapping("/user/create/check-answers")
    public String addUserCheckAnswers(Model model, HttpSession session) {
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
        if (!selectedApps.isEmpty()) {
            List<AppRoleDto> roles = userService.getAllAvailableRolesForApps(selectedApps);
            List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
            Map<String, List<AppRoleViewModel>> cyaRoles = new HashMap<>();
            for (AppRoleDto role : roles) {
                if (selectedRoles.contains(role.getId())) {
                    List<AppRoleViewModel> appRoles = cyaRoles.getOrDefault(role.getApp().getId(), new ArrayList<>());
                    appRoles.add(mapper.map(role, AppRoleViewModel.class));
                    cyaRoles.put(role.getApp().getId(), appRoles);
                }
            }
            model.addAttribute("roles", cyaRoles);
        }

        User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
        model.addAttribute("user", user);

        OfficeData officeData = getObjectFromHttpSession(session, "officeData", OfficeData.class).orElseGet(OfficeData::new);
        model.addAttribute("officeData", officeData);

        FirmDto selectedFirm =  (FirmDto) session.getAttribute("firm");
        model.addAttribute("firm", selectedFirm);

        Boolean isFirmAdmin = (Boolean) session.getAttribute("isFirmAdmin");
        model.addAttribute("isFirmAdmin", isFirmAdmin);
        return "add-user-check-answers";
    }

    @PostMapping("/user/create/check-answers")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public RedirectView addUserCheckAnswers(HttpSession session, Authentication authentication) {
        Optional<User> userOptional = getObjectFromHttpSession(session, "user", User.class);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
            Optional<OfficeData> optionalSelectedOfficeData = getObjectFromHttpSession(session, "officeData", OfficeData.class);
            List<String> selectedOffices;
            List<String> selectedOfficesDisplay;
            if (optionalSelectedOfficeData.isPresent()) {
                selectedOffices = optionalSelectedOfficeData.get().getSelectedOffices();
                selectedOfficesDisplay = optionalSelectedOfficeData.get().getSelectedOfficesDisplay();
            } else {
                selectedOffices = new ArrayList<>();
                selectedOfficesDisplay = new ArrayList<>();
            }
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            FirmDto selectedFirm =  (FirmDto) session.getAttribute("firm");
            Boolean isFirmAdmin = (Boolean) session.getAttribute("isFirmAdmin");
            EntraUser entraUser = userService.createUser(user, selectedRoles, selectedOffices, selectedFirm, isFirmAdmin, currentUserDto.getName());
            eventService.auditUserCreate(currentUserDto, entraUser, selectedRoles, selectedOfficesDisplay, selectedFirm.getName());
        } else {
            log.error("No user attribute was present in request. User not created.");
        }
        session.removeAttribute("user");
        session.removeAttribute("firm");
        session.removeAttribute("isFirmAmdin");
        session.removeAttribute("apps");
        session.removeAttribute("roles");
        session.removeAttribute("officeData");
        return new RedirectView("/users");
    }

    @GetMapping("/user/create/confirmation")
    public String addUserCreated(Model model, HttpSession session) {
        Optional<User> userOptional = getObjectFromHttpSession(session, "user", User.class);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            model.addAttribute("user", user);
        } else {
            log.error("No user attribute was present in request. User not added to model.");
        }
        return "add-user-created";
    }

    /**
     * Retrieves available user roles for user
     */
    @GetMapping("/users/edit/{id}/roles")
    public String editUserRoles(@PathVariable String id, Model model, HttpSession session) {
        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        List<String> selectedApps = getListFromHttpSession(session, "selectedApps", String.class).orElseGet(ArrayList::new);
        List<AppRoleDto> userRoles = userService.getUserAppRolesByUserId(id);
        List<AppRoleDto> availableRoles = userService.getAppRolesByAppIds(selectedApps);

        Set<String> userAssignedRoleIds = userRoles.stream()
                .filter(availableRoles::contains)
                .map(AppRoleDto::getId)
                .collect(Collectors.toSet());

        model.addAttribute("user", user);
        model.addAttribute("availableRoles", availableRoles);
        model.addAttribute("userAssignedRoles", userAssignedRoleIds);

        return "edit-user-roles";
    }

    /**
     * Update user roles via graph SDK
     */
    @PostMapping("/users/edit/{id}/roles")
    public RedirectView updateUserRoles(@PathVariable String id,
            @RequestParam(required = false) List<String> selectedRoles,
            Authentication authentication) {
        EntraUserDto user = userService.getEntraUserById(id).orElse(null);
        userService.updateUserRoles(id, selectedRoles);
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        eventService.auditUpdateRole(currentUserDto, user, selectedRoles);
        return new RedirectView("/admin/users");
    }

    /**
     * Retrieves available apps for user and their currently assigned apps.
     */
    @GetMapping("/users/edit/{id}/apps")
    public String editUserApps(@PathVariable String id, Model model) {
        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        Set<AppDto> userAssignedApps = userService.getUserAppsByUserId(id);
        List<AppDto> availableApps = userService.getApps();

        model.addAttribute("user", user);
        model.addAttribute("userAssignedApps", userAssignedApps);
        model.addAttribute("availableApps", availableApps);

        return "edit-user-apps";
    }

    @PostMapping("/users/edit/{id}/apps")
    public RedirectView setSelectedAppsEdit(@PathVariable String id, @RequestParam("selectedApps") List<String> apps,
                                         HttpSession session) {
        session.setAttribute("selectedApps", apps);
        // Ensure passed in ID is a valid UUID to avoid open redirects.
        UUID uuid = UUID.fromString(id);
        return new RedirectView(String.format("/admin/users/edit/%s/roles", uuid));
    }
}
