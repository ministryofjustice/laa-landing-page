package uk.gov.justice.laa.portal.landingpage.config;

import com.azure.core.credential.TokenCredential;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.justice.laa.portal.landingpage.auth.TokenCredentialFactory;

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

}