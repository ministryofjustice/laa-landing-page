package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;

@DataJpaTest
public class FirmRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private FirmRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private OfficeRepository officeRepository;

    @BeforeEach
    public void beforeEach() {
        // Delete offices first to avoid foreign key constraint violations
        officeRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveFirm() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildFirm("Firm2", "Firm Code 2");
        repository.saveAllAndFlush(Arrays.asList(firm1, firm2));

        Firm result = repository.findById(firm1.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(firm1.getId());
        Assertions.assertThat(result.getName()).isEqualTo("Firm1");
        Assertions.assertThat(result.getCode()).isEqualTo("Firm Code 1");
        Assertions.assertThat(result.getType()).isEqualTo(FirmType.INDIVIDUAL);

    }


}
