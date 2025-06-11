package uk.gov.justice.laa.portal.landingpage.repository;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import uk.gov.justice.laa.portal.landingpage.entity.AppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.time.LocalDateTime;
import java.util.HashSet;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BaseRepositoryTest {

    @Container
    @ServiceConnection
    public static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("postgres")
            .withPassword("password");

    @BeforeAll
    static void beforeAll() {
        postgresContainer.start();
    }

    protected EntraUser buildEntraUser(String email, String firstName, String lastName) {
        return EntraUser.builder().email(email).userName(email)
                .userAppRegistrations(HashSet.newHashSet(11))
                .userProfiles(HashSet.newHashSet(11))
                .firstName(firstName).lastName(lastName)
                .userStatus(UserStatus.ACTIVE).startDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

    protected AppRegistration buildEntraAppRegistration(String name) {
        return AppRegistration.builder().name(name)
                .entraUsers(HashSet.newHashSet(1)).build();
    }

    protected Firm buildFirm(String name) {
        return Firm.builder().name(name).offices(HashSet.newHashSet(11))
                .type(FirmType.INDIVIDUAL).build();
    }

    protected Office buildOffice(Firm firm, String name, String address, String phone) {
        return Office.builder().name(name).address(address).phone(phone).firm(firm).build();
    }

    protected App buildLaaApp(AppRegistration appRegistration, String name) {
        return App.builder().name(name).appRegistration(appRegistration)
                .appRoles(HashSet.newHashSet(1)).build();
    }

    protected AppRole buildLaaAppRole(App app, String name) {
        return AppRole.builder().name(name).app(app).build();
    }

    protected UserProfile buildLaaUserProfile(EntraUser entraUser, UserType userType) {
        return UserProfile.builder().entraUser(entraUser)
                .userType(userType).appRoles(HashSet.newHashSet(1))
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

}
