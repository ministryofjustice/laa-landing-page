package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.validation.ConstraintViolationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.AppRegistration;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class AppRegistrationRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private AppRegistrationRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveEntraAppRegistration() {
        AppRegistration appRegistration = buildEntraAppRegistration("App Registration 1");
        repository.saveAndFlush(appRegistration);

        AppRegistration result = repository.findById(appRegistration.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(appRegistration.getId());
        Assertions.assertThat(result.getName()).isEqualTo("App Registration 1");

    }

    @Test
    public void testAppNameTooSmall() {
        AppRegistration appRegistration = buildEntraAppRegistration("");

        assertThrows(ConstraintViolationException.class,
                () -> repository.saveAndFlush(appRegistration), "Exception expected");
    }

    @Test
    public void testAppNameTooBig() {
        AppRegistration appRegistration = buildEntraAppRegistration("a".repeat(256));

        assertThrows(ConstraintViolationException.class,
                () -> repository.saveAndFlush(appRegistration), "Exception expected");
    }


}
