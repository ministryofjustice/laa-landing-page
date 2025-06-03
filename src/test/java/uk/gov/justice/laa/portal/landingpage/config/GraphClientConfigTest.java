package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.core.credential.TokenCredential;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.config.BeanDefinition;
import uk.gov.justice.laa.portal.landingpage.auth.TokenCredentialFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GraphClientConfigTest {

    @Mock
    private TokenCredentialFactory credentialFactory;
    @Mock
    private TokenCredential tokenCredential;

    private GraphClientConfig graphClientConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(credentialFactory.createCredential()).thenReturn(tokenCredential);
        graphClientConfig = new GraphClientConfig(credentialFactory);
    }

    @Test
    void graphServiceClient_shouldReturnGraphServiceClientInstance() {
        GraphServiceClient client = graphClientConfig.graphServiceClient();

        assertThat(client).isNotNull();
        verify(credentialFactory).createCredential();
    }

    @Test
    void graphClientByAccessToken_shouldReturnGraphServiceClientInstance() {
        String accessToken = UUID.randomUUID().toString();
        GraphServiceClient client = graphClientConfig.graphClientByAccessToken(accessToken);

        assertThat(client).isNotNull();
    }

    @Test
    void graphClientByAccessToken_shouldHaveBeanAndScopeAnnotations() throws NoSuchMethodException {
        Method method = GraphClientConfig.class.getMethod("graphClientByAccessToken", String.class);

        assertThat(method.getAnnotation(Bean.class)).isNotNull();
        assertThat(method.getAnnotation(Scope.class)).isNotNull();
        assertThat(method.getAnnotation(Scope.class).value()).isEqualTo(BeanDefinition.SCOPE_PROTOTYPE);
    }

}