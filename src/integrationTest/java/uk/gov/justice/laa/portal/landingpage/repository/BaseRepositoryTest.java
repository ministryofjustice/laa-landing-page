package uk.gov.justice.laa.portal.landingpage.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.session.autoconfigure.SessionTimeout;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.InvitationStatus;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileSilasStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected AppRoleRepository appRoleRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected RoleAssignmentRepository roleAssignmentRepository;

    @ServiceConnection
    public static final PostgreSQLContainer<?> postgresServer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("postgres")
            .withPassword("password");

    static {
        postgresServer.start();
    }

    @Configuration
    static class TestSessionFallbackConfig {
        @Bean
        public SessionTimeout sessionTimeout() {
            return () -> Duration.ofMinutes(30);
        }

        @Bean
        public Object tokenDetailsManager() {
            // Returns a generic mock object so the framework's @PostConstruct
            // initialization checks are bypassed completely
            return Mockito.mock(Object.class);
        }
    }

    protected EntraUser buildEntraUser(String entraId, String email, String firstName, String lastName) {
        return buildEntraUser(entraId, email, firstName, lastName, false);
    }

    protected EntraUser buildEntraUser(String entraId, String email, String firstName, String lastName, boolean multiFirmUser) {
        return EntraUser.builder().email(email).entraOid(entraId)
                .userProfiles(HashSet.newHashSet(11))
                .firstName(firstName).lastName(lastName)
                .multiFirmUser(multiFirmUser)
                .userStatus(UserStatus.ACTIVE)
                .invitationStatus(InvitationStatus.VERIFICATION_SUCCESS)
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

    protected Firm buildFirm(String name, String firmCode) {
        return Firm.builder().name(name).offices(HashSet.newHashSet(11))
                .code(firmCode).type(FirmType.ADVOCATE).build();
    }

    protected Firm buildParentFirm(String name, String firmCode) {
        return Firm.builder().name(name).offices(HashSet.newHashSet(11))
                .code(firmCode).type(FirmType.LEGAL_SERVICES_PROVIDER).build();
    }

    protected Firm buildChildFirm(String name, String firmCode, Firm parentFirm) {
        return Firm.builder().name(name).offices(HashSet.newHashSet(11))
                .code(firmCode).type(FirmType.ADVOCATE).parentFirm(parentFirm).build();
    }

    protected Office buildOffice(Firm firm, String address, String officeCode) {
        Office.Address addr = Office.Address.builder().addressLine1(address).city("city").postcode("postcode").build();
        return Office.builder().code(officeCode).address(addr).firm(firm).build();
    }

    protected App buildLaaApp(String name, String entraAppId, String securityGroupOid) {
        return buildLaaApp(name, entraAppId, securityGroupOid, "Test App Description " + ThreadLocalRandom.current().nextInt(),
                "http://localhost:8080/");
    }

    protected App buildLaaApp(String name, String entraAppId, String securityGroupOid, String description, String url) {
        return App.builder().name(name).appRoles(HashSet.newHashSet(1)).url(url)
                .description(description).appType(AppType.LAA)
                .entraAppId(entraAppId).securityGroupOid(securityGroupOid)
                .enabled(true).build();
    }

    protected AppRole buildLaaAppRole(App app, String name) {
        return AppRole.builder().name(name).description(name)
                .userTypeRestriction(new UserType[]{UserType.INTERNAL}).app(app).build();
    }

    protected AppRole buildLaaExternalAppRole(App app, String name) {
        return AppRole.builder().name(name).description(name)
                .userTypeRestriction(new UserType[]{UserType.EXTERNAL}).app(app).build();
    }

    protected AppRole buildLaaAppRoleWithUserTypes(App app, String name, UserType[] userTypes, Set<Permission> permissions) {
        return AppRole.builder().name(name).description(name)
                .userTypeRestriction(userTypes)
                .permissions(permissions)
                .app(app).build();
    }

    protected UserProfile buildLaaUserProfile(EntraUser entraUser, UserType userType, boolean active) {
        return buildLaaUserProfile(entraUser, userType, active, "Global Admin");
    }

    protected UserProfile buildLaaUserProfile(
            EntraUser entraUser,
            UserType userType,
            boolean active,
            String roleName
    ) {
        Set<AppRole> roles = new HashSet<>();

        if (roleName != null) {
            List<AppRole> allAppRoles = appRoleRepository.findAllWithPermissions();

            AppRole appRole = allAppRoles.stream()
                    .filter(AppRole::isAuthzRole)
                    .filter(role -> role.getName().equals(roleName))
                    .findFirst()
                    .orElseThrow(() ->
                            new RuntimeException("Could not find app role: " + roleName)
                    );

            roles.add(appRole);
        }

        return UserProfile.builder()
                .entraUser(entraUser)
                .userType(userType)
                .appRoles(roles)
                .createdDate(LocalDateTime.now())
                .createdBy("Test")
                .activeProfile(active)
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .silasStatus(UserProfileSilasStatus.COMPLETE)
                .lastCcmsSyncSuccessful(true)
                .build();
    }

    protected UserProfile buildLaaUserProfile(EntraUser entraUser, UserType userType) {
        return buildLaaUserProfile(entraUser, userType, false, "Global Admin");
    }

    protected UserProfile buildLaaUserProfileWithoutRoles(
            EntraUser entraUser,
            UserType userType
    ) {
        return buildLaaUserProfile(entraUser, userType, false, null);
    }

    protected String generateEntraId() {
        return UUID.randomUUID().toString();
    }

    protected void deleteNonAuthzAppRoleAssignments() {
        List<RoleAssignment> nonAuthzAppRoleAssignmentss = roleAssignmentRepository.findAll().stream()
                .filter(role -> !role.getAssignableRole().isAuthzRole())
                .toList();
        roleAssignmentRepository.deleteAll(nonAuthzAppRoleAssignmentss);
    }

    protected void deleteNonAuthzAppRoles(AppRoleRepository appRoleRepository) {
        List<AppRole> nonAuthzAppRoles = appRoleRepository.findAll().stream()
                .filter(role -> !role.isAuthzRole())
                .toList();
        appRoleRepository.deleteAll(nonAuthzAppRoles);
    }

    protected void deleteNonAuthzApps(AppRepository appRepository) {
        // Keep the Manage Your Users app as it's reference data.
        List<App> nonAuthzApps = appRepository.findAll().stream()
                .filter(app -> !AppType.AUTHZ.equals(app.getAppType()))
                .toList();
        appRepository.deleteAll(nonAuthzApps);
    }

    protected EntraUser buildMultifirmEntraUser(String entraId, String email, String firstName, String lastName,
                                                Boolean multiFirmUser) {
        return EntraUser.builder().email(email).entraOid(entraId)
                .userProfiles(HashSet.newHashSet(11))
                .firstName(firstName).lastName(lastName)
                .userStatus(UserStatus.ACTIVE)
                .createdDate(LocalDateTime.now()).createdBy("Test").multiFirmUser(multiFirmUser).build();
    }

    protected EntraUser buildDeactiveEntraUser(String entraId, String email, String firstName, String lastName,
                                                Boolean multiFirmUser) {
        return EntraUser.builder().email(email).entraOid(entraId)
                .userProfiles(HashSet.newHashSet(11))
                .firstName(firstName).lastName(lastName)
                .userStatus(UserStatus.DEACTIVE)
                .enabled(false)
                .createdDate(LocalDateTime.now()).createdBy("Test").multiFirmUser(multiFirmUser).build();
    }

    protected UserProfile buildFirmUserManagerProfile(EntraUser entraUser, UserType userType, boolean active) {
        List<AppRole> allAppRoles = appRoleRepository.findAllWithPermissions();
        AppRole globalAdminAppRole = allAppRoles.stream()
                .filter(AppRole::isAuthzRole)
                .filter(role -> role.getName().equals("Firm User Manager"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find app role"));
        return UserProfile.builder().entraUser(entraUser)
                .userType(userType).appRoles(new HashSet<>(Set.of(globalAdminAppRole)))
                .createdDate(LocalDateTime.now()).createdBy("Test").activeProfile(active)
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .silasStatus(UserProfileSilasStatus.COMPLETE)
                .lastCcmsSyncSuccessful(true).build();
    }


}
