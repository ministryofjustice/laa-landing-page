package uk.gov.justice.laa.portal.landingpage.playwright.common;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import uk.gov.justice.laa.portal.landingpage.playwright.reporting.FailedTest;
import uk.gov.justice.laa.portal.landingpage.playwright.reporting.FailureHtmlReportWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScreenshotWatcher implements AfterTestExecutionCallback {

    private static final Logger LOGGER =
            Logger.getLogger(ScreenshotWatcher.class.getName());

    // Must be public – accessed by reporting package
    public static final List<FailedTest> FAILURES =
            new CopyOnWriteArrayList<>();

    private final Supplier<Page> pageSupplier;

    public ScreenshotWatcher(Supplier<Page> pageSupplier) {
        this.pageSupplier = pageSupplier;
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {

        var exception = context.getExecutionException();
        if (exception.isEmpty()) {
            return;
        }

        Page page = pageSupplier.get();
        if (page == null || page.isClosed()) {
            LOGGER.warning("Page was null or closed, cannot take screenshot");
            return;
        }

        try {
            Path screenshotPath = ScreenshotUtil.takeScreenshot(
                    page,
                    context.getDisplayName()
            );

            Throwable throwable = exception.get();

            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));

            FAILURES.add(new FailedTest(
                    context.getDisplayName(),
                    context.getRequiredTestClass().getSimpleName(),
                    exception.get().getMessage(),
                    sw.toString(),
                    screenshotPath
            ));

            FailureHtmlReportWriter.writeReport(FAILURES);

            LOGGER.log(
                    Level.INFO,
                    "Screenshot saved: {0}",
                    screenshotPath.toAbsolutePath()
            );


            FailureHtmlReportWriter.writeReport(FAILURES);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to capture screenshot", e);
        }
    }
}
