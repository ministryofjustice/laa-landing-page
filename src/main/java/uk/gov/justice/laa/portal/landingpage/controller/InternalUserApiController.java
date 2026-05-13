package uk.gov.justice.laa.portal.landingpage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/internal-users")
@RequiredArgsConstructor
public class InternalUserApiController {

    private final UserService userService;

    @GetMapping("/entra-ids")
    public ResponseEntity<List<UUID>> getInternalUserEntraIds() {
        log.debug("API: Fetching internal user Entra IDs");
        List<UUID> entraIds = userService.getInternalUserEntraIds();
        return ResponseEntity.ok(entraIds);
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createInternalUser(@RequestBody EntraUserDto userDto) {
        log.info("API: Creating internal user with Entra OID: {}", userDto.getEntraOid());
        try {
            userDto.setUserStatus(UserStatus.ACTIVE);
            int count = userService.createInternalPolledUser(List.of(userDto));
            boolean success = count > 0;
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "User created" : "User skipped (already exists or external)",
                    "entraOid", userDto.getEntraOid()
            ));
        } catch (Exception e) {
            log.error("API: Failed to create internal user {}: {}", userDto.getEntraOid(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "entraOid", userDto.getEntraOid()
            ));
        }
    }

    @DeleteMapping("/delete/{entraId}")
    public ResponseEntity<Map<String, Object>> deleteInternalUser(@PathVariable UUID entraId) {
        log.info("API: Deleting internal user with Entra ID: {}", entraId);
        try {
            int count = userService.deleteInternalUsersByEntraIds(List.of(entraId));
            boolean success = count > 0;
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "User deleted" : "User not found or not internal",
                    "entraOid", entraId.toString()
            ));
        } catch (Exception e) {
            log.error("API: Failed to delete internal user {}: {}", entraId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "entraOid", entraId.toString()
            ));
        }
    }
}
