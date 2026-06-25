package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import uk.gov.justice.laa.portal.landingpage.entity.DeleteUserReason;

@DataJpaTest
public class DeleteUserReasonRepositoryTest extends BaseRepositoryTest {

    private static final Set<String> SYSTEM_GENERATED_CODES = Set.of(
            "ExpiredInvitation",
            "NotActiveAfterMaxLifetime",
            "NoGroupsDelete"
    );

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private DeleteUserReasonRepository deleteUserReasonRepository;

    @Test
    void systemGeneratedReasons_areExactlyTheThreePollingServiceCodes() {
        List<DeleteUserReason> systemReasons = deleteUserReasonRepository.findAllBySystemGenerated(true);

        assertThat(systemReasons).isNotEmpty();
        assertThat(systemReasons)
                .extracting(DeleteUserReason::getCode)
                .containsExactlyInAnyOrderElementsOf(SYSTEM_GENERATED_CODES);
    }

    @Test
    void systemGeneratedReasons_areNotEditableByAnyUser() {
        List<DeleteUserReason> systemReasons = deleteUserReasonRepository.findAllBySystemGenerated(true);

        assertThat(systemReasons).allSatisfy(reason -> {
            assertThat(reason.isEditableByInternalUser())
                    .as("System-generated reason '%s' should not be editable by internal users", reason.getCode())
                    .isFalse();
            assertThat(reason.isEditableByExternalUser())
                    .as("System-generated reason '%s' should not be editable by external users", reason.getCode())
                    .isFalse();
        });
    }

    @Test
    void uiSelectableReasons_areNotMarkedSystemGenerated() {
        List<DeleteUserReason> uiReasons = deleteUserReasonRepository.findAllBySystemGenerated(false);

        assertThat(uiReasons)
                .extracting(DeleteUserReason::getCode)
                .doesNotContainAnyElementsOf(SYSTEM_GENERATED_CODES);
    }

    @Test
    void internalUserSelectableReasons_areNotSystemGenerated() {
        List<DeleteUserReason> internalReasons = deleteUserReasonRepository.findAllByEditableByInternalUser(true);

        assertThat(internalReasons).allSatisfy(reason ->
                assertThat(reason.isSystemGenerated())
                        .as("Reason '%s' is UI-selectable by internal users and must not be system-generated", reason.getCode())
                        .isFalse()
        );
    }

    @Test
    void externalUserSelectableReasons_areNotSystemGenerated() {
        List<DeleteUserReason> externalReasons = deleteUserReasonRepository.findAllByEditableByExternalUser(true);

        assertThat(externalReasons).allSatisfy(reason ->
                assertThat(reason.isSystemGenerated())
                        .as("Reason '%s' is UI-selectable by external users and must not be system-generated", reason.getCode())
                        .isFalse()
        );
    }
}
