package uk.gov.justice.laa.portal.landingpage.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.BaseRepositoryTest;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * base integration test
 */
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@EnableConfigurationProperties
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public abstract class BaseIntegrationTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected MockMvc mockMvc;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected EntraUserRepository entraUserRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected UserProfileRepository userProfileRepository;

    protected EntraUser defaultLoggedInUser;

    @AfterAll
    protected void baseAfterAll() {
        userProfileRepository.deleteAll();
        entraUserRepository.deleteAll();
    }

    @BeforeAll
    public void beforeAll() {
        defaultLoggedInUser = buildGlobalAdmin();
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

        OAuth2User realPrincipal = new DefaultOAuth2User(
                authorities,
                Map.of("name", user.getFirstName(), "oid", user.getEntraOid()),
                "name");
        return oauth2Login().oauth2User(realPrincipal);
    }

    protected SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor userOauth2Login(EntraUser user) {
        Set<Permission> userPermissions = user.getUserProfiles().stream()
                .findFirst()
                .orElseThrow()
                .getAppRoles().stream()
                .flatMap(appRole -> appRole.getPermissions().stream())
                .collect(Collectors.toSet());

        Set<SimpleGrantedAuthority> authorities = userPermissions.stream()
                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                .collect(Collectors.toSet());

        OAuth2User realPrincipal = new DefaultOAuth2User(
                authorities,
                Map.of("name", user.getFirstName(), "oid", user.getEntraOid()),
                "name");
        return oauth2Login().oauth2User(realPrincipal);
    }

    public EntraUser buildGlobalAdmin() {
        EntraUser loggedInUser = buildEntraUser(generateEntraId(), "test@test.com", "Test", "User");
        UserProfile profile = buildLaaUserProfile(loggedInUser, UserType.INTERNAL, true);
        loggedInUser.setUserProfiles(Set.of(profile));
        return entraUserRepository.saveAndFlush(loggedInUser);
    }
}
