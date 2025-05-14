package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphClientConfigTest {

    private MockedStatic<System> mockedSystem;
    private MockedConstruction<ClientSecretCredentialBuilder> mockedClientSecretCredentialBuilder;
    private MockedConstruction<GraphServiceClient> mockedGraphServiceClient;
    private MockedConstruction<TokenAuthAccessProvider> mockedTokenAuthAccessProvider;


    @BeforeEach
    void setUp() {

        mockedClientSecretCredentialBuilder = Mockito.mockConstruction(ClientSecretCredentialBuilder.class,
                (mock, context) -> {
                    when(mock.clientId(anyString())).thenReturn(mock);
                    when(mock.tenantId(anyString())).thenReturn(mock);
                    when(mock.clientSecret(anyString())).thenReturn(mock);
                    when(mock.build()).thenReturn(mock(ClientSecretCredential.class));
                });
    }

    @AfterEach
    void tearDown() {
        if (mockedClientSecretCredentialBuilder != null) {
            mockedClientSecretCredentialBuilder.close();
        }
        if (mockedGraphServiceClient != null) {
            mockedGraphServiceClient.close();
        }
        if (mockedTokenAuthAccessProvider != null) {
            mockedTokenAuthAccessProvider.close();
        }
        if (mockedSystem != null && !mockedSystem.isClosed()) {
            mockedSystem.close();
        }
    }

    @Test
    void getGraphClient_beanAnnotations_areCorrect() throws NoSuchMethodException {

        // Arrange
        Bean beanAnnotation = GraphClientConfig.class.getMethod("getGraphClient").getAnnotation(Bean.class);
        Primary primaryAnnotation = GraphClientConfig.class.getMethod("getGraphClient").getAnnotation(Primary.class);

        // Act & Assert
        assertThat(beanAnnotation).isNotNull();
        assertThat(primaryAnnotation).isNotNull();
    }

    @Test
    void getGraphClientByAccessToken_beanAnnotations_areCorrect() throws NoSuchMethodException {

        // Arrange
        Bean beanAnnotation = GraphClientConfig.class.getMethod("getGraphClientByAccessToken", String.class).getAnnotation(Bean.class);
        Scope scopeAnnotation = GraphClientConfig.class.getMethod("getGraphClientByAccessToken", String.class).getAnnotation(Scope.class);

        // Act & Assert
        assertThat(beanAnnotation).isNotNull();
        assertArrayEquals(new String[] {"graphicServiceClientByAccessToken"}, beanAnnotation.name());
        assertThat(scopeAnnotation).isNotNull();
        assertThat(scopeAnnotation.value()).isEqualTo(BeanDefinition.SCOPE_PROTOTYPE);
    }
}