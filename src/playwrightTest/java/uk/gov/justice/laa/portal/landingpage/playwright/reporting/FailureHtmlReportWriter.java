
package uk.gov.justice.laa.portal.landingpage.playwright.reporting;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;

/**
 * Keeps the existing failure-only report working. In addition, records the
 * screenshots for the release report, without changing any test class.
 */
public final class FailureHtmlReportWriter {

    private static final String TEMPLATE_NAME = "playwright-error-template.html";

    private FailureHtmlReportWriter() {
    }

    public static synchronized void writeReport(List<FailedTest> failures) {
        if (failures.isEmpty()) {
            return;
        }

        // An evidence file per failing test also works with parallel Gradle test workers.
        // The Gradle configuration supplied with this package clears the directory at the
        // start of the run so old failures cannot leak into a later execution.
        for (FailedTest failure : failures) {
            if (failure.screenshotPath() != null) {
                try {
                    writeEvidence(failure);
                } catch (Exception e) {
                    System.err.println("Could not record failure screenshot: " + e.getMessage());
                }
            }
        }

        // Legacy output is retained for compatibility with existing usage.
        try {
            Path reportPath = Path.of(System.getProperty("user.dir"), "build", "playwright-report.html");
            Files.createDirectories(reportPath.getParent());
            Files.writeString(reportPath, buildHtml(failures));
            System.err.println(">>> PLAYWRIGHT FAILURE REPORT: " + reportPath.toAbsolutePath().toUri());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeEvidence(FailedTest failure) throws Exception {
        Path evidenceDir = Path.of(System.getProperty("user.dir"), "build", "playwright-failure-evidence");
        Files.createDirectories(evidenceDir);

        Properties values = new Properties();
        values.setProperty("className", failure.testClass() == null ? "" : failure.testClass());
        values.setProperty("displayName", failure.displayName() == null ? "" : failure.displayName());
        values.setProperty("screenshot", failure.screenshotPath().toAbsolutePath().normalize().toString());

        String key = values.getProperty("className") + "\u0000" + values.getProperty("displayName");

        String name = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8))) + ".properties";

        Path destination = evidenceDir.resolve(name);
        Path temp = Files.createTempFile(evidenceDir, "evidence-", ".tmp");

        try {
            try (OutputStream output = Files.newOutputStream(temp)) {
                values.store(output, "Playwright failure screenshot evidence");
            }

            try {
                Files.move(temp, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temp, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static String buildHtml(List<FailedTest> failures) throws Exception {
        try (var input = FailureHtmlReportWriter.class.getClassLoader().getResourceAsStream(TEMPLATE_NAME)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + TEMPLATE_NAME);
            }

            String htmlTemplate = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            StringBuilder failuresHtml = new StringBuilder();

            for (FailedTest failure : failures) {
                failuresHtml.append(failure.toHtmlString());
            }

            return htmlTemplate.replace("{{FAILURES}}", failuresHtml.toString());
        }
    }
}
