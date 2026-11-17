package uk.gov.justice.laa.portal.landingpage.playwright.common;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.ManageUsersPage;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.UserProfilePage;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Value("${app.playwright.proxy-url:}")
    private String proxyUrl;

    private static boolean setupComplete = false;

    @ServiceConnection
    public static final PostgreSQLContainer<?> postgresServer =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("laa_portal_test")
                    .withUsername("root")
                    .withPassword("secret");

    static {
        postgresServer.start();

        System.setProperty("spring.datasource.url", postgresServer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgresServer.getUsername());
        System.setProperty("spring.datasource.password", postgresServer.getPassword());
    }

    @RegisterExtension
    ScreenshotWatcher screenshotOnFailure =
            new ScreenshotWatcher(() -> page);



    @BeforeAll
    void beforeAll() {
        if (!setupComplete) {
            playwright = Playwright.create();

            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(headless);
            if (!proxyUrl.isBlank()) {
                options.setProxy(new Proxy(proxyUrl))
                        .setArgs(List.of("--proxy-bypass-list=<-loopback>"));
            }
            browser = playwright.chromium().launch(options);

            setupComplete = true;
        }
    }

    @BeforeEach
    void beforeEach() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setIgnoreHTTPSErrors(!proxyUrl.isBlank()));
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

    protected UserProfilePage loginAndGetUserProfilePage(TestUser user, String userEmail) {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(user);
        manageUsersPage.searchForUser(userEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        assertTrue(page.url().contains("/admin/users/manage/"));

        return new UserProfilePage(page, port);
    }
}
