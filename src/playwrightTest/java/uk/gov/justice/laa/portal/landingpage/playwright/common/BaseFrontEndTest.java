package uk.gov.justice.laa.portal.landingpage.playwright.common;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.portal.landingpage.TestApplication;

import java.nio.file.Paths;

/**
 * base integration test
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableConfigurationProperties()
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseFrontEndTest {

    @Value("${laa.landing.page.url}")
    private String laaLandingPageUrl;

    @Value("${laa.landing.page.user}")
    private String laaLandingPageUser;

    @Value("${laa.landing.page.password}")
    private String laaLandingPagePassword;

   /**
     * Login to landing page
     */
    @BeforeAll
    void setup() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            Page page = browser.newPage();
            page.navigate(laaLandingPageUrl);
            page.locator("[id='i0116']").fill(laaLandingPageUser);
            page.waitForTimeout(3000);
            page.getByText("Next").click();
            page.waitForTimeout(2000);
            page.locator("[id='i0118']").fill(laaLandingPagePassword);
            page.waitForTimeout(2000);
            page.getByText("Sign in").click();
            page.waitForTimeout(2000);
            page.getByText("Yes").click();
            page.waitForTimeout(5000);
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("example.png")));
        }
    }

    /**
     * Logoff Landing Page
     */
    @AfterAll
    void tearDownAll() {}
}
