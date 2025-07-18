package uk.gov.justice.laa.portal.landingpage.polling;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.justice.laa.portal.landingpage.service.InternalUserPollingService;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class InternalUserPollingTest {

    @Mock
    private InternalUserPollingService internalUserPollingService;

    @InjectMocks
    private InternalUserPolling internalUserPolling;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCallPollForNewUsers_whenPollingEnabledIsTrue() {
        InternalUserPolling polling = new InternalUserPolling(internalUserPollingService);
        setPollingEnabled(polling, true);
        polling.poll();
        verify(internalUserPollingService, times(1)).pollForNewUsers();
    }

    @Test
    void shouldNotCallPollForNewUsers_whenPollingEnabledIsFalse() {
        InternalUserPolling polling = new InternalUserPolling(internalUserPollingService);
        setPollingEnabled(polling, false);
        polling.poll();
        verify(internalUserPollingService, never()).pollForNewUsers();
    }

    // Helper method to set private field
    private void setPollingEnabled(InternalUserPolling polling, boolean value) {
        try {
            Field field = InternalUserPolling.class.getDeclaredField("pollingEnabled");
            field.setAccessible(true);
            field.set(polling, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
