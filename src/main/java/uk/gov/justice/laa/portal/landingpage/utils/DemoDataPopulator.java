package uk.gov.justice.laa.portal.landingpage.utils;

import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The class to populate dummy data in the local db. Flag to decide whether to populate the dummy data or not and
 * what user details to user for creation can be configured in application.properties file.
 * Some data is being populated from entra to keep names consistent with entra.
 */
@Component
@Profile("!production")
public class DemoDataPopulator {

    private final FirmRepository firmRepository;

    private final OfficeRepository officeRepository;

    private final EntraUserRepository entraUserRepository;

    private final AppRepository laaAppRepository;

    private final AppRoleRepository laaAppRoleRepository;

    private final UserProfileRepository laaUserProfileRepository;

    private final GraphServiceClient graphServiceClient;

    @Value("${app.test.admin.userPrincipals}")
    private Set<String> adminUserPrincipals;

    @Value("${app.test.nonadmin.userPrincipals}")
    private Set<String> nonAdminUserPrincipals;

    @Value("${app.test.internal.userPrincipals}")
    private Set<String> internalUserPrincipals;

    @Value("${app.populate.dummy-data}")
    private boolean populateDummyData;

    @Value("${app.civil.apply.name}")
    private String appCivilApplyName;

    @Value("${app.civil.apply.details}")
    private String appCivilApplyDetails;

    @Value("${app.crime.apply.name}")
    private String appCrimeApplyName;

    @Value("${app.crime.apply.details}")
    private String appCrimeApplyDetails;

    @Value("${app.pui.name}")
    private String appPuiName;

    @Value("${app.pui.details}")
    private String appPuiDetails;

    @Value("${app.submit.crime.form.name}")
    private String appSubmitCrimeFormName;

    @Value("${app.submit.crime.form.details}")
    private String appSubmitCrimeFormDetails;

    public DemoDataPopulator(FirmRepository firmRepository,
                             OfficeRepository officeRepository, EntraUserRepository entraUserRepository,
                             AppRepository laaAppRepository, AppRoleRepository laaAppRoleRepository,
                             UserProfileRepository laaUserProfileRepository, GraphServiceClient graphServiceClient) {
        this.firmRepository = firmRepository;
        this.officeRepository = officeRepository;
        this.entraUserRepository = entraUserRepository;
        this.laaAppRepository = laaAppRepository;
        this.laaAppRoleRepository = laaAppRoleRepository;
        this.laaUserProfileRepository = laaUserProfileRepository;
        this.graphServiceClient = graphServiceClient;
    }

