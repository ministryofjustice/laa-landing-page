package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;

import java.util.Arrays;

@DataJpaTest
public class FirmRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private FirmRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveFirm() {
        Firm firm1 = buildFirm("Firm1");
        Firm firm2 = buildFirm("Firm2");
        repository.saveAllAndFlush(Arrays.asList(firm1, firm2));

        Firm result = repository.findById(firm1.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(firm1.getId());
        Assertions.assertThat(result.getName()).isEqualTo("Firm1");
        Assertions.assertThat(result.getCreatedBy()).isEqualTo("Test");
        Assertions.assertThat(result.getCreatedDate()).isNotNull();
        Assertions.assertThat(result.getType()).isEqualTo(FirmType.INDIVIDUAL);

    }


}
