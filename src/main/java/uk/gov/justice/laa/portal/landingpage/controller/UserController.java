package uk.gov.justice.laa.portal.landingpage.controller;

import com.microsoft.graph.models.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeData;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.ServicePrincipalModel;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.model.UserRole;
import uk.gov.justice.laa.portal.landingpage.service.CreateUserNotificationService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.RandomPasswordGenerator;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getListFromHttpSession;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;

/**
 * User Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CreateUserNotificationService createUserNotificationService;


    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/users")
    public String displayAllUsers(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String nextPageLink,
            Model model, HttpSession session) {

        Stack<String> pageHistory = userService.getPageHistory(session);

        PaginatedUsers paginatedUsers = userService.getPaginatedUsersWithHistory(pageHistory, size, nextPageLink);

        model.addAttribute("users", paginatedUsers.getUsers());
        model.addAttribute("nextPageLink", paginatedUsers.getNextPageLink());
        model.addAttribute("previousPageLink", paginatedUsers.getPreviousPageLink());
        model.addAttribute("pageSize", size);
        model.addAttribute("pageHistory", pageHistory);
        model.addAttribute("page", page);
        model.addAttribute("totalUsers", paginatedUsers.getTotalUsers());
        model.addAttribute("totalPages", paginatedUsers.getTotalPages());

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

        List<UserModel> users = userService.getSavedUsers();
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
        model.addAttribute("user", user);
        model.addAttribute("lastLoggedIn", lastLoggedIn);
        return "manage-user";
    }


    @GetMapping("/user/create/details")
    public String createUser(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new User();
        }
        model.addAttribute("user", user);
        return "user/user-details";
    }

    @PostMapping("/user/create/details")
    public RedirectView postUser(@RequestParam("firstName") String firstName,
                                 @RequestParam("lastName") String lastName,
                                 @RequestParam("email") String email,
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
        return new RedirectView("/user/create/services");
    }

    @GetMapping("/user/create/services")
    public String selectUserApps(Model model, HttpSession session) {
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
        List<ServicePrincipalModel> apps = userService.getServicePrincipals().stream()
                .map(servicePrincipal -> new ServicePrincipalModel(servicePrincipal, selectedApps.contains(servicePrincipal.getAppId())))
                .collect(Collectors.toList());
        model.addAttribute("apps", apps);
        User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
        model.addAttribute("user", user);
        return "add-user-apps";
    }

    @PostMapping("/user/create/services")
    public RedirectView setSelectedApps(@RequestParam("apps") List<String> apps,
                             HttpSession session) {
        session.setAttribute("apps", apps);
        return new RedirectView("/user/create/roles");
    }

    @GetMapping("/user/create/roles")
    public String getSelectedRoles(Model model, HttpSession session) {
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
        List<UserRole> roles = userService.getAllAvailableRolesForApps(selectedApps);
        List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
        for (UserRole role : roles) {
            if (selectedRoles.contains(role.getAppRoleId())) {
                role.setSelected(true);
            }
        }
        model.addAttribute("roles", roles);
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
        OfficeData officeData = getObjectFromHttpSession(session, "officeData", OfficeData.class).orElseGet(OfficeData::new);
        model.addAttribute("officeData", officeData);
        return "user/offices";
    }

    @PostMapping("/user/create/offices")
    public RedirectView postOffices(HttpSession session, @RequestParam(value = "office", required = false) List<String> selectedOffices) {
        OfficeData officeData = new OfficeData();
        officeData.setSelectedOffices(selectedOffices);
        session.setAttribute("officeData", officeData);
        return new RedirectView("/user/create/check-answers");
    }

    @GetMapping("/user/create/check-answers")
    public String addUserCheckAnswers(Model model, HttpSession session) {
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
        if (!selectedApps.isEmpty()) {
            List<UserRole> roles = userService.getAllAvailableRolesForApps(selectedApps);
            List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
            Map<String, List<UserRole>> cyaRoles = new HashMap<>();
            for (UserRole role : roles) {
                if (selectedRoles.contains(role.getAppRoleId())) {
                    List<UserRole> appRoles = cyaRoles.getOrDefault(role.getAppId(), new ArrayList<>());
                    appRoles.add(role);
                    cyaRoles.put(role.getAppId(), appRoles);
                }
            }
            model.addAttribute("roles", cyaRoles);
        }

        User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
        model.addAttribute("user", user);

        OfficeData officeData = getObjectFromHttpSession(session, "officeData", OfficeData.class).orElseGet(OfficeData::new);
        model.addAttribute("officeData", officeData);
        return "add-user-check-answers";
    }

    @PostMapping("/user/create/check-answers")
    //@PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public RedirectView addUserCheckAnswers(HttpSession session) {
        String password = RandomPasswordGenerator.generateRandomPassword(8);
        Optional<User> userOptional = getObjectFromHttpSession(session, "user", User.class);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
            user = userService.createUser(user, password, selectedRoles);
            createUserNotificationService.notifyCreateUser(user.getDisplayName(), user.getMail(), password, user.getId());
        } else {
            log.error("No user attribute was present in request. User not created.");
        }
        session.removeAttribute("roles");
        session.removeAttribute("apps");
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
        List<UserRole> userRoles = userService.getUserAppRolesByUserId(id);
        List<UserRole> availableRoles = userService.getAllAvailableRoles();

        Set<String> userAssignedRoleIds = userRoles.stream()
                .map(UserRole::getAppRoleId)
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
