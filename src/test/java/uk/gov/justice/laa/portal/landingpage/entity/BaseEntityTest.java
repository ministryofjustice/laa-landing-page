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
        return Firm.builder().name("TestFirm").type(FirmType.LEGAL_SERVICES_PROVIDER)
                .createdBy("test").createdDate(LocalDateTime.now()).build();
    }

    protected Office buildTestOffice() {
        return Office.builder().name("TestOffice").address("Address").phone("123456")
                .createdBy("test").createdDate(LocalDateTime.now()).build();
    }

    protected EntraAppRegistration buildTestEntraAppRegistration() {
        return EntraAppRegistration.builder().name("Test Entra app reg")
                .createdBy("test").createdDate(LocalDateTime.now()).build();
    }

    protected EntraUser buildTestEntraUser() {
        return EntraUser.builder().firstName("FirstName").lastName("LastName")
                .active(true).startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusYears(1))
                .email("test@email.com").userType(UserType.INTERNAL)
                .createdBy("test").createdDate(LocalDateTime.now()).build();
    }

    protected LaaApp buildTestLaaApp() {
        return LaaApp.builder().name("Test Laa App").createdBy("test").createdDate(LocalDateTime.now()).build();
    }

    protected LaaAppRole buildTestLaaAppRole() {
        return LaaAppRole.builder().name("Test Laa App Role").createdBy("test").createdDate(LocalDateTime.now()).build();
    }

    protected LaaUserProfile buildTestLaaUserProfile() {
        return LaaUserProfile.builder().entraUser(buildTestEntraUser())
                .admin(true).multiFirm(true)
                .createdDate(LocalDateTime.now()).createdBy("test").build();
    }

}
