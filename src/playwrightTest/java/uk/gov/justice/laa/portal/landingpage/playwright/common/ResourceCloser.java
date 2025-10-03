package uk.gov.justice.laa.portal.landingpage.playwright.common;

import java.util.logging.Level;
import java.util.logging.Logger;

class ResourceCloser {
    private static final Logger LOGGER = Logger.getLogger(ResourceCloser.class.getName());

    static void closeAll(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                    LOGGER.info("Closed " + resource.getClass().getSimpleName());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to close resource", e);
                }
            }
        }
    }
}
