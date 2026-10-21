package uk.gov.justice.laa.portal.landingpage.playwright.reporting;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import java.util.Properties;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates a portable release-evidence HTML report from Gradle's JUnit XML.
 * Requires no additional dependencies; run it AFTER the Gradle test task.
 *
 * args[0]: directory of Gradle JUnit XML results
 * args[1]: target HTML file
 * args[2]: optional failure-evidence directory written by FailureHtmlReportWriter
 */
public final class PlaywrightReleaseReport {
    private static final long MAX_SCREENSHOT_BYTES = 8L * 1024 * 1024;
    private static final String TEMPLATE_NAME = "playwright-release-template.html";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm z", Locale.UK);

    private PlaywrightReleaseReport() { }

    private enum Status { PASSED, FAILED, SKIPPED }

    private record TestResult(String className, String name, double seconds,
                              Status status, String message, String trace) { }

    private record Evidence(String className, String displayName, Path screenshot) { }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: PlaywrightReleaseReport <junit-xml-dir> <output-html> [failure-evidence-dir]");
            System.exit(2);
        }
        Path xmlDir = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Path evidence = args.length > 2 ? Path.of(args[2]) : null;
        try {
            write(xmlDir, output, evidence);
            System.out.println(">>> RELEASE REPORT: " + output.toAbsolutePath().toUri());
        } catch (Exception e) {
            System.err.println("Unable to generate Playwright release report: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void write(Path xmlDir, Path output, Path evidenceFile) throws Exception {
        List<String> warnings = new ArrayList<>();
        List<TestResult> results = readTests(xmlDir, warnings);
        List<Evidence> evidence = readEvidence(evidenceFile, warnings);
        boolean taskFailed = Boolean.parseBoolean(System.getProperty("report.testTaskFailed", "false"));
        boolean taskSkipped = Boolean.parseBoolean(System.getProperty("report.testTaskSkipped", "false"));

        if (taskFailed && results.stream().noneMatch(t -> t.status == Status.FAILED)) {
            warnings.add("Gradle reported a failed test task, but no failed test case was found. The execution may be incomplete.");
        }
        if (taskSkipped) {
            warnings.add("Gradle did not execute the test task in this invocation. These results may be from an earlier run.");
        }
        if (results.isEmpty()) {
            warnings.add("No test cases were found. This is not evidence of a successful regression execution.");
        }

        long passed = results.stream().filter(t -> t.status == Status.PASSED).count();
        long failed = results.stream().filter(t -> t.status == Status.FAILED).count();
        long skipped = results.stream().filter(t -> t.status == Status.SKIPPED).count();
        long executed = passed + failed;
        double passRate = executed == 0 ? 0 : 100.0 * passed / executed;
        double seconds = results.stream().mapToDouble(TestResult::seconds).sum();

        // A Gradle failure, XML parsing warning, skipped test task or zero tests must never be shown as green.
        boolean incomplete = !warnings.isEmpty();
        String overall = results.isEmpty() || incomplete ? "INCOMPLETE" : failed > 0 ? "FAILED" : "PASSED";
        String overallColour = overall.equals("PASSED") ? "passed" : overall.equals("FAILED") ? "failed" : "incomplete";

        Map<String, List<TestResult>> groups = results.stream().collect(Collectors.groupingBy(
                TestResult::className, LinkedHashMap::new, Collectors.toList()));

        String template;
        try (InputStream stream = PlaywrightReleaseReport.class.getClassLoader()
                .getResourceAsStream(TEMPLATE_NAME)) {
            if (stream == null) {
                throw new IllegalStateException("Missing src/test/resources/" + TEMPLATE_NAME);
            }
            template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        String generated = DATE_FORMAT.format(ZonedDateTime.now());
        String html = template
                .replace("{{OVERALL}}", h(overall))
                .replace("{{OVERALL_CLASS}}", overallColour)
                .replace("{{RUN_DATE}}", h(generated))
                .replace("{{METADATA}}", buildMetadata())
                .replace("{{WARNINGS}}", buildWarnings(warnings))
                .replace("{{TOTAL}}", Integer.toString(results.size()))
                .replace("{{PASSED}}", Long.toString(passed))
                .replace("{{FAILED}}", Long.toString(failed))
                .replace("{{SKIPPED}}", Long.toString(skipped))
                .replace("{{RATE}}", String.format(Locale.UK, "%.1f%%", passRate))
                .replace("{{DURATION}}", duration(seconds))
                .replace("{{PASS_WIDTH}}", pct(results.size() == 0 ? 0 : 100.0 * passed / results.size()))
                .replace("{{FAIL_WIDTH}}", pct(results.size() == 0 ? 0 : 100.0 * failed / results.size()))
                .replace("{{SKIP_WIDTH}}", pct(results.size() == 0 ? 0 : 100.0 * skipped / results.size()))
                .replace("{{SUITES}}", buildSuites(groups, evidence));

        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, html, StandardCharsets.UTF_8);
    }

    private static List<TestResult> readTests(Path directory, List<String> warnings) throws Exception {
        List<TestResult> results = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            warnings.add("JUnit XML directory does not exist: " + directory.getFileName());
            return results;
        }

        List<Path> xmlFiles;
        try (Stream<Path> stream = Files.walk(directory)) {
            xmlFiles = stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
        if (xmlFiles.isEmpty()) {
            warnings.add("No JUnit XML files were found.");
        }
        for (Path file : xmlFiles) {
            try {
                Document doc = secureDocumentFactory().newDocumentBuilder().parse(file.toFile());
                NodeList cases = doc.getElementsByTagName("testcase");
                Element suite = doc.getDocumentElement();
                int expected = toNonnegativeInt(suite.getAttribute("tests"));
                if (expected > cases.getLength()) {
                    warnings.add("Test results appear incomplete in " + file.getFileName() + ".");
                }
                for (int i = 0; i < cases.getLength(); i++) {
                    Element test = (Element) cases.item(i);
                    String className = test.getAttribute("classname");
                    if (className.isBlank()) className = suite.getAttribute("name");
                    if (className.isBlank()) className = "Unknown test class";
                    String name = test.getAttribute("name");
                    if (name.isBlank()) name = "Unnamed test";
                    double seconds = parseSeconds(test.getAttribute("time"));
                    Element failure = directChild(test, "failure");
                    if (failure == null) failure = directChild(test, "error");
                    Element skip = directChild(test, "skipped");
                    Status state = failure != null ? Status.FAILED : skip != null ? Status.SKIPPED : Status.PASSED;
                    Element details = failure != null ? failure : skip;
                    String message = details == null ? "" : details.getAttribute("message");
                    String trace = details == null ? "" : Objects.toString(details.getTextContent(), "");
                    if (message.isBlank() && !trace.isBlank()) message = trace.strip().lines().findFirst().orElse("");
                    results.add(new TestResult(className, name, seconds, state, message, trace));
                }
            } catch (Exception ex) {
                warnings.add("Could not read " + file.getFileName() + ": " + ex.getClass().getSimpleName());
            }
        }
        return results;
    }

    private static List<Evidence> readEvidence(Path directory, List<String> warnings) {
        List<Evidence> items = new ArrayList<>();
        if (directory == null || !Files.isDirectory(directory)) return items;
        try (Stream<Path> stream = Files.walk(directory)) {
            for (Path file : stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".properties"))
                    .sorted().toList()) {
                try (InputStream input = Files.newInputStream(file)) {
                    Properties values = new Properties();
                    values.load(input);
                    String screenshot = values.getProperty("screenshot", "");
                    if (!screenshot.isBlank()) {
                        items.add(new Evidence(values.getProperty("className", ""),
                                values.getProperty("displayName", ""), Path.of(screenshot)));
                    }
                } catch (Exception ex) {
                    warnings.add("Could not read an individual screenshot evidence record.");
                }
            }
        } catch (Exception ex) {
            warnings.add("Screenshot evidence directory could not be inspected.");
        }
        return items;
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
            if (node instanceof Element element && element.getTagName().equals(name)) return element;
        }
        return null;
    }

    private static String buildMetadata() {
        String release = configuration("release.id", "RELEASE_ID");
        String environment = configuration("test.environment", "TEST_ENV");
        String browser = configuration("test.browser", "PLAYWRIGHT_BROWSER");
        String commit = env("GITHUB_SHA");
        if (commit.length() > 12) commit = commit.substring(0, 12);
        String workflow = env("GITHUB_WORKFLOW");
        String runId = env("GITHUB_RUN_ID");
        if (!workflow.isBlank() && !runId.isBlank()) workflow += " · #" + runId;
        return metadata("Release", release) + metadata("Environment", environment)
                + metadata("Browser", browser) + metadata("Commit", commit)
                + metadata("CI workflow", workflow);
    }

    private static String metadata(String label, String value) {
        return "<div class=\"meta\"><span>" + h(label) + "</span><strong>"
                + h(value.isBlank() ? "Not supplied" : value) + "</strong></div>";
    }

    private static String buildWarnings(List<String> warnings) {
        if (warnings.isEmpty()) return "";
        return "<section class=\"warning\"><strong>Execution evidence needs attention</strong><ul>"
                + warnings.stream().map(s -> "<li>" + h(s) + "</li>").collect(Collectors.joining())
                + "</ul></section>";
    }

    private static String buildSuites(Map<String, List<TestResult>> groups, List<Evidence> evidence) {
        if (groups.isEmpty()) return "<p class=\"empty\">No test results available.</p>";
        StringBuilder html = new StringBuilder();
        for (Map.Entry<String, List<TestResult>> group : groups.entrySet()) {
            List<TestResult> tests = group.getValue();
            long passing = tests.stream().filter(t -> t.status == Status.PASSED).count();
            long failing = tests.stream().filter(t -> t.status == Status.FAILED).count();
            long skipped = tests.stream().filter(t -> t.status == Status.SKIPPED).count();
            String title = group.getKey();
            String simpleTitle = title.substring(title.lastIndexOf('.') + 1);
            html.append("<section class=\"suite\" data-suite>")
                    .append("<header class=\"suite-heading\"><div><h3 title=\"").append(a(title))
                    .append("\">").append(h(simpleTitle)).append("</h3><small>").append(h(title))
                    .append("</small></div><div class=\"suite-counts\"><span class=\"mini-pass\">")
                    .append(passing).append(" passed</span><span class=\"mini-fail\">")
                    .append(failing).append(" failed</span><span class=\"mini-skip\">")
                    .append(skipped).append(" skipped</span></div></header>");
            for (TestResult test : tests) {
                html.append(buildTestRow(test, evidence));
            }
            html.append("</section>");
        }
        return html.toString();
    }

    private static String buildTestRow(TestResult test, List<Evidence> evidence) {
        String status = test.status.name().toLowerCase(Locale.ROOT);
        String search = test.className + " " + test.name + " " + test.message;
        StringBuilder html = new StringBuilder();
        html.append("<details class=\"test-row ").append(status)
                .append("\" data-status=\"").append(status).append("\" data-search=\"")
                .append(a(search.toLowerCase(Locale.ROOT)))
                .append("\"><summary><span class=\"status-dot ").append(status)
                .append("\"></span><span class=\"test-name\">").append(h(test.name))
                .append("</span><span class=\"test-status\">").append(h(status))
                .append("</span><span class=\"test-time\">").append(duration(test.seconds))
                .append("</span><span class=\"expand-arrow\">⌄</span></summary>");

        if (test.status == Status.FAILED) {
            html.append("<div class=\"test-evidence\">");
            if (!test.message.isBlank()) {
                html.append("<div class=\"failure-message\"><strong>Error</strong><p>")
                        .append(h(test.message)).append("</p></div>");
            }
            if (!test.trace.isBlank()) {
                html.append("<details class=\"stack-details\"><summary>Stack trace</summary><pre>")
                        .append(h(test.trace)).append("</pre></details>");
            }
            Path screenshot = findScreenshot(test, evidence);
            if (screenshot != null) {
                html.append(buildScreenshot(screenshot));
            } else {
                html.append("<p class=\"no-evidence\">No screenshot was registered for this failure.</p>");
            }
            html.append("</div>");
        } else if (test.status == Status.SKIPPED) {
            html.append("<div class=\"test-evidence\"><strong>Skipped</strong><p>")
                    .append(h(test.message.isBlank() ? "No reason provided by JUnit." : test.message))
                    .append("</p></div>");
        } else {
            html.append("<div class=\"test-evidence\"><p>Test completed successfully.</p></div>");
        }
        return html.append("</details>").toString();
    }

    private static Path findScreenshot(TestResult test, List<Evidence> evidence) {
        // Prefer an exact full-class/display-name match; allow simple class name fallback.
        for (Evidence item : evidence) {
            boolean sameClass = item.className.equals(test.className)
                    || test.className.endsWith("." + item.className)
                    || item.className.endsWith("." + test.className);
            if (sameClass && item.displayName.equals(test.name)) return item.screenshot;
        }
        return null;
    }

    private static String buildScreenshot(Path path) {
        try {
            if (!Files.isRegularFile(path)) return missingScreenshot("Screenshot file was not found.");
            if (Files.size(path) > MAX_SCREENSHOT_BYTES) {
                return missingScreenshot("Screenshot is over the 8 MB embedded-evidence limit.");
            }
            byte[] bytes = Files.readAllBytes(path);
            String mime = imageMime(bytes);
            if (mime == null) return missingScreenshot("Screenshot is not a supported PNG, JPEG or WebP image.");
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
                && data[3] == 71 && data[4] == 13 && data[5] == 10 && data[6] == 26 && data[7] == 10)
            return "image/png";
        if (data.length >= 3 && (data[0] & 255) == 255 && (data[1] & 255) == 216 && (data[2] & 255) == 255)
            return "image/jpeg";
        if (data.length >= 12 && new String(data, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(data, 8, 4, StandardCharsets.US_ASCII).equals("WEBP"))
            return "image/webp";
        return null;
    }

    private static String missingScreenshot(String message) { return "<p class=\"no-evidence\">" + h(message) + "</p>"; }
    private static String pct(double value) { return String.format(Locale.ROOT, "%.3f%%", value); }
    private static String duration(double sec) {
        if (sec >= 60) return "%dm %02ds".formatted((int) (sec / 60), (int) Math.round(sec % 60));
        return String.format(Locale.UK, "%.1fs", sec);
    }
    private static int toNonnegativeInt(String s) { try { return Math.max(0, Integer.parseInt(s)); } catch (Exception e) { return 0; } }
    private static double parseSeconds(String s) { try { return Math.max(0, Double.parseDouble(s)); } catch (Exception e) { return 0; } }
    private static String configuration(String property, String environment) {
        String value = System.getProperty(property, "");
        return value.isBlank() ? env(environment) : value;
    }
    private static String env(String key) { return Objects.toString(System.getenv(key), "").strip(); }
    private static String h(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
    private static String a(String value) { return h(value).replace("\r", " ").replace("\n", " "); }
}
