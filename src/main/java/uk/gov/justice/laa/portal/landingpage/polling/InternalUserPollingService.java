package uk.gov.justice.laa.portal.landingpage.polling;

import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.DirectoryObjectCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InternalUserPollingService {

    private final GraphServiceClient graphClient;
    private final UserService userService;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    // wip: TODO
    // 1. refactor: move to service
    // 2. get email, first and last name for newUserIds
    // 3. insert new user info into entra & user profile tables in single transaction
    @Scheduled(fixedRate = 300000)
    public void poll() {
        List<UUID> internalUserEntraOids = userService.getInternalUserEntraIds();
        DirectoryObjectCollectionResponse response = graphClient.groups()
                .byGroupId("b8db52f8-8531-4c9b-be1e-41fcf65d96ca")
                .members().get();
        if (response != null && response.getValue() != null) {
            List<DirectoryObject> users = response.getValue();
            List<String> newUserIds = users.stream().filter(u ->
                    !internalUserEntraOids.contains(UUID.fromString(u.getId()))).map(DirectoryObject::getId).toList();
            for (String newUserId : newUserIds) {
                //TODO create user in user profile table + entraoid FK
                logger.info("Adding user with id '{}'", newUserId);
            }
        }
    }
}
