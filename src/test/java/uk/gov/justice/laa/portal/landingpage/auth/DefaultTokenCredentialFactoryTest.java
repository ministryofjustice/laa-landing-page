package uk.gov.justice.laa.portal.landingpage.auth;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class DefaultTokenCredentialFactoryTest {

    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final String TENANT_ID = "test-tenant-id";

    private DefaultTokenCredentialFactory factory;
    private MockedConstruction<ClientSecretCredentialBuilder> mockedBuilder;
    private ClientSecretCredential mockCredential;

    @BeforeEach
    void setUp() {
        mockCredential = mock(ClientSecretCredential.class);

        mockedBuilder = mockConstruction(ClientSecretCredentialBuilder.class,
                (builder, context) -> {
                    when(builder.clientId(CLIENT_ID)).thenReturn(builder);
                    when(builder.clientSecret(CLIENT_SECRET)).thenReturn(builder);
                    when(builder.tenantId(TENANT_ID)).thenReturn(builder);
                    when(builder.build()).thenReturn(mockCredential);
                });

        factory = new DefaultTokenCredentialFactory(CLIENT_ID, CLIENT_SECRET, TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        if (mockedBuilder != null) {
            mockedBuilder.close();
        }
    }

    @Test
    void createCredential_shouldBuildCredentialWithCorrectParameters() {
        var credential = factory.createCredential();

        assertThat(credential).isSameAs(mockCredential);

        ClientSecretCredentialBuilder builder = mockedBuilder.constructed().get(0);
        verify(builder).clientId(CLIENT_ID);
        verify(builder).clientSecret(CLIENT_SECRET);
        verify(builder).tenantId(TENANT_ID);
        verify(builder).build();
    }
}
