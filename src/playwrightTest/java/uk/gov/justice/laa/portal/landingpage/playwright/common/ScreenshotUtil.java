package uk.gov.justice.laa.portal.landingpage.playwright.common;

import com.microsoft.playwright.Page;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class ScreenshotUtil {
    static Path takeScreenshot(Page page, String testName) throws IOException {
        Path dir = Path.of("screenshots"); //Directory to be set at a later date
        Files.createDirectories(dir);

        String safeTestName = testName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"));
        Path path = dir.resolve(safeTestName + "_" + timestamp + ".png");

        page.screenshot(new Page.ScreenshotOptions()
                .setPath(path)
                .setFullPage(true));

        return path;
    }
}