    protected EntraUser buildEntraUser(String userPrincipal, String entraId) {
        String email = getEmailFromUserPrinciple(userPrincipal);

        return EntraUser.builder().email(email)
                .entraOid(entraId)
                .userProfiles(HashSet.newHashSet(11))
                .firstName(email).lastName("LastName")
                .userStatus(UserStatus.ACTIVE).startDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

    protected EntraUser buildEntraUser(User user) {
        String email = user.getMail() != null ? user.getMail() : getEmailFromUserPrinciple(user.getUserPrincipalName());
        String firstName = getFirstName(user);
        String lastName = getSurname(user);

        return EntraUser.builder().email(email)
                .entraOid(user.getId())
                .userProfiles(HashSet.newHashSet(11))
                .firstName(firstName).lastName(lastName)
                .userStatus(UserStatus.ACTIVE).startDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

    protected String getSurname(User user) {
        if (user.getSurname() != null) {
            return user.getSurname();
        } else if (user.getDisplayName() != null) {
            String[] parts = user.getDisplayName().split(" ");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return "Surname";
    }

    protected String getFirstName(User user) {
        if (user.getGivenName() != null) {
            return user.getGivenName();
        } else if (user.getDisplayName() != null) {
            String[] parts = user.getDisplayName().split(" ");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return "Firstname";
    }

    protected String getEmailFromUserPrinciple(String userPrincipalName) {
        if (userPrincipalName != null && userPrincipalName.contains("#")) {
            String emailPart = userPrincipalName.split("#")[0];
            int replacementPos = emailPart.lastIndexOf('_');
            return emailPart.substring(0, replacementPos) + "@" + emailPart.substring(replacementPos + 1);
        } else {
            return userPrincipalName;
        }
    }

    protected Firm buildFirm(String name) {
        return Firm.builder().name(name).offices(HashSet.newHashSet(11))
                .type(FirmType.INDIVIDUAL).build();
    }

    protected Office buildOffice(Firm firm, String name, String address, String phone) {
        return Office.builder().name(name).address(address).phone(phone).firm(firm).build();
    }

    protected App buildLaaApp(String entraAppOid, String name) {
        return App.builder().name(name).entraAppId(entraAppOid).appRoles(HashSet.newHashSet(11)).build();
    }

    protected AppRole buildLaaAppRole(App app, String name, RoleType roleType) {
        return AppRole.builder().name(name).roleType(roleType)
                .description(name).authzRole(false).app(app).build();
    }

    protected UserProfile buildLaaUserProfile(EntraUser entraUser, UserType userType) {
        return UserProfile.builder().entraUser(entraUser).activeProfile(true)
                .userType(userType).appRoles(HashSet.newHashSet(11))
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }

    private UserType getUserType(String userPrincipal) {
        if (adminUserPrincipals.contains(userPrincipal)) {
            return UserType.EXTERNAL_SINGLE_FIRM_ADMIN;
        } else if (internalUserPrincipals.contains(userPrincipal)) {
            return UserType.INTERNAL;
        } else {
            return UserType.EXTERNAL_SINGLE_FIRM;
        }
    }


    @EventListener
    public void appReady(ApplicationReadyEvent event) {
        if (populateDummyData) {
            initialTestData();
        }
    }

    private void initialTestData() {

        try {

            Firm firm = firmRepository.findFirmByName("Firm One");
            if (firm == null) {


                Firm firm1 = buildFirm("Firm One");
                Firm firm2 = buildFirm("Firm Two");
                firmRepository.saveAll(Arrays.asList(firm1, firm2));

                Office office1 = buildOffice(firm1, "F1Office1", "Addr 1", "12345");
                Office office2 = buildOffice(firm1, "F1Office2", "Addr 2", "23456");
                Office office3 = buildOffice(firm2, "F2Office1", "Addr 3", "34567");
                Office office4 = buildOffice(firm2, "F2Office2", "Addr 4", "45678");
                Office office5 = buildOffice(firm2, "F2Office3", "Addr 5", "56789");
                firm1.getOffices().addAll(Set.of(office1, office2));
                firm2.getOffices().addAll(Set.of(office3, office4, office5));
                officeRepository.saveAll(Arrays.asList(office1, office2, office3, office4, office5));

                List<User> users = Objects.requireNonNull(graphServiceClient.users().get(requestConfig -> {
                    assert requestConfig.queryParameters != null;
                    requestConfig.queryParameters.select = new String[]{"id", "displayName", "mail", "mobilePhone", "userPrincipalName", "userType", "surname", "givenName", "signInActivity"};
                    requestConfig.queryParameters.top = 10;
                })).getValue();

                List<EntraUser> entraUsers = new ArrayList<>();
                assert users != null;
                for (User user : users) {
                    EntraUser entraUser = buildEntraUser(user);
                    entraUsers.add(entraUser);
                }

                entraUserRepository.saveAll(entraUsers);

                List<UserProfile> userProfiles = new ArrayList<>();

                for (EntraUser entraUser : entraUsers) {
                    UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL_SINGLE_FIRM_ADMIN);
                    userProfile.setFirm(firm1);
                    userProfiles.add(userProfile);
                }


                laaUserProfileRepository.saveAll(userProfiles);

                System.out.println("Dummy Data Populated!!");
            }
        } catch (Exception ex) {
            System.err.println("Error populating dummy data!!");
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }

        // Now trying to populate custom-defined apps and roles
        List<Pair<String, String>> appDetailPairs = List.of(Pair.of(appPuiDetails, appPuiName), Pair.of(appCivilApplyDetails, appCivilApplyName),
                Pair.of(appCrimeApplyDetails, appCrimeApplyName), Pair.of(appSubmitCrimeFormDetails, appSubmitCrimeFormName));

        for (Pair<String, String> appDetailPair : appDetailPairs) {

            if (appDetailPair.getRight() == null) {
                return;
            }

            try {
                String[] values = appDetailPair.getLeft() == null ? new String[0] : appDetailPair.getLeft().split("//");
                String oid = values.length > 0 ? values[0] : "";
                String securityGroupName = values.length > 1 ? values[1] : "";
                String securityGroupOid = values.length > 2 ? values[2] : "";
                App app = laaAppRepository.findByEntraAppIdOrName(oid,
                        appDetailPair.getRight()).orElse(buildLaaApp(oid, appDetailPair.getRight()));

                // Update if the record already exists
                app.setEntraAppId(oid);
                app.setSecurityGroupName(securityGroupName);
                app.setSecurityGroupOid(securityGroupOid);
                String currentAppName = app.getName();
                app.setName(appDetailPair.getRight());
                laaAppRepository.save(app);

                AppRole internalRole = laaAppRoleRepository.findByName(currentAppName.toUpperCase() + "_VIEWER_INTERN")
                        .orElse(buildLaaAppRole(app, app.getName().toUpperCase() + "_VIEWER_INTERN", RoleType.INTERNAL));
                AppRole externalRole = laaAppRoleRepository.findByName(currentAppName.toUpperCase() + "_VIEWER_EXTERN")
                        .orElse(buildLaaAppRole(app, app.getName().toUpperCase() + "_VIEWER_EXTERN", RoleType.EXTERNAL));
                laaAppRoleRepository.save(internalRole);
                laaAppRoleRepository.save(externalRole);


            } catch (Exception ex) {
                System.out.println("Unable to add app to the list of apps in the database: " + appDetailPair.getRight());
                ex.printStackTrace();
            }
        }


        // Users
        Set<String> userPrinciples = new HashSet<>();
        userPrinciples.addAll(adminUserPrincipals == null ? Collections.emptySet() : adminUserPrincipals);
        userPrinciples.addAll(nonAdminUserPrincipals == null ? Collections.emptySet() : nonAdminUserPrincipals);
        userPrinciples.addAll(internalUserPrincipals == null ? Collections.emptySet() : internalUserPrincipals);
        List<AppRole> appRoles = laaAppRoleRepository.findAll();
        if (adminUserPrincipals != null) {
            for (String userPrincipal : userPrinciples) {
                try {
                    if (!userPrincipal.contains(":")) {
                        throw new RuntimeException("Invalid user principal format, the format should be <userprinciple>:<entraid>");
                    }
                    String mail = userPrincipal.split(":")[0];
                    String entraId = userPrincipal.split(":")[1];
                    EntraUser entraUser = entraUserRepository.findByEntraOid(entraId).orElse(buildEntraUser(mail, entraId));
                    boolean isNewUser = entraUser.getId() == null;
                    entraUserRepository.save(entraUser);
                    if (isNewUser || entraUser.getUserProfiles() == null || entraUser.getUserProfiles().isEmpty()) {
                        UserType userType = getUserType(userPrincipal);
                        UserProfile userProfile = buildLaaUserProfile(entraUser, userType);
                        userProfile.getAppRoles().addAll(appRoles);
                        if (userType != UserType.INTERNAL) {
                            userProfile.setFirm(firmRepository.findFirmByName("Firm One"));
                        }
                        laaUserProfileRepository.save(userProfile);
                    }
                } catch (Exception e) {
                    System.err.println("Unable to add user to the list of users in the database, the user may not present in entra: " + userPrincipal);
                    e.printStackTrace();
                    System.err.println("Continuing with the list of users in the database");
                }
            }
        }


    }

}
