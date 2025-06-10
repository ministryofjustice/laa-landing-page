package uk.gov.justice.laa.portal.landingpage.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRegistrationRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The class to populate dummy data in the local db. Flag to decide whether to populate the dummy data or not and
 * what user details to user for creation can be configured in application.properties file
 */
@Component
public class DemoDataPopulator {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRegistrationRepository entraAppRegistrationRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private FirmRepository firmRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private OfficeRepository officeRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository entraUserRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRepository laaAppRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRoleRepository laaAppRoleRepository;


    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserProfileRepository laaUserProfileRepository;

    @Value("${app.user.email}")
    private String userEmail;
    @Value("${app.user.first.name}")
    private String userFirstName;
    @Value("${app.user.last.name}")
    private String userLastName;
    @Value("${app.populate.dummy-data}")
    private boolean populateDummyData;

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
                .appRoles(HashSet.newHashSet(11)).build();
    }

    protected AppRole buildLaaAppRole(App app, String name) {
        return AppRole.builder().name(name).app(app).build();
    }

    protected UserProfile buildLaaUserProfile(EntraUser entraUser, UserType userType) {
        return UserProfile.builder().entraUser(entraUser).defaultProfile(true)
                .userType(userType).appRoles(HashSet.newHashSet(11))
                .createdDate(LocalDateTime.now()).createdBy("Test").build();
    }


    @EventListener
    public void appReady(ApplicationReadyEvent event) {
        if (populateDummyData) {
            initialTestData();
        }
    }

    private void initialTestData() {
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

        AppRegistration entraAppRegistration1 = buildEntraAppRegistration("Entra App 1");
        AppRegistration entraAppRegistration2 = buildEntraAppRegistration("Entra App 2");
        AppRegistration entraAppRegistration3 = buildEntraAppRegistration("Entra App 3");
        AppRegistration entraAppRegistration4 = buildEntraAppRegistration("Entra App 4");
        AppRegistration entraAppRegistration5 = buildEntraAppRegistration("Entra App 5");
        entraAppRegistrationRepository.saveAll(Arrays.asList(entraAppRegistration1, entraAppRegistration2,
                entraAppRegistration3, entraAppRegistration4, entraAppRegistration5));

        EntraUser entraUser = buildEntraUser(userEmail, userFirstName, userLastName);
        entraUser.getUserAppRegistrations().addAll(Set.of(entraAppRegistration1, entraAppRegistration2,
                entraAppRegistration3, entraAppRegistration4, entraAppRegistration5));
        entraUserRepository.saveAll(List.of(entraUser));


        App laaApp1 = buildLaaApp(entraAppRegistration1, "LAA App1");
        App laaApp2 = buildLaaApp(entraAppRegistration2, "LAA App2");
        App laaApp3 = buildLaaApp(entraAppRegistration3, "LAA App3");
        App laaApp4 = buildLaaApp(entraAppRegistration4, "LAA App4");
        App laaApp5 = buildLaaApp(entraAppRegistration5, "LAA App5");
        laaAppRepository.saveAll(Arrays.asList(laaApp1, laaApp2, laaApp3, laaApp4, laaApp5));

        AppRole appRole1 = buildLaaAppRole(laaApp1, "Admin");
        AppRole appRole2 = buildLaaAppRole(laaApp2, "Admin");
        AppRole appRole3 = buildLaaAppRole(laaApp3, "Admin");
        AppRole appRole4 = buildLaaAppRole(laaApp4, "Admin");
        AppRole appRole5 = buildLaaAppRole(laaApp5, "Admin");

        laaAppRoleRepository.saveAll(Arrays.asList(appRole1, appRole2, appRole3, appRole4, appRole5));

        UserProfile laaUserProfile1 = buildLaaUserProfile(entraUser, UserType.EXTERNAL_SINGLE_FIRM_ADMIN);
        laaUserProfile1.getAppRoles().addAll(Set.of(appRole1, appRole2, appRole3, appRole4, appRole5));
        laaUserProfile1.setFirm(firm1);

        laaUserProfileRepository.saveAll(List.of(laaUserProfile1));

        System.out.println("Dummy Data Populated!!");

    }

}
