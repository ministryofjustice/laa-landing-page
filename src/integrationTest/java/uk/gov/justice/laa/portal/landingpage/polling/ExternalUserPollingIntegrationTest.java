package uk.gov.justice.laa.portal.landingpage.polling;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import uk.gov.justice.laa.portal.landingpage.controller.BaseIntegrationTest;
import uk.gov.justice.laa.portal.landingpage.entity.DisableType;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserAccountStatusAuditRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.scheduler.ExternalUserPolling;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUsersResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesErrorResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesUser;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringBootApplicationProperties"})
// Enable polling for this test only.
@TestPropertySource(properties = {
    "external.user.polling.enabled=true"
})
public class ExternalUserPollingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EntraUserRepository entraUserRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private FirmRepository firmRepository;

    @Autowired
    private UserAccountStatusAuditRepository userAccountStatusAuditRepository;

    @Autowired
    private ExternalUserPolling externalUserPolling;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private TechServicesClient techServicesClient;

    @Test
    public void testExternalUserPollingDisablesUserWithReason() {
        EntraUser entraUser = buildEntraUser(UUID.randomUUID().toString(), "externalpollingdisable@test.com", "External", "Disable");
        entraUser.setEnabled(true);
        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL, true, "Global Admin");
        userProfile.setFirm(buildAndSaveFirm());
        entraUser = entraUserRepository.saveAndFlush(entraUser);
        userProfileRepository.saveAndFlush(userProfile);

        stubGetUsers(TechServicesUser.builder()
                .id(entraUser.getEntraOid())
                .accountEnabled(false)
                .customSecurityAttributes(TechServicesUser.CustomSecurityAttributes.builder()
                        .guestUserStatus(TechServicesUser.GuestUserStatus.builder()
                                .disabledReason("Absence")
                                .build())
                        .build())
                .build());

        externalUserPolling.poll();

        EntraUser updatedUser = entraUserRepository.findById(entraUser.getId()).orElseThrow();
        assertThat(updatedUser.isEnabled()).isFalse();
        assertThat(updatedUser.getDisableType()).isEqualTo(DisableType.SYNC);

        List<UserAccountStatusAudit> audits = userAccountStatusAuditRepository.findByEntraUser(updatedUser);
        assertThat(audits).anySatisfy(audit -> {
            assertThat(audit.getStatusChange()).isEqualTo(UserAccountStatus.DISABLED);
            assertThat(audit.getStatusChangedBy()).isEqualTo("External user sync");
            assertThat(audit.getDisableUserReason().getEntraDescription()).isEqualTo("Absence");
        });
    }

    @Test
    public void testExternalUserPollingEnablesUser() {
        EntraUser entraUser = buildDeactiveEntraUser(UUID.randomUUID().toString(), "externalpollingenable@test.com", "External", "Enable", false);
        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL, true, "Global Admin");
        userProfile.setFirm(buildAndSaveFirm());
        entraUser = entraUserRepository.saveAndFlush(entraUser);
        userProfileRepository.saveAndFlush(userProfile);

        stubGetUsers(TechServicesUser.builder()
                .id(entraUser.getEntraOid())
                .accountEnabled(true)
                .build());

        externalUserPolling.poll();

        EntraUser updatedUser = entraUserRepository.findById(entraUser.getId()).orElseThrow();
        assertThat(updatedUser.isEnabled()).isTrue();

        List<UserAccountStatusAudit> audits = userAccountStatusAuditRepository.findByEntraUser(updatedUser);
        assertThat(audits).anySatisfy(audit -> {
            assertThat(audit.getStatusChange()).isEqualTo(UserAccountStatus.ENABLED);
            assertThat(audit.getStatusChangedBy()).isEqualTo("External user sync");
        });
    }

    @Test
    public void testExternalUserPollingDeletesUser() {
        EntraUser entraUser = buildEntraUser(UUID.randomUUID().toString(), "externalpollingdelete@test.com", "External", "Delete");
        entraUser.setEnabled(true);
        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL, true, "Global Admin");
        userProfile.setFirm(buildAndSaveFirm());
        entraUser = entraUserRepository.saveAndFlush(entraUser);
        userProfile = userProfileRepository.saveAndFlush(userProfile);

        stubGetUsers(TechServicesUser.builder()
                .id(entraUser.getEntraOid())
                .deleted(true)
                .build());

        externalUserPolling.poll();

        assertThat(entraUserRepository.findById(entraUser.getId())).isEmpty();
        assertThat(userProfileRepository.findById(userProfile.getId())).isEmpty();

        List<UserAccountStatusAudit> deletedAudits = userAccountStatusAuditRepository.findAll().stream()
                .filter(audit -> audit.getStatusChange() == UserAccountStatus.DELETED
                        && "externalpollingdelete@test.com".equals(audit.getUserEmail()))
                .toList();
        assertThat(deletedAudits).hasSize(1);
    }

    @Test
    public void testExternalUserPollingUpdatesUserFields() {
        EntraUser entraUser = buildEntraUser(UUID.randomUUID().toString(), "externalpollingupdate@test.com", "OldFirst", "OldLast");
        entraUser.setEnabled(true);
        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL, true, "Global Admin");
        userProfile.setFirm(buildAndSaveFirm());
        entraUser = entraUserRepository.saveAndFlush(entraUser);
        userProfileRepository.saveAndFlush(userProfile);

        stubGetUsers(TechServicesUser.builder()
                .id(entraUser.getEntraOid())
                .accountEnabled(true)
                .givenName("NewFirst")
                .surname("NewLast")
                .email("externalpollingupdate-new@test.com")
                .isMailOnly(true)
                .build());

        externalUserPolling.poll();

        EntraUser updatedUser = entraUserRepository.findById(entraUser.getId()).orElseThrow();
        assertThat(updatedUser.getFirstName()).isEqualTo("NewFirst");
        assertThat(updatedUser.getLastName()).isEqualTo("NewLast");
        assertThat(updatedUser.getEmail()).isEqualTo("externalpollingupdate-new@test.com");
        assertThat(updatedUser.isMailOnly()).isTrue();
        assertThat(updatedUser.getLastSyncedOn()).isNotNull();
    }

    @Test
    public void testExternalUserPollingNoOpsWhenNoUsersFound() {
        EntraUser entraUser = buildEntraUser(UUID.randomUUID().toString(), "externalpollingnoop@test.com", "No", "Op");
        entraUser.setEnabled(true);
        UserProfile userProfile = buildLaaUserProfile(entraUser, UserType.EXTERNAL, true, "Global Admin");
        userProfile.setFirm(buildAndSaveFirm());
        entraUser = entraUserRepository.saveAndFlush(entraUser);
        userProfileRepository.saveAndFlush(userProfile);

        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(TechServicesApiResponse.error(
                TechServicesErrorResponse.builder().success(false).message("Users not found.").build()));

        externalUserPolling.poll();

        EntraUser unchangedUser = entraUserRepository.findById(entraUser.getId()).orElseThrow();
        assertThat(unchangedUser.isEnabled()).isTrue();
    }

    private Firm buildAndSaveFirm() {
        // Both rows must commit together - the firm-has-office DB trigger is enforced at commit time.
        return transactionTemplate.execute(status -> {
            Firm firm = firmRepository.saveAndFlush(buildFirm("Test Firm " + UUID.randomUUID(), UUID.randomUUID().toString().substring(0, 8)));
            officeRepository.saveAndFlush(buildOffice(firm, "1 Test Street", UUID.randomUUID().toString().substring(0, 8)));
            return firm;
        });
    }

    private void stubGetUsers(TechServicesUser... users) {
        GetUsersResponse response = GetUsersResponse.builder()
                .success(true)
                .message("Success")
                .users(List.of(users))
                .build();
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(TechServicesApiResponse.success(response));
    }

}
