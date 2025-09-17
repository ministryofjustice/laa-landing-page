package uk.gov.justice.laa.portal.landingpage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionCleanupTask {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 3600000) // every hour
    public void cleanExpiredSessions() {
        jdbcTemplate.update("DELETE FROM SPRING_SESSION WHERE EXPIRY_TIME < ?", System.currentTimeMillis());
    }
}

