package uk.gov.justice.laa.portal.landingpage.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import static org.assertj.core.api.Assertions.assertThat;

class ReinviteUserAuditEventTest {
    @ParameterizedTest
    @EnumSource(ReinviteUserAuditEvent.Outcome.class)
    void recordsInitiatorTargetAndOutcomeInAuditDetails(ReinviteUserAuditEvent.Outcome outcome) {
        UUID targetUserId = UUID.randomUUID();
        UUID targetUserOid = UUID.randomUUID();
        UUID initiatorOid = UUID.randomUUID();
        AuditUserDetailDto target = AuditUserDetailDto.builder()
                .userId(targetUserId.toString())
                .entraOid(targetUserOid.toString())
                .build();

        ReinviteUserAuditEvent event = new ReinviteUserAuditEvent(target, initiatorOid, outcome);

        assertThat(event.getEventType()).isEqualTo(EventType.USER_REINVITED);
        assertThat(event.getUserId()).isEqualTo(initiatorOid);
        assertThat(event.getDescription())
                .contains(targetUserId.toString(), targetUserOid.toString(), outcome.name())
                .doesNotContain("null");
        assertThat(LocalDateTime.parse(event.getCreatedDate(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))).isNotNull();
    }
}
