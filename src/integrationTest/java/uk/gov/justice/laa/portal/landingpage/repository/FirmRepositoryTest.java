package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
        Assertions.assertThat(result.getType()).isEqualTo(FirmType.ADVOCATE);

    }

    @Test
    public void testSaveAndRetrieveChildFirm() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildChildFirm("Firm2", "Firm Code 2", firm1);
        repository.saveAllAndFlush(Arrays.asList(firm1, firm2));

        Firm result = repository.findById(firm2.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(firm2.getId());
        Assertions.assertThat(result.getParentFirm().getId()).isEqualTo(firm1.getId());

    }

    @Test
    public void testSaveSelfParentFirm() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        firm1.setParentFirm(firm1);
        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(firm1), "Exception expected");
        Assertions.assertThat(ex.getMessage()).contains("new row for relation \"firm\" violates check constraint \"self_parent\"");
    }

    @Test
    public void testSaveGrandParentFirm() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildChildFirm("Firm2", "Firm Code 2", firm1);
        Firm firm3 = buildChildFirm("Firm3", "Firm Code 3", firm2);
        JpaSystemException ex = assertThrows(JpaSystemException.class,
                () -> repository.saveAllAndFlush(Arrays.asList(firm1, firm2, firm3)), "Exception expected");
        Assertions.assertThat(ex.getMessage()).contains("parent firm (" + firm2.getId() + ") already has parent");
    }

}
