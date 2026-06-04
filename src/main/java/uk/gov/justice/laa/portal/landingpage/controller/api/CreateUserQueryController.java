package uk.gov.justice.laa.portal.landingpage.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.portal.dto.createuser.EmailCheckResult;
import uk.gov.justice.laa.portal.dto.createuser.FirmSummaryDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.service.EmailValidationService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.List;

/**
 * CQRS Query Side: Read-only endpoints for the create user flow.
 * Called by SiLAS to validate data before issuing commands.
 */
@Slf4j
@RestController
@RequestMapping("/api/create-user/query")
@RequiredArgsConstructor
public class CreateUserQueryController {

    private final UserService userService;
    private final EmailValidationService emailValidationService;
    private final FirmService firmService;

    /**
     * Check if an email address is available and has a valid domain.
     */
    @GetMapping("/check-email")
    public ResponseEntity<EmailCheckResult> checkEmail(@RequestParam String email) {
        log.debug("CQRS Query: checking email availability for '{}'", email);

        boolean exists = userService.userExistsByEmail(email);
        boolean isMultiFirm = exists && userService.isMultiFirmUserByEmail(email);
        boolean validDomain = emailValidationService.isValidEmailDomain(email);

        String message = null;
        if (exists && isMultiFirm) {
            message = "This email address is already registered as a multi-firm user";
        } else if (exists) {
            message = "Email address already exists";
        } else if (!validDomain) {
            message = "The email address domain is not valid or cannot receive emails.";
        }

        return ResponseEntity.ok(EmailCheckResult.builder()
                .available(!exists)
                .validDomain(validDomain)
                .isMultiFirmUser(isMultiFirm)
                .message(message)
                .build());
    }

    /**
     * Search firms by name (for firm selection step).
     */
    @GetMapping("/firms")
    public ResponseEntity<List<FirmSummaryDto>> searchFirms(
            @RequestParam String search,
            @RequestParam(defaultValue = "10") int maxResults) {
        log.debug("CQRS Query: searching firms with term '{}'", search);

        List<FirmDto> allFirms = firmService.getAllFirmsFromCache();
        List<FirmSummaryDto> results = allFirms.stream()
                .filter(f -> f.getName().toLowerCase().contains(search.toLowerCase()))
                .limit(maxResults)
                .map(f -> FirmSummaryDto.builder()
                        .id(f.getId())
                        .name(f.getName())
                        .code(f.getCode())
                        .build())
                .toList();

        return ResponseEntity.ok(results);
    }
}
