package uk.gov.justice.laa.portal.landingpage.utils;

import com.microsoft.graph.applications.ApplicationsRequestBuilder;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.ApplicationCollectionResponse;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.graph.userswithuserprincipalname.UsersWithUserPrincipalNameRequestBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.AppRegistrationRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataPopulatorTest {

    @Mock
    private AppRegistrationRepository entraAppRegistrationRepository;

    @Mock
    private FirmRepository firmRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private EntraUserRepository entraUserRepository;

    @Mock
    private AppRepository laaAppRepository;

    @Mock
    private AppRoleRepository laaAppRoleRepository;

    @Mock
    private UserProfileRepository laaUserProfileRepository;

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    @Mock
    private ApplicationCollectionResponse mockApplicationCollectionResponse;

    @Mock
    private GraphServiceClient graphServiceClient;

    @InjectMocks
    private DemoDataPopulator demoDataPopulator;

    @Test
    void populateDummyDataDisabled() {
        demoDataPopulator.appReady(applicationReadyEvent);
        verifyMockCalls(0);
    }

    @Test
    void populateDummyDataEnabled() {
        //Setup
        Application app1 = new Application();
        app1.setAppId("698815d2-5760-4fd0-bdef-54c683e91b26");
        app1.setDisplayName("App One");

        Application app2 = new Application();
        app2.setAppId("f27a5c75-a33b-4290-becf-9e4f0c14a1eb");
        app2.setDisplayName("App Two");

        // Mocked response from Graph API
        when(mockApplicationCollectionResponse.getValue()).thenReturn(List.of(app1, app2));
        ApplicationsRequestBuilder applicationsRequestBuilder = mock(ApplicationsRequestBuilder.class);
        when(graphServiceClient.applications()).thenReturn(applicationsRequestBuilder);
        when(applicationsRequestBuilder.get(any())).thenReturn(mockApplicationCollectionResponse);

        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(graphServiceClient.users()).thenReturn(usersRequestBuilder);
        UsersWithUserPrincipalNameRequestBuilder usersWithUserPrincipalNameRequestBuilder = mock(UsersWithUserPrincipalNameRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(graphServiceClient.usersWithUserPrincipalName(any())).thenReturn(usersWithUserPrincipalNameRequestBuilder);

        ReflectionTestUtils.setField(demoDataPopulator, "populateDummyData", true);
        demoDataPopulator.appReady(applicationReadyEvent);
        verifyMockCalls(1);
    }

    private void verifyMockCalls(int times) {
        Mockito.verify(entraAppRegistrationRepository, Mockito.times(times)).saveAll(Mockito.anySet());
        Mockito.verify(firmRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(officeRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(entraUserRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(laaAppRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(laaAppRoleRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(laaUserProfileRepository, Mockito.times(times)).saveAll(Mockito.anyList());
    }

    @Test
    void getSurname() {
        // Surname and DisplayNames are null
        User user = new User();

        String surname = demoDataPopulator.getSurname(user);
        Assertions.assertNotNull(surname);
        Assertions.assertEquals("Surname", surname);

        // Display name present but surname is null
        user.setDisplayName("First Last");

        surname = demoDataPopulator.getSurname(user);

        Assertions.assertNotNull(surname);
        Assertions.assertEquals("Last", surname);

        // Display name present but one word
        user.setDisplayName("First");

        surname = demoDataPopulator.getSurname(user);

        Assertions.assertNotNull(surname);
        Assertions.assertEquals("Surname", surname);

        // Surname present
        user.setSurname("Sname");

        surname = demoDataPopulator.getSurname(user);

        Assertions.assertNotNull(surname);
        Assertions.assertEquals("Sname", surname);

        // Surname present but DisplayNames is null
        user.setDisplayName(null);

        surname = demoDataPopulator.getSurname(user);

        Assertions.assertNotNull(surname);
        Assertions.assertEquals("Sname", surname);

    }

    @Test
    void getFirstName() {
        // Firstname and DisplayNames are null
        User user = new User();

        String firstName = demoDataPopulator.getFirstName(user);
        Assertions.assertNotNull(firstName);
        Assertions.assertEquals("Firstname", firstName);

        // Display name present but the Firstname is null
        user.setDisplayName("First Last");

        firstName = demoDataPopulator.getFirstName(user);

        Assertions.assertNotNull(firstName);
        Assertions.assertEquals("First", firstName);

        // Display name present but one word
        user.setDisplayName("First");

        firstName = demoDataPopulator.getFirstName(user);

        Assertions.assertNotNull(firstName);
        Assertions.assertEquals("First", firstName);

        // Firstname present
        user.setGivenName("Fname");

        firstName = demoDataPopulator.getFirstName(user);

        Assertions.assertNotNull(firstName);
        Assertions.assertEquals("Fname", firstName);

        // Firstname present but DisplayNames is null
        user.setDisplayName(null);

        firstName = demoDataPopulator.getFirstName(user);

        Assertions.assertNotNull(firstName);
        Assertions.assertEquals("Fname", firstName);

    }

    @Test
    void getEmailFromUserPrinciple() {
        // UserPrincipalName is null
        User user = new User();

        String email = demoDataPopulator.getEmailFromUserPrinciple(user);

        Assertions.assertNull(email);

        // UserPrincipalName present and is email
        user.setUserPrincipalName("test@email.com");

        email = demoDataPopulator.getEmailFromUserPrinciple(user);

        Assertions.assertNotNull(email);
        Assertions.assertEquals("test@email.com", email);

        // UserPrincipalName present but not email
        user.setUserPrincipalName("test");

        email = demoDataPopulator.getEmailFromUserPrinciple(user);

        Assertions.assertNotNull(email);
        Assertions.assertEquals("test", email);

        // UserPrincipalName present and is external email
        user.setUserPrincipalName("test.user_email.com#EXT#@version1workforcesandbox.onmicrosoft.com");

        email = demoDataPopulator.getEmailFromUserPrinciple(user);

        Assertions.assertNotNull(email);
        Assertions.assertEquals("test.user@email.com", email);

    }
}