package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.DirectoryObjectCollectionResponse;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

class InternalUserPollingServiceTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private GraphServiceClient graphServiceClient;
    @Mock
    private UserService userService;
    @InjectMocks
    private InternalUserPollingService internalUserPollingService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Set the @Value field via reflection
        java.lang.reflect.Field field = InternalUserPollingService.class.getDeclaredField("pollingGroupId");
        field.setAccessible(true);
        field.set(internalUserPollingService, "test-group-id");
    }


    @Test
    void shouldNotCreateNewUsers_whenNoResponseFromGraphApi() {
        when(userService.getInternalUserEntraIds()).thenReturn(List.of());
        when(graphServiceClient.groups().byGroupId(anyString()).members().get()).thenReturn(null);

        internalUserPollingService.pollForNewUsers();

        verify(userService, times(1)).getInternalUserEntraIds();
    }

    @Test
    void shouldNotCreateNewUsers_whenNewUsersNotFound() {
        when(userService.getInternalUserEntraIds()).thenReturn(List.of(UUID.randomUUID()));
        DirectoryObjectCollectionResponse response = new DirectoryObjectCollectionResponse();
        response.setValue(new ArrayList<>());
        when(graphServiceClient.groups().byGroupId(anyString()).members().get()).thenReturn(response);

        internalUserPollingService.pollForNewUsers();
        verify(userService, times(1)).getInternalUserEntraIds();
    }

    @Test
    void shouldCreateNewUsers_whenNewUsersFound() {
        UUID existingId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        when(userService.getInternalUserEntraIds()).thenReturn(List.of(existingId));
        User newUser = new User();
        newUser.setId(newId.toString());
        newUser.setMail("test@example.com");
        newUser.setSurname("Doe");
        newUser.setGivenName("John");
        newUser.setDisplayName("John Doe");
        List<DirectoryObject> directoryObjects = List.of(newUser);
        DirectoryObjectCollectionResponse response = new DirectoryObjectCollectionResponse();
        response.setValue(directoryObjects);
        when(graphServiceClient.groups().byGroupId(anyString()).members().get()).thenReturn(response);

        internalUserPollingService.pollForNewUsers();
        ArgumentCaptor<List<EntraUserDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(userService).createInternalPolledUser(captor.capture());
        List<EntraUserDto> dtos = captor.getValue();
        assertEquals(1, dtos.size());
        assertEquals(newId.toString(), dtos.get(0).getEntraOid());
        assertEquals("test@example.com", dtos.get(0).getEmail());
        assertEquals("John", dtos.get(0).getFirstName());
        assertEquals("Doe", dtos.get(0).getLastName());
    }

    @Test
    void shouldNotCreateNewUsers_whenResponseHasNoUserInstances() {
        DirectoryObject notAUser1 = new DirectoryObject();
        notAUser1.setId(UUID.randomUUID().toString());
        DirectoryObject notAUser2 = new DirectoryObject();
        notAUser2.setId(UUID.randomUUID().toString());
        DirectoryObjectCollectionResponse response = new DirectoryObjectCollectionResponse();
        response.setValue(List.of(notAUser1, notAUser2));
        when(graphServiceClient.groups().byGroupId(anyString()).members().get()).thenReturn(response);
        when(userService.getInternalUserEntraIds()).thenReturn(List.of(UUID.randomUUID()));

        internalUserPollingService.pollForNewUsers();
        verify(userService, never()).createInternalPolledUser(any());
    }

    @Test
    void shouldCreateNewUsers_whenResponseAUser() {
        UUID newId = UUID.randomUUID();
        DirectoryObject notAUser = new DirectoryObject();
        notAUser.setId(UUID.randomUUID().toString());
        User user = new User();
        user.setId(newId.toString());
        user.setMail("user@example.com");
        user.setSurname("Smith");
        user.setGivenName("Jane");
        user.setDisplayName("Jane Smith");
        DirectoryObjectCollectionResponse response = new DirectoryObjectCollectionResponse();
        response.setValue(List.of(notAUser, user));
        when(graphServiceClient.groups().byGroupId(anyString()).members().get()).thenReturn(response);
        when(userService.getInternalUserEntraIds()).thenReturn(List.of(UUID.randomUUID()));

        internalUserPollingService.pollForNewUsers();
        ArgumentCaptor<List<EntraUserDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(userService).createInternalPolledUser(captor.capture());
        List<EntraUserDto> dtos = captor.getValue();
        assertEquals(1, dtos.size());
        assertEquals(newId.toString(), dtos.get(0).getEntraOid());
    }
}
