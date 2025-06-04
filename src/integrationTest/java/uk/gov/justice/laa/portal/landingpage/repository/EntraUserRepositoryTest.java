package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.validation.ConstraintViolationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.AppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class EntraUserRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRegistrationRepository appRegistrationRepository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        appRegistrationRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveEntraUser() {
        EntraUser entraUser = buildEntraUser("test@email.com", "FirstName", "LastName");
        repository.saveAndFlush(entraUser);

        EntraUser result = repository.findById(entraUser.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getUserName()).isEqualTo("test@email.com");
        Assertions.assertThat(result.getFirstName()).isEqualTo("FirstName");
        Assertions.assertThat(result.getLastName()).isEqualTo("LastName");
        Assertions.assertThat(result.getEmail()).isEqualTo("test@email.com");
        Assertions.assertThat(result.getCreatedBy()).isEqualTo("Test");
        Assertions.assertThat(result.getCreatedDate()).isNotNull();

    }

    @Test
    public void testSaveAndRetrieveEntraUserWithAppRegistration() {
        EntraUser entraUser = buildEntraUser("test@email.com", "FirstName", "LastName");
        repository.saveAndFlush(entraUser);

        AppRegistration appRegistration = buildEntraAppRegistration("Entra App");
        entraUser.getUserAppRegistrations().add(appRegistration);
        appRegistration.getEntraUsers().add(entraUser);
        appRegistrationRepository.save(appRegistration);

        EntraUser result = repository.findById(entraUser.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getUserAppRegistrations()).isNotEmpty();

        AppRegistration resultAppRegistration = result.getUserAppRegistrations().stream().findFirst().orElseThrow();
        Assertions.assertThat(resultAppRegistration.getId()).isEqualTo(appRegistration.getId());
        Assertions.assertThat(resultAppRegistration.getName()).isEqualTo("Entra App");
        Assertions.assertThat(resultAppRegistration.getEntraUsers()).isNotEmpty();
        Assertions.assertThat(resultAppRegistration.getEntraUsers().stream().findFirst().orElseThrow()).isEqualTo(result);

    }

    @Test
    public void testSaveEntraUserWithInvalidEmail() {
        EntraUser entraUser = buildEntraUser("testemail.com", "FirstName", "LastName");

        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> repository.saveAndFlush(entraUser), "Exception expected");
        Assertions.assertThat(ex.getMessage()).contains("User email must be a valid email address");
    }

    @Test
    public void testSaveEntraUserStartDateAfterEndDate() {
        EntraUser entraUser = buildEntraUser("test@email.com", "FirstName", "LastName");
        entraUser.setStartDate(LocalDateTime.now());
        entraUser.setEndDate(LocalDateTime.now().minusMinutes(1));

        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> repository.saveAndFlush(entraUser), "Exception expected");
        Assertions.assertThat(ex.getMessage()).contains("End date must be after start date");
    }


}
