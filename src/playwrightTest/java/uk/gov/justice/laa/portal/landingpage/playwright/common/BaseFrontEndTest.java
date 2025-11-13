package uk.gov.justice.laa.portal.landingpage.playwright.common;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseFrontEndTest {
    private static final Logger LOGGER = Logger.getLogger(BaseFrontEndTest.class.getName());
    private static final String CONFIG_FILE = "src/playwrightTest/resources/playwright.properties";

    protected static Playwright playwright;
    protected static Browser browser;
    protected static Page page;
    protected static Properties config;

    @LocalServerPort
    protected int port;

    private static boolean setupDone = false;

    @Container
    @ServiceConnection
    public static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("postgres")
            .withPassword("password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @RegisterExtension
    TestWatcher screenshotOnFailure = new ScreenshotWatcher();

    @BeforeEach
    void setup() throws IOException {
        if (!setupDone) {
            loadConfig();
            initializeBrowser();
            performLogin();
            setupDone = true;
        }
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

    private void performLogin() {
        LOGGER.info("Performing login...");
        try {
            // Use properties for URL, username, and password
            String userEmail = config.getProperty("initial.user.email");
            String url = String.format("http://localhost:%d/playwright/login?email=%s", port, userEmail);
            page.navigate(url);
            LOGGER.info("Login successful");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login failed", e);
            throw e;
        }
    }
}
