package uk.gov.justice.laa.portal.silas.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.portal.dto.createuser.CreateUserResult;
import uk.gov.justice.laa.portal.dto.createuser.EmailCheckResult;
import uk.gov.justice.laa.portal.dto.createuser.FirmSummaryDto;
import uk.gov.justice.laa.portal.silas.service.CreateUserService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SiLAS Create User Controller.
 * Orchestrates the create user flow using CQRS pattern:
 * - OPA for authorization
 * - User API queries for validation (read side)
 * - User API commands for persistence (write side)
 *
 * This is the entry point for the new architecture.
 * The landing page calls SiLAS (this controller), which then calls User API.
 */
@Slf4j
@RestController
@RequestMapping("/api/create-user")
@RequiredArgsConstructor
public class CreateUserController {

    private final CreateUserService createUserService;

    /**
     * Check if the actor is authorized to create an external user.
     * Uses OPA policy evaluation.
     */
    @PostMapping("/authorize")
    public ResponseEntity<Map<String, Boolean>> authorize(@RequestBody AuthorizeRequest request) {
        boolean authorized = createUserService.isAuthorized(
                request.isActorInternal(), request.permissions());
        if (!authorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("authorized", false));
        }
        return ResponseEntity.ok(Map.of("authorized", true));
    }

    /**
     * Validate email — delegates to User API query endpoint.
     */
    @GetMapping("/validate-email")
    public ResponseEntity<EmailCheckResult> validateEmail(@RequestParam String email) {
        EmailCheckResult result = createUserService.validateEmail(email);
        return ResponseEntity.ok(result);
    }

    /**
     * Search firms — delegates to User API query endpoint.
     */
    @GetMapping("/search-firms")
    public ResponseEntity<List<FirmSummaryDto>> searchFirms(
            @RequestParam String search,
            @RequestParam(defaultValue = "10") int maxResults) {
        List<FirmSummaryDto> results = createUserService.searchFirms(search, maxResults);
        return ResponseEntity.ok(results);
    }

    /**
     * Execute create user — validates authorization via OPA, then issues
     * CQRS command to User API.
     */
    @PostMapping("/execute")
    public ResponseEntity<CreateUserResult> executeCreateUser(@RequestBody CreateUserRequest request) {
        // Re-check authorization before executing the command
        if (!createUserService.isAuthorized(request.isActorInternal(), request.actorPermissions())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(CreateUserResult.builder()
                            .success(false)
                            .message("Not authorized to create external users")
                            .build());
        }

        CreateUserResult result = createUserService.executeCreateUser(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.isUserManager(),
                request.isMultiFirmUser(),
                request.firmId(),
                request.createdBy());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    // Request records

    public record AuthorizeRequest(boolean isActorInternal, Set<String> permissions) {}

    public record CreateUserRequest(
            String firstName,
            String lastName,
            String email,
            boolean isUserManager,
            boolean isMultiFirmUser,
            UUID firmId,
            String createdBy,
            boolean isActorInternal,
            Set<String> actorPermissions) {}
}
