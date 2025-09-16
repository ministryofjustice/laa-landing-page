package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DoNothingTestServiceClientTest {

    private ListAppender<ILoggingEvent> logAppender;

    private TechServicesClient techServicesClient;

    @BeforeEach
    public void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(DoNothingTechServicesClient.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        techServicesClient = new DoNothingTechServicesClient();

    }

    @Test
    void testUpdateRoleAssignment() {
        UUID userId = UUID.randomUUID();

        techServicesClient.updateRoleAssignment(userId);

        assertLogMessage("Updating role assignment received on Dummy Tech Services Client for user");
    }

    @Test
    void testRegisterUser() {
        EntraUserDto user = EntraUserDto.builder().build();

        RegisterUserResponse response = techServicesClient.registerNewUser(user);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.isSuccess()).isTrue();
        Assertions.assertThat(response.getCreatedUser()).isNotNull();
        Assertions.assertThat(response.getCreatedUser().getId()).isNotNull();
        assertLogMessage("Register new user request received on Dummy Tech Services Client for user");
    }

    @Test
    void testSendEmailVerification() {
        EntraUserDto user = EntraUserDto.builder().build();

        TechServicesApiResponse<SendUserVerificationEmailResponse> response = techServicesClient.sendEmailVerification(user);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.isSuccess()).isTrue();
        Assertions.assertThat(response.getError()).isNull();
        Assertions.assertThat(response.getData()).isNotNull();
        Assertions.assertThat(response.getData().getMessage()).isNotNull();
        Assertions.assertThat(response.getData().getMessage()).isEqualTo("Activation code has been generated and sent successfully via email.");
        assertLogMessage("Verification email has been resent from Dummy Tech Services Client for user");
    }

    private void assertLogMessage(String message) {
        assertTrue(logAppender.list.stream()
                        .anyMatch(logEvent -> logEvent.getLevel() == Level.INFO
                                && logEvent.getFormattedMessage().contains(message)),
                String.format("Log message not found with level %s and message %s. Actual Logs are: %s", Level.INFO, message,
                        logAppender.list.stream().map(e -> String.format("[%s] %s", Level.INFO, e.getFormattedMessage()))
                                .toList()));
    }

}
