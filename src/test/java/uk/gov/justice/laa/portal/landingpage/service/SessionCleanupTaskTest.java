package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SessionCleanupTaskTest {

    @InjectMocks
    private SessionCleanupTask sessionCleanupTask;

    @Mock
    JdbcTemplate jdbcTemplate;

    @Test
    void test_cleanExpiredSessions() {
        sessionCleanupTask = new SessionCleanupTask(jdbcTemplate);
        sessionCleanupTask.cleanExpiredSessions();
        verify(jdbcTemplate, times(1)).update(anyString(), any(Object.class));
        verify(jdbcTemplate, times(1)).update(anyString());
    }
}
