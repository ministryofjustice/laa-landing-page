package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

public abstract class RoleBasedAccessIntegrationTest extends BaseIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected EntraUserRepository entraUserRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected UserProfileRepository userProfileRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected AppRoleRepository appRoleRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected AppRepository appRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected FirmRepository firmRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected OfficeRepository officeRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected EntityManager entityManager;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected PlatformTransactionManager transactionManager;

    protected Firm testFirm1;
    protected Firm testFirm2;
    protected List<EntraUser> internalUsersNoRoles = new ArrayList<>();
    protected List<EntraUser> internalUserManagers = new ArrayList<>();
    protected List<EntraUser> internalAndExternalUserManagers = new ArrayList<>();
    protected List<EntraUser> internalWithExternalOnlyUserManagers = new ArrayList<>();
    protected List<EntraUser> externalOnlyUserManagers = new ArrayList<>();
    protected List<EntraUser> externalUsersNoRoles = new ArrayList<>();
    protected List<EntraUser> externalUserAdmins = new ArrayList<>();
    protected List<EntraUser> internalUserViewers = new ArrayList<>();
    protected List<EntraUser> externalUserViewers = new ArrayList<>();
    protected List<EntraUser> multiFirmUsers = new ArrayList<>();
    protected List<EntraUser> globalAdmins = new ArrayList<>();
    protected List<EntraUser> silasAdmins = new ArrayList<>();
    protected List<EntraUser> firmUserManagers = new ArrayList<>();
    protected List<EntraUser> securityResponseUsers = new ArrayList<>();
    protected List<EntraUser> allUsers = new ArrayList<>();

    @BeforeAll
    public void beforeAll() {
        clearRepositories();
        setupFirms();
        setupTestUsers();
    }

    @AfterAll
    public void afterAll() {
        clearRepositories();
    }

    protected void setupFirms() {
        TransactionStatus txStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            // Temporarily disable constraint triggers during setup
            entityManager.createNativeQuery("SET session_replication_role = replica").executeUpdate();
            Firm firm1 = buildFirm("firm1", "firm1");
            testFirm1 = firmRepository.save(firm1);
            Office firm1Office1 = buildOffice(testFirm1, "Firm1Office1", "Firm 1 Office 1", "123456789", "F1Office1Code");
            Office firm1Office2 = buildOffice(testFirm1, "Firm1Office2", "Firm 1 Office 2", "123456789", "F1Office2Code");
            officeRepository.save(firm1Office1);
            officeRepository.save(firm1Office2);
            testFirm1.setOffices(new java.util.HashSet<>(Set.of(firm1Office1, firm1Office2)));
            testFirm1 = firmRepository.save(testFirm1);

            Firm firm2 = buildFirm("firm2", "firm2");
            testFirm2 = firmRepository.save(firm2);
            Office firm2Office1 = buildOffice(testFirm2, "Firm2Office1", "Firm 2 Office 1", "123456789", "F2Office1Code");
            Office firm2Office2 = buildOffice(testFirm2, "Firm2Office2", "Firm 2 Office 2", "123456789", "F2Office2Code");
            officeRepository.save(firm2Office1);
            officeRepository.save(firm2Office2);
            testFirm2.setOffices(new java.util.HashSet<>(Set.of(firm2Office1, firm2Office2)));
            testFirm2 = firmRepository.save(testFirm2);
            
            firmRepository.flush();
            entityManager.createNativeQuery("SET session_replication_role = DEFAULT").executeUpdate();
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }

    protected void setupTestUsers() {
        // Index to keep all email addresses unique.
        int emailIndex = 0;
        List<AppRole> allAppRoles = appRoleRepository.findAllWithPermissions();

        for (int i = 0; i < 5; i++) {
            // Setup 5 internal users no roles
            EntraUser user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "External", "FirmOne");
            UserProfile profile = buildLaaUserProfile(user, UserType.INTERNAL, true);
            profile.setAppRoles(Set.of());
            user.setUserProfiles(Set.of(profile));
            profile.setEntraUser(user);
            internalUsersNoRoles.add(entraUserRepository.saveAndFlush(user));
        }

        // Setup 5 internal userManagers
        for (int i = 0; i < 5; i++) {
            EntraUser user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "Internal", "InternalUserManager");
            UserProfile profile = buildLaaUserProfile(user, UserType.INTERNAL, true);
            AppRole appRole = allAppRoles.stream()
                    .filter(AppRole::isAuthzRole)
                    .filter(role -> role.getName().equals("Internal User Manager"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find app role"));
            profile.setAppRoles(Set.of(appRole));
            user.setUserProfiles(Set.of(profile));
            profile.setEntraUser(user);
            internalUserManagers.add(entraUserRepository.saveAndFlush(user));
        }

        // Setup 5 internal Users with external user manager role.
        for (int i = 0; i < 5; i++) {
            EntraUser user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "Internal", "ExternalUserManager");
            UserProfile profile = buildLaaUserProfile(user, UserType.INTERNAL, true);
            AppRole internalUserManagerRole = allAppRoles.stream()
                    .filter(AppRole::isAuthzRole)
                    .filter(role -> role.getName().equals("Internal User Manager"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find app role"));
            AppRole externalUserManagerRole = allAppRoles.stream()
                    .filter(AppRole::isAuthzRole)
                    .filter(role -> role.getName().equals("External User Manager"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find app role"));
            profile.setAppRoles(Set.of(internalUserManagerRole, externalUserManagerRole));
            user.setUserProfiles(Set.of(profile));
            profile.setEntraUser(user);
            internalAndExternalUserManagers.add(entraUserRepository.saveAndFlush(user));
        }

        // Setup 5 internal users with ONLY external user manager role.
        for (int i = 0; i < 5; i++) {
            EntraUser user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "Internal", "ExternalOnlyUserManager");
            UserProfile profile = buildLaaUserProfile(user, UserType.INTERNAL, true);
            AppRole externalUserManagerRole = allAppRoles.stream()
                    .filter(AppRole::isAuthzRole)
                    .filter(role -> role.getName().equals("External User Manager"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find app role"));
            profile.setAppRoles(Set.of(externalUserManagerRole));
            user.setUserProfiles(Set.of(profile));
            profile.setEntraUser(user);
            internalWithExternalOnlyUserManagers.add(entraUserRepository.saveAndFlush(user));
        }

        // Setup 5 external users with external user manager role.
        for (int i = 0; i < 5; i++) {
            EntraUser user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "External", "ExternalUserManager");
            UserProfile profile = buildLaaUserProfile(user, UserType.EXTERNAL, true);
            profile.setFirm(testFirm1);
            AppRole externalUserManagerRole = allAppRoles.stream()
                    .filter(AppRole::isAuthzRole)
                    .filter(role -> role.getName().equals("Firm User Manager"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find app role"));
            profile.setAppRoles(Set.of(externalUserManagerRole));
            user.setUserProfiles(Set.of(profile));
            profile.setEntraUser(user);
            externalOnlyUserManagers.add(entraUserRepository.saveAndFlush(user));
        }

        // Setup 5 Firm1 External Users
        for (int i = 0; i < 5; i++) {
            EntraUser user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "External", "FirmOne");
            UserProfile profile = buildLaaUserProfile(user, UserType.EXTERNAL, true);
            profile.setAppRoles(Set.of());
            profile.setFirm(testFirm1);
            user.setUserProfiles(Set.of(profile));
            profile.setEntraUser(user);
            externalUsersNoRoles.add(entraUserRepository.saveAndFlush(user));
        }

        // Setup 5 Firm2 External Users
        for (int i = 0; i < 5; i++) {
            EntraUser user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "External", "FirmTwo");
            UserProfile profile = buildLaaUserProfile(user, UserType.EXTERNAL, true);
            profile.setAppRoles(Set.of());
            profile.setFirm(testFirm2);
            user.setUserProfiles(Set.of(profile));
            profile.setEntraUser(user);
            externalUsersNoRoles.add(entraUserRepository.saveAndFlush(user));
        }

        // Setup Firm1 admin
        EntraUser user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "External", "FirmOneAdmin");
        UserProfile profile = buildLaaUserProfile(user, UserType.INTERNAL, true);
        AppRole appRole = allAppRoles.stream()
                .filter(AppRole::isAuthzRole)
                .filter(role -> role.getName().equals("External User Admin"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find app role"));
        profile.setAppRoles(Set.of(appRole));
        user.setUserProfiles(Set.of(profile));
        profile.setEntraUser(user);
        externalUserAdmins.add(entraUserRepository.saveAndFlush(user));

        // Setup Firm2 admin
        user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "External", "FirmTwoAdmin");
        profile = buildLaaUserProfile(user, UserType.INTERNAL, true);
        profile.setAppRoles(Set.of(appRole));
        user.setUserProfiles(Set.of(profile));
        profile.setEntraUser(user);
        externalUserAdmins.add(entraUserRepository.saveAndFlush(user));

        // Setup Global Admin
        user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "Internal", "GlobalAdmin");
        profile = buildLaaUserProfile(user, UserType.INTERNAL, true);
        appRole = allAppRoles.stream()
                .filter(AppRole::isAuthzRole)
                .filter(role -> role.getName().equals("Global Admin"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find app role"));
        profile.setAppRoles(Set.of(appRole));
        user.setUserProfiles(Set.of(profile));
        profile.setEntraUser(user);
        globalAdmins.add(entraUserRepository.saveAndFlush(user));

        // Set up Internal User Viewer
        user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "Internal", "UserViewer");
        profile = buildLaaUserProfile(user, UserType.INTERNAL, true);
        appRole = allAppRoles.stream()
                .filter(AppRole::isAuthzRole)
                .filter(role -> role.getName().equals("Internal User Viewer"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find app role"));
        profile.setAppRoles(Set.of(appRole));
        user.setUserProfiles(Set.of(profile));
        profile.setEntraUser(user);
        internalUserViewers.add(entraUserRepository.saveAndFlush(user));

        // Set up External User Viewer
        user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "External", "UserViewer");
        profile = buildLaaUserProfile(user, UserType.INTERNAL, true);
        appRole = allAppRoles.stream()
                .filter(AppRole::isAuthzRole)
                .filter(role -> role.getName().equals("External User Viewer"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find app role"));
        profile.setAppRoles(Set.of(appRole));
        user.setUserProfiles(Set.of(profile));
        profile.setEntraUser(user);
        externalUserViewers.add(entraUserRepository.saveAndFlush(user));

        // Set up Firm User Manager
        user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "External", "FirmOneUserManager");
        profile = buildLaaUserProfile(user, UserType.EXTERNAL, true);
        AppRole firmUserManagerRole = allAppRoles.stream()
                .filter(AppRole::isAuthzRole)
                .filter(role -> role.getName().equals("Firm User Manager"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find app role"));
        profile.setAppRoles(Set.of(firmUserManagerRole));
        profile.setFirm(testFirm2);
        user.setUserProfiles(Set.of(profile));
        profile.setEntraUser(user);
        firmUserManagers.add(entraUserRepository.saveAndFlush(user));


        // Set up Multi-firm User
        user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "External", "FirmOne");
        user.setMultiFirmUser(true);
        profile = buildLaaUserProfile(user, UserType.EXTERNAL, true);
        profile.setAppRoles(Set.of());
        profile.setFirm(testFirm1);
        user.setUserProfiles(Set.of(profile));
        profile.setEntraUser(user);
        multiFirmUsers.add(entraUserRepository.saveAndFlush(user));

        // Set up Security Response User
        user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "Internal", "SecurityResponseUser");
        profile = buildLaaUserProfile(user, UserType.INTERNAL, true);
        AppRole informationAndAssuranceRole = allAppRoles.stream()
                .filter(AppRole::isAuthzRole)
                .filter(role -> role.getName().equals("Security Response"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find app role"));
        profile.setAppRoles(Set.of(informationAndAssuranceRole));
        user.setUserProfiles(Set.of(profile));
        profile.setEntraUser(user);
        securityResponseUsers.add(entraUserRepository.saveAndFlush(user));

        user = buildEntraUser(UUID.randomUUID().toString(), String.format("test%d@test.com", emailIndex++), "SiLAS", "AdminUser");
        profile = buildLaaUserProfile(user, UserType.INTERNAL, true);
        AppRole silasAdminRole = allAppRoles.stream()
                .filter(AppRole::isAuthzRole)
                .filter(role -> role.getName().equals("SILAS System Administration"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find app role"));
        profile.setAppRoles(Set.of(silasAdminRole));
        user.setUserProfiles(Set.of(profile));
        profile.setEntraUser(user);
        silasAdmins.add(entraUserRepository.saveAndFlush(user));


        allUsers.addAll(internalUsersNoRoles);
        allUsers.addAll(internalUserManagers);
        allUsers.addAll(internalAndExternalUserManagers);
        allUsers.addAll(internalWithExternalOnlyUserManagers);
        allUsers.addAll(externalOnlyUserManagers);
        allUsers.addAll(firmUserManagers);
        allUsers.addAll(externalUsersNoRoles);
        allUsers.addAll(externalUserAdmins);
        allUsers.addAll(globalAdmins);
        allUsers.addAll(internalUserViewers);
        allUsers.addAll(externalUserViewers);
        allUsers.addAll(multiFirmUsers);
        allUsers.addAll(securityResponseUsers);
        allUsers.addAll(silasAdmins);
    }

    protected void clearRepositories() {
        TransactionStatus txStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            // Temporarily disable constraint triggers during cleanup
            entityManager.createNativeQuery("SET session_replication_role = replica").executeUpdate();
            
            userProfileRepository.deleteAllInBatch();
            entraUserRepository.deleteAllInBatch();
            firmRepository.deleteAllInBatch();
            officeRepository.deleteAllInBatch();
            
            entityManager.createNativeQuery("SET session_replication_role = DEFAULT").executeUpdate();
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }

    protected Firm createChildFirm(Firm parent, String name, String code) {
        Firm child = buildChildFirm(name, code, parent);
        child = firmRepository.saveAndFlush(child);
        return child;
    }

    protected EntraUser createExternalUserAtFirm(String email, Firm firm) {
        EntraUser user = buildEntraUser(UUID.randomUUID().toString(), email, "Ext", "User");
        user = entraUserRepository.saveAndFlush(user);
        UserProfile profile = buildLaaUserProfile(user, UserType.EXTERNAL, true);
        profile.setFirm(firm);
        profile.setAppRoles(Set.of());
        user.setUserProfiles(Set.of(profile));
        userProfileRepository.saveAndFlush(profile);
        return user;
    }

    protected EntraUser createExternalFirmUserManagerAtFirm(String email, Firm firm) {
        EntraUser user = buildEntraUser(UUID.randomUUID().toString(), email, "Ext", "FUM");
        user = entraUserRepository.saveAndFlush(user);
        UserProfile profile = buildLaaUserProfile(user, UserType.EXTERNAL, true);
        profile.setFirm(firm);
        profile.setAppRoles(Set.of(getFirmUserManagerRole()));
        user.setUserProfiles(Set.of(profile));
        userProfileRepository.saveAndFlush(profile);
        return user;
    }

    protected AppRole getFirmUserManagerRole() {
        return appRoleRepository.findAllWithPermissions().stream()
                .filter(AppRole::isAuthzRole)
                .filter(r -> r.getName().equals("Firm User Manager"))
                .findFirst().orElseThrow();
    }

    protected void setParentFirmType(Firm parentFirm) {
        parentFirm.setType(FirmType.CHAMBERS);
        firmRepository.saveAndFlush(parentFirm);
    }
}
