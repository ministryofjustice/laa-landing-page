package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;

import java.time.LocalDateTime;
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
        return Office.builder().name("TestOffice").address("Address").phone("123456").build();
    }

    protected AppRegistration buildTestEntraAppRegistration() {
        return AppRegistration.builder().name("Test app reg").build();
    }

    protected EntraUser buildTestEntraUser() {
        return EntraUser.builder().firstName("FirstName").lastName("LastName").userStatus(UserStatus.ACTIVE)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusYears(1))
                .userName("firstname.lastname").email("test@email.com")
                .createdBy("test").createdDate(LocalDateTime.now()).build();
    }

    protected App buildTestLaaApp() {
        return App.builder().name("Test App").build();
    }

    protected AppRole buildTestLaaAppRole() {
        return AppRole.builder().name("Test App Role").build();
    }

    protected UserProfile buildTestLaaUserProfile() {
        return UserProfile.builder().entraUser(buildTestEntraUser())
                .userType(UserType.INTERNAL)
                .createdDate(LocalDateTime.now()).createdBy("test").build();
    }

}
