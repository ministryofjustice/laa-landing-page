package uk.gov.justice.laa.portal.landingpage.repository;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import uk.gov.justice.laa.portal.landingpage.entity.EntraAppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.LaaApp;
import uk.gov.justice.laa.portal.landingpage.entity.LaaAppRole;
import uk.gov.justice.laa.portal.landingpage.entity.LaaUserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.time.LocalDateTime;
import java.util.HashSet;

@DataJpaTest
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

    protected EntraUser buildEntraUser(String email, String firstName, String lastName, UserType userType) {
        return EntraUser.builder().userType(userType).email(email)
                .userAppRegistrations(HashSet.newHashSet(11))
                .laaUserProfiles(HashSet.newHashSet(11))
                .firstName(firstName).lastName(lastName)
                .active(true).startDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

    protected EntraAppRegistration buildEntraAppRegistration(String name) {
        return EntraAppRegistration.builder().name(name)
                .entraUsers(HashSet.newHashSet(1))
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

    protected Firm buildFirm(String name) {
        return Firm.builder().name(name).createdBy("Test").offices(HashSet.newHashSet(11))
                .createdDate(LocalDateTime.now()).type(FirmType.INDIVIDUAL).build();
    }

    protected Office buildOffice(Firm firm, String name, String address, String phone) {
        return Office.builder().name(name).address(address).phone(phone)
                .createdBy("Test").createdDate(LocalDateTime.now()).firm(firm).build();
    }

    protected LaaApp buildLaaApp(EntraAppRegistration entraAppRegistration, String name) {
        return LaaApp.builder().name(name).entraAppRegistration(entraAppRegistration)
                .appRoles(HashSet.newHashSet(1))
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

    protected LaaAppRole buildLaaAppRole(LaaApp laaApp, String name) {
        return LaaAppRole.builder().name(name).laaApp(laaApp)
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

    protected LaaUserProfile buildLaaUserProfile(EntraUser entraUser) {
        return LaaUserProfile.builder().entraUser(entraUser)
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

}
