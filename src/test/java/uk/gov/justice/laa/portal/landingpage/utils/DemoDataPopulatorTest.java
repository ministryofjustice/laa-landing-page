package uk.gov.justice.laa.portal.landingpage.utils;

import com.microsoft.graph.models.ApplicationCollectionResponse;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataPopulatorTest {

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
    private GraphServiceClient graphServiceClient;

    @InjectMocks
    private DemoDataPopulator demoDataPopulator;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(demoDataPopulator, "appCivilApplyName", "Civil Apply");
        ReflectionTestUtils.setField(demoDataPopulator, "appCrimeApplyName", "Crime Apply");
        ReflectionTestUtils.setField(demoDataPopulator, "appPuiName", "PUI");
        ReflectionTestUtils.setField(demoDataPopulator, "appSubmitCrimeFormName", "Submit a crime form");
        ReflectionTestUtils.setField(demoDataPopulator, "appCrimeApplyDetails",
                "crime_apply_oid//APPREG-USER-Access-LAAD-Apply-Criminal-Legal-Aid//6466b2ed-1103-4588-9c28-561ec9dafa5d");
        ReflectionTestUtils.setField(demoDataPopulator, "appCivilApplyDetails",
                "civil_apply_oid//AppReg-User-Access-LAAD-Apply-Civil-Legal-Aid//3e3003ee-2183-4058-8279-4a557daba8c8");
    }

    @Test
    void populateDummyDataDisabled() {
        demoDataPopulator.appReady(applicationReadyEvent);
        verifyMockCalls(0);
    }

    @Test
    void populateDummyDataAlreadyExists() {
        ReflectionTestUtils.setField(demoDataPopulator, "populateDummyData", true);
        when(firmRepository.findFirmByName("Firm One")).thenReturn(Firm.builder().name("Firm One").build());

        demoDataPopulator.appReady(applicationReadyEvent);

        verifyMockCalls(0);
    }

    @Test
    void populateDummyDataWithMockUser() {
        ReflectionTestUtils.setField(demoDataPopulator, "populateDummyData", true);

        User user = new User();
        UserCollectionResponse userCollectionResponse = mock(UserCollectionResponse.class, RETURNS_DEEP_STUBS);
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(graphServiceClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);
        when(userCollectionResponse.getValue()).thenReturn(List.of(user));

        demoDataPopulator.appReady(applicationReadyEvent);

        verifyMockCalls(1);
    }

    @Test
    void populateDummyDataErrorAddingData() {
        ReflectionTestUtils.setField(demoDataPopulator, "populateDummyData", true);

        // Mocked response from Graph API
        when(firmRepository.saveAll(anyList())).thenThrow(new RuntimeException("Constraint Violation"));

        demoDataPopulator.appReady(applicationReadyEvent);

        Mockito.verify(firmRepository, Mockito.times(1)).saveAll(Mockito.anyList());
        Mockito.verify(officeRepository, Mockito.times(0)).saveAll(Mockito.anyList());
        Mockito.verify(entraUserRepository, Mockito.times(0)).saveAll(Mockito.anyList());
        Mockito.verify(laaAppRepository, Mockito.times(0)).saveAll(Mockito.anyList());
        Mockito.verify(laaAppRoleRepository, Mockito.times(0)).saveAll(Mockito.anyList());
        Mockito.verify(laaUserProfileRepository, Mockito.times(0)).saveAll(Mockito.anyList());
    }

    @Test
    void populateDummyDataErrorAddingCustomApps() {
        ReflectionTestUtils.setField(demoDataPopulator, "populateDummyData", true);

        // Mocked response from Graph API
        when(firmRepository.findFirmByName("Firm One")).thenReturn(Firm.builder().name("Firm One").build());
        when(laaAppRepository.findByEntraAppIdOrName(any(), anyString())).thenThrow(new RuntimeException("Constraint Violation"));

        demoDataPopulator.appReady(applicationReadyEvent);

        verifyMockCalls(0);
    }

    @Test
    void populateDummyDataErrorAddingCustomUser() {
        ReflectionTestUtils.setField(demoDataPopulator, "populateDummyData", true);
        ReflectionTestUtils.setField(demoDataPopulator, "adminUserPrincipals", Set.of("test"));

        // Mocked response from Graph API
        when(firmRepository.findFirmByName("Firm One")).thenReturn(Firm.builder().name("Firm One").build());

        demoDataPopulator.appReady(applicationReadyEvent);

        verifyMockCalls(0);
    }

    @Test
    void populateDummyDataEnabled() {
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(graphServiceClient.users()).thenReturn(usersRequestBuilder);

        ReflectionTestUtils.setField(demoDataPopulator, "populateDummyData", true);
        demoDataPopulator.appReady(applicationReadyEvent);
        verifyMockCalls(1);
    }

    @Test
    void populateDummyDataEnabledWithAdditionalUsers() {
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(graphServiceClient.users()).thenReturn(usersRequestBuilder);

        ReflectionTestUtils.setField(demoDataPopulator, "populateDummyData", true);
        ReflectionTestUtils.setField(demoDataPopulator, "adminUserPrincipals", Set.of("testadmin@email.com:123"));
        ReflectionTestUtils.setField(demoDataPopulator, "nonAdminUserPrincipals", Set.of("testuser@email.com:1234"));
        demoDataPopulator.appReady(applicationReadyEvent);
        verifyMockCalls(1);
    }

    @Test
    void populateDummyDataEnabledWithAdditionalAppsAndUsers() {
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class, RETURNS_DEEP_STUBS);
        when(graphServiceClient.users()).thenReturn(usersRequestBuilder);

        ReflectionTestUtils.setField(demoDataPopulator, "populateDummyData", true);
        ReflectionTestUtils.setField(demoDataPopulator, "adminUserPrincipals", Set.of("testadmin@email.com:123"));
        ReflectionTestUtils.setField(demoDataPopulator, "nonAdminUserPrincipals", Set.of("testuser@email.com:1234"));
        ReflectionTestUtils.setField(demoDataPopulator, "internalUserPrincipals", Set.of("testinternaluser@email.com:1234"));
        demoDataPopulator.appReady(applicationReadyEvent);
        verifyMockCalls(1);
        Mockito.verify(laaAppRepository, Mockito.times(4)).save(Mockito.any(App.class));
        Mockito.verify(laaAppRoleRepository, Mockito.times(8)).save(Mockito.any(AppRole.class));
    }

    @Test
    void getSurname() {
        // Surname and DisplayNames are null
        User user = new User();

        String surname = demoDataPopulator.getSurname(user);
        Assertions.assertThat(surname).isNotNull();
        Assertions.assertThat(surname).isEqualTo("Surname");

        // Display name present but surname is null
        user.setDisplayName("First Last");

        surname = demoDataPopulator.getSurname(user);

        Assertions.assertThat(surname).isNotNull();
        Assertions.assertThat(surname).isEqualTo("Last");

        // Display name present but one word
        user.setDisplayName("First");

        surname = demoDataPopulator.getSurname(user);

        Assertions.assertThat(surname).isNotNull();
        Assertions.assertThat(surname).isEqualTo("Surname");

        // Surname present
        user.setSurname("Sname");

        surname = demoDataPopulator.getSurname(user);

        Assertions.assertThat(surname).isNotNull();
        Assertions.assertThat(surname).isEqualTo("Sname");

        // Surname present but DisplayNames is null
        user.setDisplayName(null);

        surname = demoDataPopulator.getSurname(user);

        Assertions.assertThat(surname).isNotNull();
        Assertions.assertThat(surname).isEqualTo("Sname");

    }

    @Test
    void getFirstName() {
        // Firstname and DisplayNames are null
        User user = new User();

        String firstName = demoDataPopulator.getFirstName(user);
        Assertions.assertThat(firstName).isNotNull();
        Assertions.assertThat(firstName).isEqualTo("Firstname");

        // Display name present but the Firstname is null
        user.setDisplayName("First Last");

        firstName = demoDataPopulator.getFirstName(user);

        Assertions.assertThat(firstName).isNotNull();
        Assertions.assertThat(firstName).isEqualTo("First");

        // Display name present but one word
        user.setDisplayName("First");

        firstName = demoDataPopulator.getFirstName(user);

        Assertions.assertThat(firstName).isNotNull();
        Assertions.assertThat(firstName).isEqualTo("First");

        // Firstname present
        user.setGivenName("Fname");

        firstName = demoDataPopulator.getFirstName(user);

        Assertions.assertThat(firstName).isNotNull();
        Assertions.assertThat(firstName).isEqualTo("Fname");

        // Firstname present but DisplayNames is null
        user.setDisplayName(null);

        firstName = demoDataPopulator.getFirstName(user);

        Assertions.assertThat(firstName).isNotNull();
        Assertions.assertThat(firstName).isEqualTo("Fname");

    }

    @Test
    void getEmailFromUserPrinciple() {
        // UserPrincipalName is null
        User user = new User();

        String email = demoDataPopulator.getEmailFromUserPrinciple(user.getUserPrincipalName());

        Assertions.assertThat(email).isNull();

        // UserPrincipalName present and is email
        user.setUserPrincipalName("test@email.com");

        email = demoDataPopulator.getEmailFromUserPrinciple(user.getUserPrincipalName());

        Assertions.assertThat(email).isNotNull();
        Assertions.assertThat(email).isEqualTo("test@email.com");

        // UserPrincipalName present but not email
        user.setUserPrincipalName("test");

        email = demoDataPopulator.getEmailFromUserPrinciple(user.getUserPrincipalName());

        Assertions.assertThat(email).isNotNull();
        Assertions.assertThat(email).isEqualTo("test");

        // UserPrincipalName present and is external email
        user.setUserPrincipalName("test.user_email.com#EXT#@version1workforcesandbox.onmicrosoft.com");

        email = demoDataPopulator.getEmailFromUserPrinciple(user.getUserPrincipalName());

        Assertions.assertThat(email).isNotNull();
        Assertions.assertThat(email).isEqualTo("test.user@email.com");

    }

    private void verifyMockCalls(int times) {
        Mockito.verify(firmRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(officeRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(entraUserRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(laaUserProfileRepository, Mockito.times(times)).saveAll(Mockito.anyList());
    }
}