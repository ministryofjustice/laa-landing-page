package uk.gov.justice.laa.portal.landingpage.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.portal.dto.createuser.CreateUserCommand;
import uk.gov.justice.laa.portal.dto.createuser.CreateUserResult;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.exception.TechServicesClientException;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.UUID;

/**
 * CQRS Command Side: Write endpoints for the create user flow.
 * Called by SiLAS after business logic validation and OPA authorization.
 * Handles TechServices registration and DB persistence.
 */
@Slf4j
@RestController
@RequestMapping("/api/create-user/command")
@RequiredArgsConstructor
public class CreateUserCommandController {

    private final UserService userService;

    /**
     * Execute the create user command.
     * Registers user with TechServices (Entra ID) and persists to DB.
     */
    @PostMapping("/execute")
    public ResponseEntity<CreateUserResult> createUser(@RequestBody CreateUserCommand command) {
        log.info("CQRS Command: creating user with email '{}'", command.getEmail());

        EntraUserDto userDto = new EntraUserDto();
        userDto.setFirstName(command.getFirstName());
        userDto.setLastName(command.getLastName());
        userDto.setFullName(command.getFirstName() + " " + command.getLastName());
        userDto.setEmail(command.getEmail());

        FirmDto firmDto = new FirmDto();
        if (command.getFirmId() != null) {
            firmDto.setId(command.getFirmId());
        } else if (command.isMultiFirmUser()) {
            firmDto.setSkipFirmSelection(true);
        }

        try {
            EntraUser createdUser = userService.createUser(
                    userDto,
                    firmDto,
                    command.isUserManager(),
                    command.getCreatedBy(),
                    command.isMultiFirmUser());

            UUID profileId = createdUser.getUserProfiles() != null
                    ? createdUser.getUserProfiles().stream()
                        .findFirst()
                        .map(UserProfile::getId)
                        .orElse(null)
                    : null;

            return ResponseEntity.ok(CreateUserResult.builder()
                    .success(true)
                    .message("User created successfully")
                    .entraOid(createdUser.getEntraOid())
                    .userProfileId(profileId)
                    .email(command.getEmail())
                    .fullName(command.getFirstName() + " " + command.getLastName())
                    .multiFirmUser(command.isMultiFirmUser())
                    .build());

        } catch (TechServicesClientException e) {
            log.error("TechServices error creating user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CreateUserResult.builder()
                    .success(false)
                    .message(e.getMessage())
                    .email(command.getEmail())
                    .build());
        } catch (Exception e) {
            log.error("Unexpected error creating user: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(CreateUserResult.builder()
                    .success(false)
                    .message("An unexpected error occurred: " + e.getMessage())
                    .email(command.getEmail())
                    .build());
        }
    }
}
