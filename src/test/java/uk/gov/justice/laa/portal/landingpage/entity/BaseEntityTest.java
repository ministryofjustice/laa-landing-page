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
        Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("postcode").build();
        return Office.builder().code("office code").address(address).build();
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
        return AppRole.builder().name("Test App Role").ccmsCode("ccms_code").description("App Role Description")
                .roleType(RoleType.INTERNAL).build();
    }

    protected UserProfile buildTestLaaUserProfile() {
        return UserProfile.builder().entraUser(buildTestEntraUser())
                .userType(UserType.INTERNAL).legacyUserId(UUID.randomUUID())
                .userProfileStatus(UserProfileStatus.COMPLETE)
                .createdDate(LocalDateTime.now()).createdBy("test").build();
    }

}
