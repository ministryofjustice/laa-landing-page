package uk.gov.justice.laa.portal.landingpage.playwright.common;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseFrontEndTest {
    private static final Logger LOGGER = Logger.getLogger(BaseFrontEndTest.class.getName());
    private static final String CONFIG_FILE = "src/playwrightTest/resources/playwright.properties";

    protected static Playwright playwright;
    protected static Browser browser;
    protected static Page page;
    protected static Properties config;

    @RegisterExtension
    TestWatcher screenshotOnFailure = new ScreenshotWatcher();

    @BeforeAll
    static void setup() throws IOException {
        loadConfig();
        initializeBrowser();
        performLogin();
    }

    @AfterAll
    static void tearDown() {
        ResourceCloser.closeAll(page, browser, playwright);
    }

    private static void loadConfig() throws IOException {
        config = new Properties();
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            config.load(input);
        }
    }

    private static void initializeBrowser() {
        LOGGER.info("Initializing browser...");
        playwright = Playwright.create();
        boolean headless = Boolean.parseBoolean(config.getProperty("app.playwright.headless", "false"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
        page = browser.newPage();
    }

    private static void performLogin() {
        LOGGER.info("Performing login...");
        try {
            // Use properties for URL, username, and password
            String url = config.getProperty("laa.landing.page.url");
            final String username = config.getProperty("laa.landing.page.user");
            final String password = config.getProperty("laa.landing.page.password");

            page.navigate(url);
            page.locator("[id='i0116']").fill(username);
            page.getByText("Next").click();
            page.locator("[id='i0118']").fill(password);
            page.waitForTimeout(1000);
            page.locator("input[type='submit'][value='Sign in']").click();

            // page.getByText("Yes").click();

            LOGGER.info("Login successful");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login failed", e);
            throw e;
        }
    }
}
