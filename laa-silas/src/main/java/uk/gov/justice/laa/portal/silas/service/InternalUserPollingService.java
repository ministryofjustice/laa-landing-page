package uk.gov.justice.laa.portal.silas.service;

import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.DirectoryObjectCollectionResponse;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.dto.InternalUserPollRequestDto;
import uk.gov.justice.laa.portal.dto.InternalUserPollResultDto;
import uk.gov.justice.laa.portal.silas.client.UserApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalUserPollingService {

    private final GraphServiceClient graphClient;
    private final UserApiClient userApiClient;

    @Value("${internal.user.polling.group.id}")
    private String pollingGroupId;

    private static final String MISSING_NAME = "MISSING";

    public void pollForNewUsers() {
        log.info("Starting internal user polling (v2 service)...");

        List<UUID> existingUserOids = userApiClient.getInternalUserEntraIds();

//        List<DirectoryObject> allGraphUsers = fetchAllGroupMembers();
//        if (allGraphUsers.isEmpty()) {
//            log.warn("No users found in internal group from Graph API");
//            return;
//        }
//
//        log.info("Found {} users in Graph group, {} existing internal users in DB",
//                allGraphUsers.size(), existingUserOids.size());
//
//        deleteRemovedUsers(allGraphUsers, existingUserOids);
//        createNewUsers(allGraphUsers, existingUserOids);

        log.info("Internal user polling (v2 service) completed");
    }

    private List<DirectoryObject> fetchAllGroupMembers() {
        DirectoryObjectCollectionResponse response = graphClient.groups()
                .byGroupId(pollingGroupId)
                .members().get();

        if (response == null || response.getValue() == null) {
            log.warn("No response for users in internal group");
            return List.of();
        }

        List<DirectoryObject> allUsers = new ArrayList<>(response.getValue());
        String nextLink = response.getOdataNextLink();
        int page = 1;

        while (nextLink != null) {
            response = graphClient
                    .groups()
                    .byGroupId(pollingGroupId)
                    .members()
                    .withUrl(nextLink)
                    .get();

            if (response != null && response.getValue() != null) {
                allUsers.addAll(response.getValue());
                nextLink = response.getOdataNextLink();
                page++;
                log.debug("Page {} of users retrieved from entra group", page);
            } else {
                break;
            }
        }

        return allUsers;
    }

    private void createNewUsers(List<DirectoryObject> allGraphUsers, List<UUID> existingUserOids) {
        List<DirectoryObject> newUsers = allGraphUsers.stream()
                .filter(u -> !existingUserOids.contains(UUID.fromString(u.getId())))
                .toList();

        if (newUsers.isEmpty()) {
            log.info("No new users found to add.");
            return;
        }

        log.info("Creating {} new internal users via User API (individual calls)", newUsers.size());
        int successCount = 0;
        int failCount = 0;

        for (DirectoryObject obj : newUsers) {
            if (obj instanceof User user) {
                InternalUserPollRequestDto request = InternalUserPollRequestDto.builder()
                        .entraOid(user.getId())
                        .email(user.getMail())
                        .firstName(user.getGivenName() != null ? user.getGivenName() : MISSING_NAME)
                        .lastName(user.getSurname() != null ? user.getSurname() : MISSING_NAME)
                        .build();

                InternalUserPollResultDto result = userApiClient.createInternalUser(request);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                    log.warn("Failed to create user {}: {}", request.getEntraOid(), result.getMessage());
                }
            }
        }

        log.info("User creation complete. Success: {}, Failed: {}", successCount, failCount);
    }

    private void deleteRemovedUsers(List<DirectoryObject> allGraphUsers, List<UUID> existingUserOids) {
        List<UUID> graphApiUserOids = allGraphUsers.stream()
                .map(u -> UUID.fromString(u.getId()))
                .toList();

        List<UUID> usersToDelete = existingUserOids.stream()
                .filter(existingUserOid -> !graphApiUserOids.contains(existingUserOid))
                .toList();

        if (usersToDelete.isEmpty()) {
            log.info("No internal users found for deletion");
            return;
        }

        log.info("Deleting {} internal users via User API (individual calls)", usersToDelete.size());
        int successCount = 0;
        int failCount = 0;

        for (UUID entraId : usersToDelete) {
            InternalUserPollResultDto result = userApiClient.deleteInternalUser(entraId);
            if (result.isSuccess()) {
                successCount++;
            } else {
                failCount++;
                log.warn("Failed to delete user {}: {}", entraId, result.getMessage());
            }
        }

        log.info("User deletion complete. Success: {}, Failed: {}", successCount, failCount);
    }
}
