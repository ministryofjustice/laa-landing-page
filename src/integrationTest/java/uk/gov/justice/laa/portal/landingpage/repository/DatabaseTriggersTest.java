package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.jpa.JpaSystemException;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.time.LocalDateTime;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class DatabaseTriggersTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private FirmRepository firmRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private OfficeRepository officeRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private UserProfileRepository userProfileRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository entraUserRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntityManager entityManager;

    private boolean exceptionChainContains(Throwable t, String needle) {
        String lowerNeedle = needle.toLowerCase();
        Throwable cur = t;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null && msg.toLowerCase().contains(lowerNeedle)) {
                return true;
            }
            String asString = cur.toString();
            if (asString != null && asString.toLowerCase().contains(lowerNeedle)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private Firm newFirm(FirmType type, String name) {
        return Firm.builder()
            .type(type)
            .name(name)
            .code(null)
            .parentFirm(null)
            .build();
    }

    private Office newOffice(Firm firm, String code) {
        return Office.builder()
            .code(code)
            .firm(firm)
            .address(Office.Address.builder()
                .addressLine1("ADDR 1")
                .city("CITY")
                .postcode("AA1 1AA")
                .build())
            .build();
    }

    private EntraUser newUser(String email) {
        return EntraUser.builder()
            .entraOid("oid-" + email)
            .firstName("First")
            .lastName("Last")
            .email(email)
            .userStatus(UserStatus.ACTIVE)
            .createdBy("Test")
            .createdDate(LocalDateTime.now())
            .multiFirmUser(false)
            .build();
    }

    private UserProfile newProfile(EntraUser user, Firm firm, UserType type) {
        return UserProfile.builder()
            .activeProfile(true)
            .userType(type)
            .entraUser(user)
            .firm(firm)
            .userProfileStatus(UserProfileStatus.PENDING)
            .lastCcmsSyncSuccessful(false)
            .createdBy("Test")
            .createdDate(LocalDateTime.now())
            .build();
    }

    @Test
    void testFirmMustHaveAtLeastOneOffice_failsWhenNoOfficeOnCommit() {
        firmRepository.save(newFirm(FirmType.LEGAL_SERVICES_PROVIDER, "Firm A"));
        PersistenceException ex = assertThrows(PersistenceException.class, () -> {
            entityManager.createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE").executeUpdate();
            firmRepository.flush();
        });
        assertTrue(exceptionChainContains(ex, "Firm must have at least one office"));
    }

    @Test
    void testFirmConstraint_allowsFirmWhenOfficeAddedInSameTransaction() {
        Firm firm = firmRepository.save(newFirm(FirmType.LEGAL_SERVICES_PROVIDER, "Firm B"));
        officeRepository.save(newOffice(firm, "OFF-B"));
        assertDoesNotThrow(() -> firmRepository.flush());
    }

    @Test
    void testUserProfileOfficeMustMatchFirm_failsForMismatchedFirm() {
        Firm firm1 = firmRepository.save(newFirm(FirmType.LEGAL_SERVICES_PROVIDER, "Firm C1"));
        Firm firm2 = firmRepository.save(newFirm(FirmType.LEGAL_SERVICES_PROVIDER, "Firm C2"));
        Office officeOfFirm1 = officeRepository.save(newOffice(firm1, "OFF-C1"));
        officeRepository.save(newOffice(firm2, "OFF-C2"));

        EntraUser user = entraUserRepository.save(newUser("c@example.com"));
        UserProfile profile = userProfileRepository.save(newProfile(user, firm2, UserType.EXTERNAL));

        profile.setOffices(new java.util.HashSet<>(Set.of(officeOfFirm1)));
        userProfileRepository.save(profile);

        JpaSystemException ex = assertThrows(JpaSystemException.class, () -> userProfileRepository.flush());
        assertTrue(exceptionChainContains(ex, "Office " + officeOfFirm1.getId() + " does not belong to the user_profile's firm"));
    }

    @Test
    void testAdvocateFirmCannotBeParent() {
        Firm advocateParent = firmRepository.save(newFirm(FirmType.ADVOCATE, "Firm P-ADV"));
        officeRepository.save(newOffice(advocateParent, "OFF-P-ADV"));

        Firm child = newFirm(FirmType.LEGAL_SERVICES_PROVIDER, "Firm CHILD");
        child.setParentFirm(advocateParent);
        child = firmRepository.save(child);
        officeRepository.save(newOffice(child, "OFF-CHILD"));
        JpaSystemException ex = assertThrows(JpaSystemException.class, () -> firmRepository.flush());
        assertTrue(exceptionChainContains(ex, "ADVOCATE firms cannot be parent firms"));
    }

    @Test
    void testCannotChangeParentFirmToAdvocateWhenHasChildren() {
        Firm parent = firmRepository.save(newFirm(FirmType.LEGAL_SERVICES_PROVIDER, "Firm Q"));
        officeRepository.save(newOffice(parent, "OFF-Q"));

        Firm child1 = newFirm(FirmType.LEGAL_SERVICES_PROVIDER, "Firm Q-CHILD1");
        child1.setParentFirm(parent);
        child1 = firmRepository.save(child1);
        officeRepository.save(newOffice(child1, "OFF-Q-C1"));

        Firm child2 = newFirm(FirmType.LEGAL_SERVICES_PROVIDER, "Firm Q-CHILD2");
        child2.setParentFirm(parent);
        child2 = firmRepository.save(child2);
        officeRepository.save(newOffice(child2, "OFF-Q-C2"));

        firmRepository.flush();
        
        Firm managedParent = firmRepository.findById(parent.getId()).orElseThrow();
        managedParent.setType(FirmType.ADVOCATE);
        firmRepository.save(managedParent);
        JpaSystemException jpaEx = assertThrows(JpaSystemException.class, () -> firmRepository.flush());
        assertTrue(exceptionChainContains(jpaEx, "Cannot set firm to ADVOCATE while it is a parent"));
    }

    @Test
    void testPreventInternalUserWithMultiFirmTrue() {
        EntraUser user = entraUserRepository.save(newUser("mf@example.com"));
        userProfileRepository.save(newProfile(user, null, UserType.INTERNAL));

        EntraUser managed = entraUserRepository.findById(user.getId()).orElseThrow();
        managed.setMultiFirmUser(true);
        entraUserRepository.save(managed);
        JpaSystemException ex2 = assertThrows(JpaSystemException.class, () -> entraUserRepository.flush());
        assertTrue(exceptionChainContains(ex2, "Internal users cannot have multi_firm_user=true"));
    }

    @Test
    void testAllowExternalUserWithMultiFirmTrue() {
        EntraUser user = entraUserRepository.save(newUser("ok@example.com"));
        Firm firm = firmRepository.save(newFirm(FirmType.LEGAL_SERVICES_PROVIDER, "Firm OK"));
        officeRepository.save(newOffice(firm, "OFF-OK"));
        userProfileRepository.save(newProfile(user, firm, UserType.EXTERNAL));

        EntraUser managed = entraUserRepository.findById(user.getId()).orElseThrow();
        managed.setMultiFirmUser(true);
        entraUserRepository.save(managed);
        assertDoesNotThrow(() -> entraUserRepository.flush());
    }
}
