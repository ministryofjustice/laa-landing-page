package uk.gov.justice.laa.portal.landingpage.playwright.reporting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FailureHtmlReportWriter {

    private static final String TEMPLATE_NAME =
            "playwright-error-template.html";

    private FailureHtmlReportWriter() {
    }

    public static synchronized void writeReport(List<FailedTest> failures) {
        if (failures.isEmpty()) {
            return;
        }

        try {
            Path reportPath = Path.of(
                    System.getProperty("user.dir"),
                    "build",
                    "playwright-report.html"
            );

            Files.createDirectories(reportPath.getParent());
            Files.writeString(reportPath, buildHtml(failures));

            System.err.println(
                    ">>> PLAYWRIGHT REPORT: file://" + reportPath.toAbsolutePath()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String buildHtml(List<FailedTest> failures) throws Exception {
        String htmlTemplate = Files.readString(
                Path.of(
                        FailureHtmlReportWriter.class
                                .getClassLoader()
                                .getResource(TEMPLATE_NAME)
                                .toURI()
                )
        );

        StringBuilder failuresHtml = new StringBuilder();
        for (FailedTest failure : failures) {
            failuresHtml.append(failure.toHtmlString());
        }

        return htmlTemplate.replace(
                "{{FAILURES}}",
                failuresHtml.toString()
        );
    }
}
