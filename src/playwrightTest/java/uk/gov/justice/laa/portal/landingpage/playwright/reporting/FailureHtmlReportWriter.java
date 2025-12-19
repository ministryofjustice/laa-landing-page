package uk.gov.justice.laa.portal.landingpage.playwright.reporting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FailureHtmlReportWriter {

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

    private static String buildHtml(List<FailedTest> failures) {
        StringBuilder html = new StringBuilder();

        html.append("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>Playwright Test Failures</title>
                        <style>
                            body {
                                font-family: Arial, sans-serif;
                                padding: 20px;
                            }
                            h1 {
                                color: #b00020;
                            }
                            .test {
                                margin-bottom: 50px;
                                padding-bottom: 20px;
                                border-bottom: 1px solid #ddd;
                            }
                            .test h2 {
                                margin-bottom: 10px;
                                color: #333;
                            }
                            .error {
                                background: #f5f5f5;
                                padding: 10px;
                                border-left: 4px solid #b00020;
                                white-space: pre-wrap;
                                margin-bottom: 10px;
                            }
                            details {
                                margin-bottom: 10px;
                            }
                            summary {
                                cursor: pointer;
                                font-weight: bold;
                            }
                            pre {
                                background: #f5f5f5;
                                padding: 10px;
                                overflow-x: auto;
                                border-left: 4px solid #999;
                            }
                            img {
                                max-width: 100%;
                                border: 1px solid #ccc;
                                margin-top: 10px;
                            }
                        </style>
                    </head>
                    <body>
                        <h1>❌ Playwright Test Failures</h1>
                """);

        for (FailedTest failure : failures) {
            html.append("""
                            <div class="test">
                                <h2>""")
                    .append(escape(failure.testClass()))
                    .append(" – ")
                    .append(escape(failure.displayName()))
                    .append("""
                            </h2>
                            
                            <div class="error">
                            """)
                    .append(escape(failure.errorMessage()))
                    .append("""
                                    </div>
                            """);

            // Stack trace
            if (failure.stackTrace() != null && !failure.stackTrace().isBlank()) {
                html.append("""
                                <details>
                                    <summary>Stack trace</summary>
                                    <pre>""")
                        .append(escape(failure.stackTrace()))
                        .append("""
                                        </pre>
                                    </details>
                                """);
            }

            // Screenshot
            if (failure.screenshotPath() != null) {
                html.append("""
                                <img src=\"""")
                        .append(failure.screenshotPath().toUri())
                        .append("""
                                    \" />
                                """);
            }

            html.append("""
                        </div>
                    """);
        }

        html.append("""
                    </body>
                    </html>
                """);

        return html.toString();
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}