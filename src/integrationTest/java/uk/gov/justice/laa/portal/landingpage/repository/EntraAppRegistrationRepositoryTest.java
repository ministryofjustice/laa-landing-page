package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.validation.ConstraintViolationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.EntraAppRegistration;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class EntraAppRegistrationRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraAppRegistrationRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveEntraAppRegistration() {
        EntraAppRegistration entraAppRegistration = buildEntraAppRegistration("Entra App 1");
        repository.saveAndFlush(entraAppRegistration);

        EntraAppRegistration result = repository.findById(entraAppRegistration.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(entraAppRegistration.getId());
        Assertions.assertThat(result.getName()).isEqualTo("Entra App 1");

    }

    @Test
    public void testAppNameTooSmall() {
        EntraAppRegistration entraAppRegistration = buildEntraAppRegistration("");

        assertThrows(ConstraintViolationException.class,
                () -> repository.saveAndFlush(entraAppRegistration), "Exception expected");
    }

    @Test
    public void testAppNameTooBig() {
        EntraAppRegistration entraAppRegistration = buildEntraAppRegistration("a".repeat(256));

        assertThrows(ConstraintViolationException.class,
                () -> repository.saveAndFlush(entraAppRegistration), "Exception expected");
    }


}
