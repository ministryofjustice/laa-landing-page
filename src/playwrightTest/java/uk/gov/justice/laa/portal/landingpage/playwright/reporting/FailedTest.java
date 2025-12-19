package uk.gov.justice.laa.portal.landingpage.playwright.reporting;

import java.nio.file.Path;

public record FailedTest(
        String displayName,
        String testClass,
        String errorMessage,
        String stackTrace,
        Path screenshotPath
) {}
