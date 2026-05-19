package uk.gov.justice.laa.portal.landingpage.polling;

import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.DirectoryObjectCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.portal.landingpage.controller.BaseIntegrationTest;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringBootApplicationProperties"})
// Enable polling for this test only.
@TestPropertySource(properties = {
    "internal.user.polling.enabled=true"
})
public class InternalUserPollingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EntraUserRepository entraUserRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private InternalUserPolling internalUserPolling;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private GraphServiceClient graphServiceClient;

    @Test
    public void testInternalUserPollingDeletesNotReturnedInternalUser() {
        // Have the poll response return all users before adding new user.
        List<UUID> allUserEntraIds = userService.getInternalUserEntraIds();
        // Setup mock entra response.
        List<DirectoryObject> directoryObjects = allUserEntraIds.stream()
                .map(uuid -> {
                    DirectoryObject directoryObject = mock(DirectoryObject.class);
                    when(directoryObject.getId()).thenReturn(uuid.toString());
                    return directoryObject;
                })
                .toList();
        DirectoryObjectCollectionResponse response = mock(DirectoryObjectCollectionResponse.class);
        when(response.getValue()).thenReturn(directoryObjects);
        when(graphServiceClient.groups().byGroupId(any()).members().get()).thenReturn(response);

        // Add a new internal user.
        EntraUser userToBeDeleted = buildEntraUser(UUID.randomUUID().toString(), "internalpollingtest@test.com", "InternalPolling", "Test");
        UserProfile profileToBeDeleted = buildLaaUserProfile(userToBeDeleted, UserType.INTERNAL, true, "Global Admin");
        userToBeDeleted = entraUserRepository.saveAndFlush(userToBeDeleted);
        profileToBeDeleted = userProfileRepository.saveAndFlush(profileToBeDeleted);

        // Poll
        internalUserPolling.poll();

        // Ensure newly added user was successfully deleted after polling.
        Optional<EntraUser> deletedUser = entraUserRepository.findById(userToBeDeleted.getId());
        Optional<UserProfile> deletedProfile = userProfileRepository.findById(profileToBeDeleted.getId());

        assertThat(deletedUser).isEmpty();
        assertThat(deletedProfile).isEmpty();
    }

    @Test
    public void testInternalUserPollingDoesNotDeleteUsersWhenAllUsersExist() {
        // Have the poll response return all users.
        List<UUID> allUserEntraIds = userService.getInternalUserEntraIds();
        // Setup mock entra response.
        List<DirectoryObject> directoryObjects = allUserEntraIds.stream()
                .map(uuid -> {
                    DirectoryObject directoryObject = mock(DirectoryObject.class);
                    when(directoryObject.getId()).thenReturn(uuid.toString());
                    return directoryObject;
                })
                .toList();
        DirectoryObjectCollectionResponse response = mock(DirectoryObjectCollectionResponse.class);
        when(response.getValue()).thenReturn(directoryObjects);
        when(graphServiceClient.groups().byGroupId(any()).members().get()).thenReturn(response);

        // Poll
        internalUserPolling.poll();

        // Ensure no users deleted.
        assertThat(allUserEntraIds.size()).isEqualTo(userService.getInternalUserEntraIds().size());
    }

}
