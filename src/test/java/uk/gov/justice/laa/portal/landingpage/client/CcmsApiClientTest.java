package uk.gov.justice.laa.portal.landingpage.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.laa.portal.landingpage.model.CcmsMessage;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CcmsApiClientTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CcmsApiClient ccmsApiClient;

    private CcmsMessage testMessage;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(ccmsApiClient, "ccmsApiBaseUrl", "https://test-api.example.com");
        ReflectionTestUtils.setField(ccmsApiClient, "ccmsApiKey", "test-api-key");

        ReflectionTestUtils.setField(ccmsApiClient, "restTemplate", restTemplate);

        testMessage = CcmsMessage.builder()
                .userName("testuser123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .vendorNumber("VENDOR001")
                .timestamp(LocalDateTime.now())
                .build();

        lenient().when(objectMapper.writeValueAsString(any(CcmsMessage.class))).thenReturn("{\"userName\":\"testuser123\"}");
    }

    @Test
    void shouldSendUserRoleChange_successfully() throws Exception {
        ResponseEntity<String> successResponse = new ResponseEntity<>("Success", HttpStatus.CREATED);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(successResponse);

        assertDoesNotThrow(() -> ccmsApiClient.sendUserRoleChange(testMessage));

        verify(restTemplate).exchange(
                eq("https://test-api.example.com/api/v1/user"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void shouldThrowCcmsApiException_whenRestClientExceptionOccurs() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("API connection failed"));

        CcmsApiException exception = assertThrows(CcmsApiException.class, 
                () -> ccmsApiClient.sendUserRoleChange(testMessage));
        
        assertEquals("Failed to communicate with CCMS API", exception.getMessage());
        assertTrue(exception.getCause() instanceof RestClientException);
    }

    @Test
    void shouldThrowCcmsApiException_whenJsonProcessingFails() throws Exception {
        when(objectMapper.writeValueAsString(any(CcmsMessage.class)))
                .thenThrow(new RuntimeException("JSON serialization failed"));

        CcmsApiException exception = assertThrows(CcmsApiException.class, 
                () -> ccmsApiClient.sendUserRoleChange(testMessage));
        
        assertEquals("Failed to send role change notification", exception.getMessage());
    }

    @Test
    void shouldSerializeMessageCorrectly() throws Exception {
        ResponseEntity<String> successResponse = new ResponseEntity<>("Success", HttpStatus.CREATED);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(successResponse);

        ccmsApiClient.sendUserRoleChange(testMessage);

        verify(objectMapper).writeValueAsString(testMessage);
    }
}
