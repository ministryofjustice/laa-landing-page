package uk.gov.justice.laa.portal.landingpage.playwright.reporting;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Generates a standalone HTML report from the Playwright Gradle task's JUnit XML.
 * The file locations are fixed beneath the project's build directory.
 */
public final class PlaywrightReleaseReport {
    private static final String TEMPLATE_NAME = "playwright-release-template.html";
    private static final long MAX_SCREENSHOT_BYTES = 8L * 1024 * 1024;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm z", Locale.UK);

    private PlaywrightReleaseReport() {
    }

    private enum Status {
        PASSED, FAILED, SKIPPED
    }

    private record TestResult(String className, String name, double seconds,
                              Status status, String message, String trace) {
    }

    private record Evidence(String className, String displayName, Path screenshot) {
    }

    /**
     * Generates the report using the fixed, Gradle-managed build locations.
     * Legacy arguments are deliberately ignored: they cannot select filesystem paths.
     *
     * @param args unused legacy Gradle arguments
     */
    public static void main(String[] args) {
        Path projectRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path buildRoot = projectRoot.resolve("build");
        Path xmlDirectory = buildRoot.resolve("test-results/playwrightTest");
        Path evidenceDirectory = buildRoot.resolve("playwright-failure-evidence");
        Path reportFile = buildRoot.resolve("reports/playwright/playwright-report.html");

        try {
            writeReport(buildRoot, xmlDirectory, evidenceDirectory, reportFile);
            System.out.println(">>> RELEASE REPORT: " + reportFile.toUri());
        } catch (Exception ex) {
            System.err.println("Unable to generate Playwright release report: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void writeReport(Path buildRoot, Path xmlDirectory,
                                    Path evidenceDirectory, Path reportFile) throws Exception {
        List<String> warnings = new ArrayList<>();
        List<TestResult> results = readTests(buildRoot, xmlDirectory, warnings);
        final List<Evidence> evidence = readEvidence(buildRoot, evidenceDirectory, warnings);

        boolean taskFailed = Boolean.parseBoolean(System.getProperty("report.testTaskFailed", "false"));
        boolean taskSkipped = Boolean.parseBoolean(System.getProperty("report.testTaskSkipped", "false"));

        if (taskFailed && results.stream().noneMatch(test -> test.status() == Status.FAILED)) {
            warnings.add("Gradle failed, but no failed test case was recorded. Execution may be incomplete.");
        }
        if (taskSkipped) {
            warnings.add("Gradle skipped the Playwright test task. Results may be from an earlier run.");
        }
        if (results.isEmpty()) {
            warnings.add("No test cases were found. This is not evidence of successful regression.");
        }

        long passed = results.stream().filter(test -> test.status() == Status.PASSED).count();
        long failed = results.stream().filter(test -> test.status() == Status.FAILED).count();
        long skipped = results.stream().filter(test -> test.status() == Status.SKIPPED).count();
        long executed = passed + failed;
        double passRate = executed == 0 ? 0 : 100.0 * passed / executed;
        double seconds = results.stream().mapToDouble(TestResult::seconds).sum();

        String overall;
        if (!warnings.isEmpty()) {
            overall = "INCOMPLETE";
        } else if (failed > 0) {
            overall = "FAILED";
        } else {
            overall = "PASSED";
        }
        String overallColour = overall.toLowerCase(Locale.ROOT);

        Map<String, List<TestResult>> groups = results.stream().collect(Collectors.groupingBy(
                TestResult::className, LinkedHashMap::new, Collectors.toList()));

        String template = readTemplate();
        final String html = template
                .replace("{{OVERALL}}", escapeHtml(overall))
                .replace("{{OVERALL_CLASS}}", overallColour)
                .replace("{{RUN_DATE}}", escapeHtml(DATE_FORMAT.format(ZonedDateTime.now())))
                .replace("{{METADATA}}", buildMetadata())
                .replace("{{WARNINGS}}", buildWarnings(warnings))
                .replace("{{TOTAL}}", Integer.toString(results.size()))
                .replace("{{PASSED}}", Long.toString(passed))
                .replace("{{FAILED}}", Long.toString(failed))
                .replace("{{SKIPPED}}", Long.toString(skipped))
                .replace("{{RATE}}", String.format(Locale.UK, "%.1f%%", passRate))
                .replace("{{DURATION}}", duration(seconds))
                .replace("{{PASS_WIDTH}}", percentageWidth(passed, results.size()))
                .replace("{{FAIL_WIDTH}}", percentageWidth(failed, results.size()))
                .replace("{{SKIP_WIDTH}}", percentageWidth(skipped, results.size()))
                .replace("{{SUITES}}", buildSuites(groups, evidence));

        if (Files.isSymbolicLink(buildRoot)) {
            throw new IOException("The build directory must not be a symbolic link.");
        }
        Files.createDirectories(reportFile.getParent());
        Path canonicalBuild = buildRoot.toRealPath();
        Path canonicalParent = reportFile.getParent().toRealPath();
        if (!canonicalParent.startsWith(canonicalBuild) || Files.isSymbolicLink(reportFile)) {
            throw new IOException("The report destination is outside the build directory.");
        }
        Files.writeString(reportFile, html, StandardCharsets.UTF_8);
    }

    private static String readTemplate() throws IOException {
        try (InputStream input = PlaywrightReleaseReport.class.getClassLoader()
                .getResourceAsStream(TEMPLATE_NAME)) {
            if (input == null) {
                throw new IOException("Missing resource: " + TEMPLATE_NAME);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<TestResult> readTests(Path buildRoot, Path directory,
                                              List<String> warnings) throws Exception {
        List<TestResult> results = new ArrayList<>();
        if (!isSafeDirectory(buildRoot, directory)) {
            warnings.add("Playwright JUnit XML directory is missing or outside the build directory.");
            return results;
        }

        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "TEST-*.xml")) {
            for (Path file : stream) {
                if (isSafeFile(directory, file)) {
                    files.add(file);
                }
            }
        }
        files.sort(Comparator.comparing(Path::toString));
        if (files.isEmpty()) {
            warnings.add("No JUnit XML files were found.");
        }

        for (Path file : files) {
            try {
                Document document = secureDocumentFactory().newDocumentBuilder().parse(file.toFile());
                Element suite = document.getDocumentElement();
                NodeList cases = document.getElementsByTagName("testcase");
                int expected = parseNonnegativeInt(suite.getAttribute("tests"));
                if (expected > cases.getLength()) {
                    warnings.add("Test results appear incomplete in " + file.getFileName() + ".");
                }
                for (int index = 0; index < cases.getLength(); index++) {
                    Element test = (Element) cases.item(index);
                    results.add(readTestCase(suite, test));
                }
            } catch (Exception ex) {
                warnings.add("Could not read " + file.getFileName() + ": "
                        + ex.getClass().getSimpleName());
            }
        }
        return results;
    }

    private static TestResult readTestCase(Element suite, Element test) {
        String className = test.getAttribute("classname");
        if (className.isBlank()) {
            className = suite.getAttribute("name");
        }
        if (className.isBlank()) {
            className = "Unknown test class";
        }
        String name = test.getAttribute("name");
        if (name.isBlank()) {
            name = "Unnamed test";
        }

        Element failure = directChild(test, "failure");
        if (failure == null) {
            failure = directChild(test, "error");
        }
        Element skipped = directChild(test, "skipped");
        Status status = failure != null ? Status.FAILED : skipped != null ? Status.SKIPPED : Status.PASSED;
        Element details = failure != null ? failure : skipped;
        String message = details == null ? "" : details.getAttribute("message");
        String trace = details == null ? "" : Objects.toString(details.getTextContent(), "");
        if (message.isBlank() && !trace.isBlank()) {
            message = trace.strip().lines().findFirst().orElse("");
        }
        return new TestResult(className, name, parseSeconds(test.getAttribute("time")),
                status, message, trace);
    }

    private static List<Evidence> readEvidence(Path buildRoot, Path directory,
                                               List<String> warnings) throws IOException {
        List<Evidence> items = new ArrayList<>();
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            return items;
        }
        if (!isSafeDirectory(buildRoot, directory)) {
            warnings.add("Failure evidence directory is not a safe build directory.");
            return items;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.properties")) {
            for (Path file : stream) {
                if (!isSafeFile(directory, file)) {
                    warnings.add("An unsafe evidence file was ignored.");
                    continue;
                }
                try (InputStream input = Files.newInputStream(file)) {
                    Properties values = new Properties();
                    values.load(input);
                    String screenshotName = values.getProperty("screenshot", "");
                    if (screenshotName.matches("[a-f0-9]{64}\\.(png|jpeg|webp)")) {
                        Path screenshot = directory.resolve(screenshotName);
                        if (isSafeFile(directory, screenshot)) {
                            items.add(new Evidence(values.getProperty("className", ""),
                                    values.getProperty("displayName", ""), screenshot));
                        }
                    } else if (!screenshotName.isBlank()) {
                        warnings.add("An evidence record had an unsafe screenshot reference.");
                    }
                } catch (Exception ex) {
                    warnings.add("Could not read a failure evidence record.");
                }
            }
        }
        return items;
    }

    private static boolean isSafeDirectory(Path buildRoot, Path directory) throws IOException {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(buildRoot, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        return directory.toRealPath().startsWith(buildRoot.toRealPath());
    }

    private static boolean isSafeFile(Path directory, Path file) throws IOException {
        return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                && file.toRealPath().getParent().equals(directory.toRealPath());
    }

    private static DocumentBuilderFactory secureDocumentFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static Element directChild(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(name)) {
                return element;
            }
        }
        return null;
    }

    private static String buildMetadata() {
        String release = configuration("release.id", "RELEASE_ID");
        String environment = configuration("test.environment", "TEST_ENV");
        String browser = configuration("test.browser", "PLAYWRIGHT_BROWSER");
        String commit = environmentValue("GITHUB_SHA");
        if (commit.length() > 12) {
            commit = commit.substring(0, 12);
        }
        String workflow = environmentValue("GITHUB_WORKFLOW");
        String runId = environmentValue("GITHUB_RUN_ID");
        if (!workflow.isBlank() && !runId.isBlank()) {
            workflow += " · #" + runId;
        }
        return metadata("Release", release) + metadata("Environment", environment)
                + metadata("Browser", browser) + metadata("Commit", commit)
                + metadata("CI workflow", workflow);
    }

    private static String metadata(String label, String value) {
        return "<div class=\"meta\"><span>" + escapeHtml(label) + "</span><strong>"
                + escapeHtml(value.isBlank() ? "Not supplied" : value) + "</strong></div>";
    }

    private static String buildWarnings(List<String> warnings) {
        if (warnings.isEmpty()) {
            return "";
        }
        return "<section class=\"warning\"><strong>Execution evidence needs attention</strong><ul>"
                + warnings.stream().map(warning -> "<li>" + escapeHtml(warning) + "</li>")
                .collect(Collectors.joining()) + "</ul></section>";
    }

    private static String buildSuites(Map<String, List<TestResult>> groups, List<Evidence> evidence) {
        if (groups.isEmpty()) {
            return "<p class=\"empty\">No test results available.</p>";
        }
        StringBuilder html = new StringBuilder();
        for (Map.Entry<String, List<TestResult>> group : groups.entrySet()) {
            List<TestResult> tests = group.getValue();
            long passing = tests.stream().filter(test -> test.status() == Status.PASSED).count();
            long failing = tests.stream().filter(test -> test.status() == Status.FAILED).count();
            long skipping = tests.stream().filter(test -> test.status() == Status.SKIPPED).count();
            String title = group.getKey();
            String simpleTitle = title.substring(title.lastIndexOf('.') + 1);
            html.append("<section class=\"suite\" data-suite>")
                    .append("<header class=\"suite-heading\"><div><h3 title=\"").append(escapeAttribute(title))
                    .append("\">").append(escapeHtml(simpleTitle)).append("</h3><small>").append(escapeHtml(title))
                    .append("</small></div><div class=\"suite-counts\"><span class=\"mini-pass\">")
                    .append(passing).append(" passed</span><span class=\"mini-fail\">")
                    .append(failing).append(" failed</span><span class=\"mini-skip\">")
                    .append(skipping).append(" skipped</span></div></header>");
            for (TestResult test : tests) {
                html.append(buildTestRow(test, evidence));
            }
            html.append("</section>");
        }
        return html.toString();
    }

    private static String buildTestRow(TestResult test, List<Evidence> evidence) {
        String status = test.status().name().toLowerCase(Locale.ROOT);
        String search = test.className() + " " + test.name() + " " + test.message();
        StringBuilder html = new StringBuilder();
        html.append("<details class=\"test-row ").append(status)
                .append("\" data-status=\"").append(status).append("\" data-search=\"")
                .append(escapeAttribute(search.toLowerCase(Locale.ROOT)))
                .append("\"><summary><span class=\"status-dot ").append(status)
                .append("\"></span><span class=\"test-name\">").append(escapeHtml(test.name()))
                .append("</span><span class=\"test-status\">").append(status)
                .append("</span><span class=\"test-time\">").append(duration(test.seconds()))
                .append("</span><span class=\"expand-arrow\">⌄</span></summary>");

        if (test.status() == Status.FAILED) {
            html.append("<div class=\"test-evidence\">");
            if (!test.message().isBlank()) {
                html.append("<div class=\"failure-message\"><strong>Error</strong><p>")
                        .append(escapeHtml(test.message())).append("</p></div>");
            }
            if (!test.trace().isBlank()) {
                html.append("<details class=\"stack-details\"><summary>Stack trace</summary><pre>")
                        .append(escapeHtml(test.trace())).append("</pre></details>");
            }
            Path screenshot = findScreenshot(test, evidence);
            if (screenshot == null) {
                html.append("<p class=\"no-evidence\">No screenshot was registered for this failure.</p>");
            } else {
                html.append(buildScreenshot(screenshot));
            }
            html.append("</div>");
        } else if (test.status() == Status.SKIPPED) {
            html.append("<div class=\"test-evidence\"><strong>Skipped</strong><p>")
                    .append(escapeHtml(test.message().isBlank() ? "No reason provided by JUnit." : test.message()))
                    .append("</p></div>");
        } else {
            html.append("<div class=\"test-evidence\"><p>Test completed successfully.</p></div>");
        }
        return html.append("</details>").toString();
    }

    private static Path findScreenshot(TestResult test, List<Evidence> evidence) {
        for (Evidence item : evidence) {
            boolean sameClass = item.className().equals(test.className())
                    || test.className().endsWith("." + item.className())
                    || item.className().endsWith("." + test.className());
            if (sameClass && item.displayName().equals(test.name())) {
                return item.screenshot();
            }
        }
        return null;
    }

    private static String buildScreenshot(Path path) {
        try {
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                return missingScreenshot("Screenshot file was not found.");
            }
            if (Files.size(path) > MAX_SCREENSHOT_BYTES) {
                return missingScreenshot("Screenshot exceeds the 8 MB evidence limit.");
            }
            byte[] bytes = Files.readAllBytes(path);
            String mime = imageMime(bytes);
            if (mime == null) {
                return missingScreenshot("Screenshot is not a PNG, JPEG or WebP image.");
            }
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "<div class=\"screenshot\"><strong>Failure screenshot</strong>"
                    + "<img alt=\"Failure screenshot\" loading=\"lazy\" src=\"data:"
                    + mime + ";base64," + base64 + "\">"
                    + "<p>Right-click the screenshot to save a copy.</p></div>";
        } catch (Exception ex) {
            return missingScreenshot("Screenshot could not be embedded.");
        }
    }

