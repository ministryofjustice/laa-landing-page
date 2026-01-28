package uk.gov.justice.laa.portal.landingpage.playwright.common;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.ManageUsersPage;

import java.util.logging.Level;
import java.util.logging.Logger;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseFrontEndTest {
    private static final Logger LOGGER = Logger.getLogger(BaseFrontEndTest.class.getName());

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    @LocalServerPort
    protected int port;

    @Value("${app.playwright.headless}")
    private boolean headless;

    private static boolean setupComplete = false;

    protected static final PostgreSQLContainer<?> postgresContainer =
            SharedPostgresContainer.getInstance();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @RegisterExtension
    ScreenshotWatcher screenshotOnFailure =
            new ScreenshotWatcher(() -> page);


    @BeforeAll
    void beforeAll() {
        if (!setupComplete) {
            playwright = Playwright.create();

            browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions().setHeadless(headless));

            setupComplete = true;
        }
    }

    @BeforeEach
    void beforeEach() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void afterEach() {
        if (context != null) {
            context.close(); // closes page too
        }
    }

    /**
     * Navigate to the login page as the specified test user.
     *
     * @param userEmail the email of the test user
     */
    protected void loginAs(String userEmail) {
        LOGGER.info("Logging in as " + userEmail);
        try {
            String url = String.format("http://localhost:%d/playwright/login?email=%s", port, userEmail);
            page.navigate(url);
            LOGGER.info("Login complete for " + userEmail);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login failed for " + userEmail, e);
            throw e;
        }
    }

    protected ManageUsersPage loginAndGetManageUsersPage(TestUser user) {
        loginAs(user.email);
        page.navigate(String.format("http://localhost:%d/admin/users", port));
        return new ManageUsersPage(page, port);
    }

    protected ManageUsersPage loginAndGetManageUsersPage(String userEmail) {
        loginAs(userEmail);
        page.navigate(String.format("http://localhost:%d/admin/users", port));
        return new ManageUsersPage(page, port);
    }
}
