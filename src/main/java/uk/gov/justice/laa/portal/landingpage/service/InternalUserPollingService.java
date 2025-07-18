package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.DirectoryObjectCollectionResponse;
import com.microsoft.graph.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class InternalUserPollingService {

    private final GraphServiceClient graphClient;
    private final UserService userService;

    @Value("${internal.user.polling.group.id}")
    private String pollingGroupId;
    private static final String MISSING_NAME = "MISSING";

    Logger logger = LoggerFactory.getLogger(InternalUserPollingService.class);

    public void pollForNewUsers() {
        List<UUID> existingUserOids = userService.getInternalUserEntraIds();
        DirectoryObjectCollectionResponse response = graphClient.groups()
                .byGroupId(pollingGroupId)
                .members().get();

        if (response == null || response.getValue() == null) {
            logger.warn("No response for users in internal group");
            return;
        }

        List<DirectoryObject> allUsers = response.getValue();
        List<DirectoryObject> newUsers = allUsers.stream()
                .filter(u -> !existingUserOids.contains(UUID.fromString(u.getId())))
                .toList();

        if (newUsers.isEmpty()) {
            logger.info("No new users found to add.");
            return;
        }

        List<EntraUserDto> entraUserDtoList = extractEntraUserDtos(newUsers);
        if (!entraUserDtoList.isEmpty()) {
            logger.info("Inserting {} new internal users.", entraUserDtoList.size());
            int savedUserCount = userService.createInternalPolledUser(entraUserDtoList);
            logger.info("Inserted {} new internal users.", savedUserCount);
        } else {
            logger.info("No valid User objects found among new users.");
        }
    }

    private List<EntraUserDto> extractEntraUserDtos(List<DirectoryObject> directoryObjects) {
        List<EntraUserDto> userDtos = new ArrayList<>();
        for (DirectoryObject obj : directoryObjects) {
            if (obj instanceof User user) {
                userDtos.add(EntraUserDto.builder()
                        .entraOid(user.getId())
                        .email(user.getMail())
                        .firstName(user.getGivenName() != null ? user.getGivenName() : MISSING_NAME)
                        .lastName(user.getSurname() != null ? user.getSurname() : MISSING_NAME)
                        .build());
            }
        }
        return userDtos;
    }
}

