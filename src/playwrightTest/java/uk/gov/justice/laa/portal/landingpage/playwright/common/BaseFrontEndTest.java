package uk.gov.justice.laa.portal.landingpage.playwright.common;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Base class with login and logout for each test class
 */
public abstract class BaseFrontEndTest {
    private static Playwright playwright;
    private static Browser browser;
    public static Page page;
    public static Properties config;

    /**
     * Login to landing page
     */
    @BeforeAll
    static void setup() throws IOException {
        config = new Properties();
        try(InputStream stream = BaseFrontEndTest.class.getResourceAsStream("/playwright.properties")) {
            config.load(stream);
        }

        playwright = Playwright.create();
        boolean headless = Boolean.parseBoolean(config.getProperty("app.playwright.headless", "true"));

        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));

        page = browser.newPage();
        page.navigate(config.getProperty("laa.landing.page.url"));
        page.locator("[id='i0116']").fill(config.getProperty("laa.landing.page.user"));
        page.getByText("Next").click();
        page.locator("[id='i0118']").fill(config.getProperty("laa.landing.page.password"));
        page.waitForTimeout(1000);
        page.getByText("Sign in").click();
        page.getByText("Yes").click();
    }

    /**
     * Logoff Landing Page
     */
    @AfterAll
    static void tearDown() {
        page.close();
        browser.close();
        playwright.close();
    }
}
