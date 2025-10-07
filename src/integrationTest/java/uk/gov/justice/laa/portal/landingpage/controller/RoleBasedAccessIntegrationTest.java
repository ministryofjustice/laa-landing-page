package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
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
    protected List<EntraUser> globalAdmins = new ArrayList<>();
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
        Firm firm1 = buildFirm("firm1", "firm1");
        testFirm1 = firmRepository.saveAndFlush(firm1);
        Office firm1Office1 = buildOffice(testFirm1, "Firm1Office1", "Firm 1 Office 1", "123456789", "F1Office1Code");
        Office firm1Office2 = buildOffice(testFirm1, "Firm1Office2", "Firm 1 Office 2", "123456789", "F1Office2Code");
        firm1Office1 = officeRepository.saveAndFlush(firm1Office1);
        firm1Office2 = officeRepository.saveAndFlush(firm1Office2);
        testFirm1.setOffices(Set.of(firm1Office1, firm1Office2));
        firmRepository.save(testFirm1);
        Firm firm2 = buildFirm("firm2", "firm2");
        testFirm2 = firmRepository.saveAndFlush(firm2);
        Office firm2Office1 = buildOffice(testFirm2, "Firm2Office1", "Firm 2 Office 1", "123456789", "F2Office1Code");
        Office firm2Office2 = buildOffice(testFirm2, "Firm2Office2", "Firm 2 Office 2", "123456789", "F2Office2Code");
        firm2Office1 = officeRepository.saveAndFlush(firm2Office1);
        firm2Office2 = officeRepository.saveAndFlush(firm2Office2);
        testFirm2.setOffices(Set.of(firm2Office1, firm2Office2));
        testFirm2 = firmRepository.save(testFirm2);
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


        allUsers.addAll(internalUsersNoRoles);
        allUsers.addAll(internalUserManagers);
        allUsers.addAll(internalAndExternalUserManagers);
        allUsers.addAll(internalWithExternalOnlyUserManagers);
        allUsers.addAll(externalOnlyUserManagers);
        allUsers.addAll(externalUsersNoRoles);
        allUsers.addAll(externalUserAdmins);
        allUsers.addAll(globalAdmins);
        allUsers.addAll(internalUserViewers);
        allUsers.addAll(externalUserViewers);
    }

    protected void clearRepositories() {
        userProfileRepository.deleteAll();
        entraUserRepository.deleteAll();
        officeRepository.deleteAll(); // Delete offices first to avoid foreign key constraint violation
        firmRepository.deleteAll();
    }
}
