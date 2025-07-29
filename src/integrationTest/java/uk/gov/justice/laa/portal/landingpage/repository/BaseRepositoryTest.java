package uk.gov.justice.laa.portal.landingpage.repository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

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

    protected EntraUser buildEntraUser(String entraId, String email, String firstName, String lastName) {
        return EntraUser.builder().email(email).entraOid(entraId)
                .userProfiles(HashSet.newHashSet(11))
                .firstName(firstName).lastName(lastName)
                .userStatus(UserStatus.ACTIVE)
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

    protected Firm buildFirm(String name, String firmCode) {
        return Firm.builder().name(name).offices(HashSet.newHashSet(11))
                .code(firmCode).type(FirmType.INDIVIDUAL).build();
    }

    protected Office buildOffice(Firm firm, String name, String address, String phone, String officeCode) {
        return Office.builder().name(name).code(officeCode).address(address).phone(phone).firm(firm).build();
    }

    protected App buildLaaApp(String name, String entraAppId, String securityGroupOid, String securityGroupName) {
        return App.builder().name(name).appRoles(HashSet.newHashSet(1))
                .entraAppId(entraAppId).securityGroupOid(securityGroupOid).securityGroupName(securityGroupName).build();
    }

    protected AppRole buildLaaAppRole(App app, String name) {
        return AppRole.builder().name(name).roleType(RoleType.INTERNAL).description(name)
                .userTypeRestriction(new UserType[]{UserType.INTERNAL}).app(app).build();
    }

    protected UserProfile buildLaaUserProfile(EntraUser entraUser, UserType userType, boolean active) {
        return UserProfile.builder().entraUser(entraUser)
                .userType(userType).appRoles(HashSet.newHashSet(1))
                .createdDate(LocalDateTime.now()).createdBy("Test").activeProfile(active)
                .userProfileStatus(UserProfileStatus.COMPLETE).build();
    }

    protected UserProfile buildLaaUserProfile(EntraUser entraUser, UserType userType) {
        return buildLaaUserProfile(entraUser, userType, false);
    }

    protected String generateEntraId() {
        return UUID.randomUUID().toString();
    }

}
