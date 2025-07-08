package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.utils.RestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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
    void logsUser_fromGraph() {
        service.logoutUser("token");
        String url = "https://graph.microsoft.com/v1.0/me/revokeSignInSessions";
        mockedRestUtils.verify(() -> RestUtils.postGraphApi(eq("token"), eq(url), any()));
    }
}
