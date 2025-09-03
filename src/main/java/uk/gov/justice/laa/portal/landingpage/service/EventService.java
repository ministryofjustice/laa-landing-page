package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.AuditEvent;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private static final String EVENT_TEMPLATE = """
            %s: Audit event %s, by User with user id %s, %s
            """;

    public void logEvent(AuditEvent event) {
        String eventString = String.format(EVENT_TEMPLATE, event.getCreatedDate(), event.getEventType().toString(),
                event.getUserId().toString(), event.getDescription());
        logger.info(eventString);
    }
}
