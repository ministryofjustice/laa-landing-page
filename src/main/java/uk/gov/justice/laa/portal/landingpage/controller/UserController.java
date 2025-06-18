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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.microsoft.graph.models.User;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeData;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.forms.ApplicationsForm;
import uk.gov.justice.laa.portal.landingpage.forms.OfficesForm;
import uk.gov.justice.laa.portal.landingpage.forms.RolesForm;
import uk.gov.justice.laa.portal.landingpage.forms.UserDetailsForm;
import uk.gov.justice.laa.portal.landingpage.model.OfficeModel;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.UserUtils;

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
@RequestMapping("/admin")
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
    public String createUser(UserDetailsForm userDetailsForm, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new User();
        }
        List<FirmDto> firms = firmService.getFirms();
        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        model.addAttribute("firms", firms);
        model.addAttribute("selectedFirm", selectedFirm);
        // If user is already in session, populate the form with existing user details
        userDetailsForm = UserUtils.populateUserDetailsFormWithSession(userDetailsForm, user, session);
        model.addAttribute("userDetailsForm", userDetailsForm);
        model.addAttribute("user", user);
        model.addAttribute("userDetailsForm", userDetailsForm);
        model.addAttribute("user", user);
        return "add-user-details";
    }

    @PostMapping("/user/create/details")
    public String postUser(
            @Valid UserDetailsForm userDetailsForm, BindingResult result,
            @RequestParam(required = false) String isFirmAdmin,
            HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new User();
        }

        // Set user details from the form
        user.setGivenName(userDetailsForm.getFirstName());
        user.setSurname(userDetailsForm.getLastName());
        user.setDisplayName(userDetailsForm.getFirstName() + " " + userDetailsForm.getLastName());
        user.setMail(userDetailsForm.getEmail());
        session.setAttribute("user", user);

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while creating user: {}", result.getAllErrors());

            // If there are validation errors, return to the user details page with errors
            List<FirmDto> firms = firmService.getFirms();
            FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
            model.addAttribute("firms", firms);
            model.addAttribute("selectedFirm", selectedFirm);
            model.addAttribute("user", user);
            return "add-user-details";
        }

        // Set firm and admin status
        FirmDto firm = firmService.getFirm(userDetailsForm.getFirmId());
        session.setAttribute("firm", firm);
        session.setAttribute("isFirmAdmin", userDetailsForm.getIsFirmAdmin());

        return "redirect:/admin/user/create/services";
    }

    @GetMapping("/user/create/services")
    public String selectUserApps(ApplicationsForm applicationsForm, Model model, HttpSession session) {
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
    public String setSelectedApps(ApplicationsForm applicationsForm, Model model,
            HttpSession session
    ) {
        if (applicationsForm.getApps() == null || applicationsForm.getApps().isEmpty()) {
            return "redirect:/admin/user/create/check-answers";
        }
        session.setAttribute("apps", applicationsForm.getApps());
        return "redirect:/admin/user/create/roles";
    }

    @GetMapping("/user/create/roles")
    public String getSelectedRoles(RolesForm rolesForm, Model model, HttpSession session) {
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
        List<AppRoleDto> roles = userService.getAllAvailableRolesForApps(selectedApps);
        List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
        List<AppRoleViewModel> appRoleViewModels = roles.stream()
                .map(appRoleDto -> {
                    AppRoleViewModel viewModel = mapper.map(appRoleDto, AppRoleViewModel.class);
                    viewModel.setSelected(selectedRoles.contains(appRoleDto.getId()));
                    return viewModel;
                }).toList();
        User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
        model.addAttribute("user", user);
        model.addAttribute("roles", appRoleViewModels);
        return "add-user-roles";
    }

    @PostMapping("/user/create/roles")
    public String setSelectedRoles(@Valid RolesForm rolesForm, BindingResult result,
            Model model, HttpSession session) {
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while setting user roles: {}", result.getAllErrors());

            List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
            List<AppRoleDto> roles = userService.getAllAvailableRolesForApps(selectedApps);
            List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
            List<AppRoleViewModel> appRoleViewModels = roles.stream()
                    .map(appRoleDto -> {
                        AppRoleViewModel viewModel = mapper.map(appRoleDto, AppRoleViewModel.class);
                        viewModel.setSelected(selectedRoles.contains(appRoleDto.getId()));
                        return viewModel;
                    }).toList();
            User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
            model.addAttribute("user", user);
            model.addAttribute("roles", appRoleViewModels);
            return "add-user-roles";
        }
        session.setAttribute("roles", rolesForm.getRoles());
        return "redirect:/admin/user/create/offices";
    }

    @GetMapping("/user/create/offices")
    public String offices(OfficesForm officesForm, HttpSession session, Model model) {
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
        return "add-user-offices";
    }

    @PostMapping("/user/create/offices")
    public String postOffices(@Valid OfficesForm officesForm, BindingResult result, Model model, HttpSession session
    ) {

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while selecting offices: {}", result.getAllErrors());
            // If there are validation errors, return to the offices page with errors
            OfficeData selectedOfficeData = getObjectFromHttpSession(session, "officeData", OfficeData.class).orElseGet(OfficeData::new);
            //if user has firms, use officeService.getOfficesByFirms();
            List<Office> offices = officeService.getOffices();
            List<OfficeModel> officeDataList = offices.stream()
                    .map(office -> new OfficeModel(office.getName(), office.getAddress(),
                    office.getId().toString(), Objects.nonNull(selectedOfficeData.getSelectedOffices())
                    && selectedOfficeData.getSelectedOffices().contains(office.getId().toString())))
                    .collect(Collectors.toList());
            model.addAttribute("officeData", officeDataList);
            User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
            model.addAttribute("user", user);
            return "add-user-offices";
        }

        OfficeData officeData = new OfficeData();
        List<String> selectedOffices = officesForm.getOffices();
        officeData.setSelectedOffices(officesForm.getOffices());
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
        return "redirect:/admin/user/create/check-answers";
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

        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
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
            FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
            Boolean isFirmAdmin = (Boolean) session.getAttribute("isFirmAdmin");
            userService.createUser(user, selectedRoles, selectedOffices, selectedFirm, isFirmAdmin);
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
