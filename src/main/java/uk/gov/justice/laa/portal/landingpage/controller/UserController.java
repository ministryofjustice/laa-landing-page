package uk.gov.justice.laa.portal.landingpage.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import com.microsoft.graph.models.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.Stack;

/**
 * User Controller
 */
@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Add new user via Microsoft Graph API.
     */
    @PostMapping("/register")
    public String addUserToGraph(@RequestParam("username") String username,
                               @RequestParam("password") String password) {
        User user = userService.createUser(username, password);
        return "register";
    }

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/register")
    public String register() {
        return "register";
    }

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/users")
    public String displayAllUsers(@RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) String nextPageLink,
                                  Model model, HttpSession session) {
        
        Stack<String> pageHistory = userService.getPageHistory(session);

        PaginatedUsers paginatedUsers = userService.getPaginatedUsersWithHistory(pageHistory, size, nextPageLink);

        model.addAttribute("users", paginatedUsers.getUsers());
        model.addAttribute("nextPageLink", paginatedUsers.getNextPageLink());
        model.addAttribute("previousPageLink", paginatedUsers.getPreviousPageLink());
        model.addAttribute("pageSize", size);
        model.addAttribute("pageHistory", pageHistory);

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
    @PostMapping("/users/disable")
    public String disableUsers(@RequestParam List<String> id) throws IOException {
        userService.disableUsers(id);
        return "redirect:/users";
    }
}