    private static String imageMime(byte[] data) {
        if (data.length >= 8 && (data[0] & 255) == 137 && data[1] == 80 && data[2] == 78
                && data[3] == 71 && data[4] == 13 && data[5] == 10 && data[6] == 26 && data[7] == 10) {
            return "image/png";
        }
        if (data.length >= 3 && (data[0] & 255) == 255 && (data[1] & 255) == 216
                && (data[2] & 255) == 255) {
            return "image/jpeg";
        }
        if (data.length >= 12 && new String(data, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(data, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return "image/webp";
        }
        return null;
    }

    private static String missingScreenshot(String message) {
        return "<p class=\"no-evidence\">" + escapeHtml(message) + "</p>";
    }

    private static String percentageWidth(long count, int total) {
        return String.format(Locale.ROOT, "%.3f%%", total == 0 ? 0 : 100.0 * count / total);
    }

    private static String duration(double seconds) {
        if (seconds >= 60) {
            long roundedSeconds = Math.round(seconds);
            return "%dm %02ds".formatted(roundedSeconds / 60, roundedSeconds % 60);
        }
        return String.format(Locale.UK, "%.1fs", seconds);
    }

    private static int parseNonnegativeInt(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static double parseSeconds(String value) {
        try {
            return Math.max(0, Double.parseDouble(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String configuration(String property, String environment) {
        String value = System.getProperty(property, "");
        return value.isBlank() ? environmentValue(environment) : value;
    }

    private static String environmentValue(String key) {
        return Objects.toString(System.getenv(key), "").strip();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String escapeAttribute(String value) {
        return escapeHtml(value).replace("\r", " ").replace("\n", " ");
    }
}
