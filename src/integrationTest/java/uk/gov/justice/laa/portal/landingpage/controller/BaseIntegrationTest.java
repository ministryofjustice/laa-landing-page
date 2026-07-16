package uk.gov.justice.laa.portal.landingpage.controller;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.BaseRepositoryTest;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserAccountStatusAuditRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

/**
 * base integration test
 */
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@EnableConfigurationProperties
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "management.endpoints.web.exposure.include=health,metrics",
    "management.endpoint.env.access=read_only",
    "management.endpoint.metrics.access=read_only"
})
public abstract class BaseIntegrationTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected WebApplicationContext context;

    protected MockMvc mockMvc;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected EntraUserRepository entraUserRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected UserProfileRepository userProfileRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected FirmRepository firmRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected OfficeRepository officeRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected UserAccountStatusAuditRepository userAccountStatusAuditRepository;

    protected EntraUser defaultLoggedInUser;

    protected EntraUser silasAdminUser;

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    protected void baseAfterEach() {
        entityManager.clear();

        transactionTemplate.executeWithoutResult(status -> {
            try {
                entityManager.createNativeQuery("SET session_replication_role = replica;").executeUpdate();

                entityManager.createNativeQuery("DELETE FROM user_account_status_audit;").executeUpdate();
                entityManager.createNativeQuery("DELETE FROM user_profile;").executeUpdate();
                entityManager.createNativeQuery("DELETE FROM entra_user;").executeUpdate();

                entityManager.createNativeQuery("DELETE FROM office;").executeUpdate();
                entityManager.createNativeQuery("DELETE FROM firm;").executeUpdate();

            } finally {
                entityManager.createNativeQuery("SET session_replication_role = DEFAULT;").executeUpdate();
            }
        });
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        defaultLoggedInUser = buildGlobalAdmin();
        silasAdminUser = buildSilasAdmin();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    protected final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules().configure(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    protected <T> T performPostRequest(String path, Object object, Class<T> responseType, ResultMatcher expectedStatus)
            throws Exception {
        MvcResult mvcResult = getResultActions(path, object)
                .andExpect(expectedStatus)
                .andReturn();
        return convertStringToClass(mvcResult.getResponse().getContentAsString(), responseType);
    }

    protected Map<String, Object> performPostRequestWithParams(String path,
                                               LinkedMultiValueMap<String, String> requestParams,
                                               ResultMatcher expectedStatus,
                                               String view)
            throws Exception {
        MvcResult mvcResult = getResultActions(path, requestParams)
                .andExpect(expectedStatus)
                .andExpect(view().name(view))
                .andReturn();
        return mvcResult.getModelAndView().getModel();
    }

    protected <T> T performPostRequestExpectedSuccess(String path, Object object, Class<T> responseType)
            throws Exception {
        return performPostRequest(path, object, responseType, status().is2xxSuccessful());
    }

    protected <T> T performPostRequestExpectedServerError(String path, Object object, Class<T> responseType)
            throws Exception {
        return performPostRequest(path, object, responseType, status().is5xxServerError());
    }

    private ResultActions getResultActions(String path, Object object) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(object)).with(csrf()));
    }

    private ResultActions getResultActions(String path, LinkedMultiValueMap<String, String> requestParams) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)
                .params(requestParams).with(csrf()));
    }

    private <T> T convertStringToClass(String jsonString, Class<T> responseType) throws JsonProcessingException {
        return mapper.readValue(jsonString, responseType);
    }

    protected SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor defaultOauth2Login(EntraUser user) {
        String[] userPermissions = Permission.ADMIN_PERMISSIONS;

        Set<SimpleGrantedAuthority> authorities = Arrays.stream(userPermissions)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        Map<String, Object> claims = new HashMap<>();
        String userOid = Objects.requireNonNullElse(user.getEntraOid(), "00000000-0000-0000-0000-000000000000");

        claims.put("sub", userOid); // Entra ID maps the principal subject to the unique OID
        claims.put("oid", userOid);
        claims.put("name", Objects.requireNonNullElse(user.getFirstName(), "TestAdmin"));
        claims.put("preferred_username", Objects.requireNonNullElse(user.getEmail(), "admin@test.com"));
        claims.put("email", Objects.requireNonNullElse(user.getEmail(), "admin@test.com"));

        OidcIdToken idToken = new OidcIdToken(
                "mock-entra-id-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
        );

        DefaultOidcUser realPrincipal = new DefaultOidcUser(authorities, idToken, "name");

        return oauth2Login().oauth2User(realPrincipal);
    }

    protected SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor userOauth2Login(EntraUser user) {
        EntraUser freshUser = entraUserRepository.findByIdWithAssociations(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found: " + user.getId()));

        Set<SimpleGrantedAuthority> authorities = freshUser.getUserProfiles().stream()
                .findFirst()
                .map(profile -> profile.getAppRoles().stream()
                        .flatMap(appRole -> appRole.getPermissions().stream())
                        .map(permission -> new SimpleGrantedAuthority(permission.name()))
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());

        Map<String, Object> claims = new HashMap<>();
        String userOid = Objects.requireNonNullElse(freshUser.getEntraOid(), UUID.randomUUID().toString());
        claims.put("sub", userOid);
        claims.put("oid", userOid);
        claims.put("name", freshUser.getFirstName() + " " + freshUser.getLastName());
        claims.put("preferred_username", freshUser.getEmail());
        claims.put("email", freshUser.getEmail());

        OidcIdToken idToken = new OidcIdToken(
                "mock-azure-entra-id-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
        );

        DefaultOidcUser oidcPrincipal = new DefaultOidcUser(authorities, idToken, "name");

        return oauth2Login().oauth2User(oidcPrincipal);
    }

    public EntraUser buildGlobalAdmin() {
        EntraUser loggedInUser = buildEntraUser(generateEntraId(), "test@test.com", "Test", "User");
        UserProfile profile = buildLaaUserProfile(loggedInUser, UserType.INTERNAL, true, "Global Admin");
        loggedInUser.setUserProfiles(Set.of(profile));
        return entraUserRepository.saveAndFlush(loggedInUser);
    }

    public EntraUser buildSilasAdmin() {
        EntraUser loggedInUser = buildEntraUser(generateEntraId(), "silas-admin@test.com", "SiLAS", "Admin");
        UserProfile profile = buildLaaUserProfile(loggedInUser, UserType.INTERNAL, true, "SILAS System Administration");
        loggedInUser.setUserProfiles(Set.of(profile));
        return entraUserRepository.saveAndFlush(loggedInUser);
    }


}
