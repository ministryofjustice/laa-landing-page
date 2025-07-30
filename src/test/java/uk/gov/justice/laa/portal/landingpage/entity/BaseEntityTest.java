package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class BaseEntityTest {

    protected static Validator validator;

    @BeforeAll
    public static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

    }

    public <T extends BaseEntity> void update(T type, Consumer<T> action) {
        action.accept(type);
    }

    protected Firm buildTestFirm() {
        return Firm.builder().name("TestFirm").type(FirmType.LEGAL_SERVICES_PROVIDER).build();
    }

    protected Office buildTestOffice() {
        return Office.builder().name("TestOffice").code("office code").address("Address").phone("123456").build();
    }

    protected EntraUser buildTestEntraUser() {
        return EntraUser.builder().firstName("FirstName").lastName("LastName").userStatus(UserStatus.ACTIVE)
                .entraOid("entra_id").email("test@email.com")
                .createdBy("test").createdDate(LocalDateTime.now()).build();
    }

    protected App buildTestLaaApp() {
        return App.builder().name("Test App").entraAppId("Entra App Id")
                .securityGroupOid("SecGroupId").securityGroupName("SecGroup Name")
                .build();
    }

    protected AppRole buildTestLaaAppRole() {
        return AppRole.builder().name("Test App Role").description("App Role Description")
                .roleType(RoleType.INTERNAL).build();
    }

    protected Permission buildTestPermission(Set<AppRole> appRoles) {
        return Permission.builder().name("Test Permission").description("description")
                .function("function").appRoles(appRoles).build();
    }

    protected UserProfile buildTestLaaUserProfile() {
        return UserProfile.builder().entraUser(buildTestEntraUser())
                .userType(UserType.INTERNAL).legacyUserId(UUID.randomUUID())
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .createdDate(LocalDateTime.now()).createdBy("test").build();
    }

}
