package uk.gov.justice.laa.portal.landingpage.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.microsoft.graph.models.User;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeData;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.model.OfficeModel;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getListFromHttpSession;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppViewModel;

/**
 * User Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OfficeService officeService;
    private final ModelMapper mapper;
    private final FirmService firmService;

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/users")
    public String displayAllUsers(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer page,
            Model model,
            HttpSession session) {

        // Allow the user to reset the cache by refreshing the page on the /users endpoint.
        if (page == null) {
            page = 1;
            session.setAttribute("cachedUsers", null);
            session.setAttribute("lastResponse", null);
            session.setAttribute("totalUsers", null);
        }

        // Initialise cached user list if not already.
        if (session.getAttribute("cachedUsers") == null) {
            session.setAttribute("cachedUsers", new ArrayList<>());
        }

        PaginatedUsers paginatedUsers = userService.getPaginatedUsers(page, size, session);

        model.addAttribute("users", paginatedUsers.getUsers());
        model.addAttribute("requestedPageSize", size);
        model.addAttribute("actualPageSize", paginatedUsers.getUsers().size());
        model.addAttribute("page", page);
        model.addAttribute("totalUsers", paginatedUsers.getTotalUsers());
        model.addAttribute("totalPages", paginatedUsers.getTotalPages(size));

        return "users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUser(@PathVariable String id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
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
        User user = userService.getUserById(id);
        String lastLoggedIn = userService.getLastLoggedInByUserId(id);
        List<AppRoleDto> userAppRoles = userService.getUserAppRolesByUserId(id);
        List<Office> offices = officeService.getOffices();
        model.addAttribute("user", user);
        model.addAttribute("lastLoggedIn", lastLoggedIn);
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

        Firm firm = firmService.getFirm(firmId);
        FirmDto firmUserDto = new FirmDto();
        firmUserDto.setId(firm.getId());
        firmUserDto.setName(firm.getName());
        session.setAttribute("firm", firmUserDto);
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
                    viewModel.setSelected(selectedRoles.contains(appRoleDto.getId().toString()));
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
    public RedirectView addUserCheckAnswers(HttpSession session) {
        Optional<User> userOptional = getObjectFromHttpSession(session, "user", User.class);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
            Optional<OfficeData> optionalSelectedOfficeData = getObjectFromHttpSession(session, "officeData", OfficeData.class);
            List<String> selectedOffices;
            if (optionalSelectedOfficeData.isPresent()) {
                selectedOffices = optionalSelectedOfficeData.get().getSelectedOffices();
            } else {
                selectedOffices = new ArrayList<>();
            }
            FirmDto selectedFirm =  (FirmDto) session.getAttribute("firm");
            Firm firm = firmService.getFirm(selectedFirm.getId().toString());
            Boolean isFirmAdmin = (Boolean) session.getAttribute("isFirmAdmin");
            userService.createUser(user, selectedRoles, selectedOffices, firm, isFirmAdmin);
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
    public String getUserRoles(@PathVariable String id, Model model) {
        User user = userService.getUserById(id);
        List<AppRoleDto> userRoles = userService.getUserAppRolesByUserId(id);
        List<AppRoleDto> availableRoles = userService.getAllAvailableRoles();

        Set<String> userAssignedRoleIds = userRoles.stream()
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
            @RequestParam(required = false) List<String> selectedRoles) {
        userService.updateUserRoles(id, selectedRoles);
        return new RedirectView("/users");
    }
}
