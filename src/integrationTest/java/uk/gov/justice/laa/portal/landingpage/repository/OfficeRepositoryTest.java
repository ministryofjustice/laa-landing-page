package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@DataJpaTest
public class OfficeRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private OfficeRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private FirmRepository firmRepository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        firmRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveOffice() {
        Firm firm1 = buildFirm("Firm1", "Firm Code 1");
        Firm firm2 = buildFirm("Firm2", "Firm Code 2");
        firmRepository.saveAllAndFlush(Arrays.asList(firm1, firm2));

        Office office1 = buildOffice(firm1, "Office1", "Addr 1", "12345", "Office Code 1");
        Office office2 = buildOffice(firm2, "Office2", "Addr 2", "23456", "Office Code 2");
        Office office3 = buildOffice(firm2, "Office3", "Addr 3", "34567", "Office Code 3");
        firm1.getOffices().add(office1);
        firm2.getOffices().add(office2);
        firm2.getOffices().add(office3);

        repository.saveAllAndFlush(Arrays.asList(office1, office2, office3));

        Office result = repository.findById(office2.getId()).orElseThrow();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(office2.getId());
        Assertions.assertThat(result.getCode()).isEqualTo("Office Code 2");
        Assertions.assertThat(result.getFirm()).isNotNull();

        List<UUID> officeIds = Arrays.asList(firm2.getId(), firm1.getId());
        List<Office> offices = repository.findOfficeByFirm_IdIn(officeIds);
        Assertions.assertThat(offices).hasSize(3);

        List<UUID> f2OfficeId = Arrays.asList(firm2.getId());
        List<Office> f2offices = repository.findOfficeByFirm_IdIn(f2OfficeId);
        Assertions.assertThat(f2offices).hasSize(2);

        List<Office> allOffices = repository.findAll();
        Assertions.assertThat(allOffices).hasSize(3);

        Firm firm = result.getFirm();
        Assertions.assertThat(firm.getId()).isEqualTo(firm2.getId());
        Assertions.assertThat(firm.getName()).isEqualTo("Firm2");
        Assertions.assertThat(firm.getCode()).isEqualTo("Firm Code 2");
        Assertions.assertThat(firm.getType()).isEqualTo(FirmType.ADVOCATE);
        Assertions.assertThat(firm.getOffices()).containsExactlyInAnyOrder(office2, office3);

    }

}
