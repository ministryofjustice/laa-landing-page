package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.validation.ConstraintViolationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.EntraAppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class EntraUserRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraAppRegistrationRepository entraAppRegistrationRepository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        entraAppRegistrationRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveEntraUser() {
        EntraUser entraUser = buildEntraUser("test@email.com", "FirstName", "LastName", UserType.INTERNAL);
        repository.saveAndFlush(entraUser);

        EntraUser result = repository.findById(entraUser.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getFirstName()).isEqualTo("FirstName");
        Assertions.assertThat(result.getLastName()).isEqualTo("LastName");
        Assertions.assertThat(result.getEmail()).isEqualTo("test@email.com");
        Assertions.assertThat(result.getCreatedBy()).isEqualTo("Test");
        Assertions.assertThat(result.getCreatedDate()).isNotNull();
        Assertions.assertThat(result.getUserType()).isEqualTo(UserType.INTERNAL);

    }

    @Test
    public void testSaveAndRetrieveEntraUserWithAppRegistration() {
        EntraUser entraUser = buildEntraUser("test@email.com", "FirstName", "LastName", UserType.INTERNAL);
        repository.saveAndFlush(entraUser);

        EntraAppRegistration entraAppRegistration = buildEntraAppRegistration("Entra App");
        entraUser.getUserAppRegistrations().add(entraAppRegistration);
        entraAppRegistration.getEntraUsers().add(entraUser);
        entraAppRegistrationRepository.save(entraAppRegistration);

        EntraUser result = repository.findById(entraUser.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getUserAppRegistrations()).isNotEmpty();

        EntraAppRegistration resultEntraAppRegistration = result.getUserAppRegistrations().stream().findFirst().orElseThrow();
        Assertions.assertThat(resultEntraAppRegistration.getId()).isEqualTo(entraAppRegistration.getId());
        Assertions.assertThat(resultEntraAppRegistration.getName()).isEqualTo("Entra App");
        Assertions.assertThat(resultEntraAppRegistration.getCreatedBy()).isEqualTo("Test");
        Assertions.assertThat(resultEntraAppRegistration.getCreatedDate()).isNotNull();
        Assertions.assertThat(resultEntraAppRegistration.getEntraUsers()).isNotEmpty();
        Assertions.assertThat(resultEntraAppRegistration.getEntraUsers().stream().findFirst().orElseThrow()).isEqualTo(result);

    }

    @Test
    public void testSaveEntraUserWithInvalidEmail() {
        EntraUser entraUser = buildEntraUser("testemail.com", "FirstName", "LastName", UserType.INTERNAL);

        assertThrows(ConstraintViolationException.class,
                () -> repository.saveAndFlush(entraUser), "Exception expected");
    }


}
