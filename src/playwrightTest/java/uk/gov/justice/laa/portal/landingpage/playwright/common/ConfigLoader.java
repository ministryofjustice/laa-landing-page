package uk.gov.justice.laa.portal.landingpage.playwright.common;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

class ConfigLoader {
    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());

    static Properties loadConfig(String configFile) throws IOException {
        Properties config = new Properties();
        try (var stream = ConfigLoader.class.getResourceAsStream(configFile)) {
            if (stream == null) {
                throw new IOException("Could not find " + configFile + " on classpath");
            }
            config.load(stream);
            LOGGER.info("Configuration loaded successfully");
            return config;
        }
    }
}
