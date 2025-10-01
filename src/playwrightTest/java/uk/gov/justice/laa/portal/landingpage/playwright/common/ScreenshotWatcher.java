package uk.gov.justice.laa.portal.landingpage.playwright.common;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

class ScreenshotWatcher implements TestWatcher {
    private static final Logger LOGGER = Logger.getLogger(ScreenshotWatcher.class.getName());

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (BaseFrontEndTest.page == null) return;

        try {
            Path screenshotPath = ScreenshotUtil.takeScreenshot(
                    BaseFrontEndTest.page,
                    context.getDisplayName()
            );
            LOGGER.log(Level.INFO, "Screenshot saved: {0}", screenshotPath);
            LOGGER.log(Level.INFO, "URL at failure: {0}", BaseFrontEndTest.page.url());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to capture screenshot", e);
        }
    }
}
