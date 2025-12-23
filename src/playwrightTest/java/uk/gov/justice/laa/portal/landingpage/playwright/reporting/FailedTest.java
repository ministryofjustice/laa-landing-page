package uk.gov.justice.laa.portal.landingpage.playwright.reporting;

import java.nio.file.Path;

public record FailedTest(
        String displayName,
        String testClass,
        String errorMessage,
        String stackTrace,
        Path screenshotPath
) {

    public String toHtmlString() {
        StringBuilder html = new StringBuilder();

        html.append("""
                <div class="test">
                    <h2>""")
                .append(escape(testClass))
                .append(" – ")
                .append(escape(displayName))
                .append("""
                    </h2>

                    <div class="error">
                """)
                .append(escape(errorMessage))
                .append("""
                    </div>
                """);

        html.append(buildStacktraceHtml(stackTrace));
        html.append(buildScreenshotHtml(screenshotPath));

        html.append("""
                </div>
            """);

        return html.toString();
    }


    private static String buildStacktraceHtml(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) {
            return "";
        }

        return """
            <details>
                <summary>Stack trace</summary>
                <pre>%s</pre>
            </details>
            """.formatted(escape(stackTrace));
    }

    private static String buildScreenshotHtml(Path screenshotPath) {
        if (screenshotPath == null) {
            return "";
        }

        return """
            <img src="%s" />
            """.formatted(screenshotPath.toUri());
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
