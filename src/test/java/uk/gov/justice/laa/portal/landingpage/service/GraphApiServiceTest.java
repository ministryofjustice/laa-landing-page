package uk.gov.justice.laa.portal.landingpage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.microsoft.graph.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;
import uk.gov.justice.laa.portal.landingpage.utils.RestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class GraphApiServiceTest {
    @InjectMocks
    private GraphApiService service;

    private MockedStatic<RestUtils> mockedRestUtils;

    @BeforeEach
    void setUp() {
        mockedRestUtils = mockStatic(RestUtils.class);
    }

    @AfterEach
    void cleanUp() {
        if (mockedRestUtils != null) {
            mockedRestUtils.close();
        }
    }

    @Nested
    class GetUserProfile {

        @Test
        void deserialisesUser_fromGraphJson() {
            // Arrange
            String json = """
                    {"displayName":"Alice Smith","mail":"alice@example.com"}""";
            mockedRestUtils.when(() -> RestUtils.getGraphApi(anyString(), anyString()))
                    .thenReturn(json);

            // Act
            User user = service.getUserProfile("");

            // Assert
            assertThat(user.getDisplayName()).isEqualTo("Alice Smith");
            mockedRestUtils.verify(() -> RestUtils.getGraphApi(anyString(), anyString()));
        }
    }

    @Test
    void logoutUser_fromGraph_Ok() {
        service.logoutUser("token");
        String url = "https://graph.microsoft.com/v1.0/me/revokeSignInSessions";
        mockedRestUtils.verify(() -> RestUtils.postGraphApi(eq("token"), eq(url), any()));
    }

    @Test
    void logoutUser_fromGraph_Error() {
        String url = "https://graph.microsoft.com/v1.0/me/revokeSignInSessions";
        given(RestUtils.postGraphApi(eq("token"), eq(url), any())).willAnswer(invocation -> { throw new Exception("abc msg"); });
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(GraphApiService.class);
        service.logoutUser("token");
        List<ILoggingEvent> warningLogs = LogMonitoring.getLogsByLevel(listAppender, Level.ERROR);
        assertThat(warningLogs.size()).isEqualTo(1);
    }
}
