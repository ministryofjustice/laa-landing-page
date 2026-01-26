package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FirmDirectorySearchCriteriaTest {

    @Test
    void testDefaultConstructor() {
        // When
        FirmDirectorySearchCriteria criteria = new FirmDirectorySearchCriteria();

        // Then
        assertThat(criteria).isNotNull();
        assertThat(criteria.getSearch()).isEqualTo("");
        assertThat(criteria.getFirmSearch()).isNull();
        assertThat(criteria.getSelectedFirmId()).isNull();
        assertThat(criteria.getSelectedFirmType()).isNull();
        assertThat(criteria.getSize()).isEqualTo(10);
        assertThat(criteria.getPage()).isEqualTo(1);
        assertThat(criteria.getSort()).isEqualTo("name");
        assertThat(criteria.getDirection()).isEqualTo("asc");
    }

    @Test
    void testAllArgsConstructor() {
        // When
        UUID firmId = UUID.randomUUID();
        FirmDirectorySearchCriteria criteria = new FirmDirectorySearchCriteria();
        criteria.setSelectedFirmId(firmId.toString());
        criteria.setSearch("search");
        criteria.setFirmSearch("FirmSearch");
        criteria.setSelectedFirmType(FirmType.CHAMBERS.getValue());
        criteria.setPage(20);
        criteria.setSize(20);
        criteria.setDirection("desc");
        criteria.setSort("code");

        // Then
        assertThat(criteria).isNotNull();
        assertThat(criteria.getSearch()).isEqualTo("search");
        assertThat(criteria.getFirmSearch()).isEqualTo("FirmSearch");
        assertThat(criteria.getSelectedFirmId()).isEqualTo(firmId);
        assertThat(criteria.getSelectedFirmType()).isEqualTo(FirmType.CHAMBERS.getValue());
        assertThat(criteria.getSize()).isEqualTo(20);
        assertThat(criteria.getPage()).isEqualTo(20);
        assertThat(criteria.getSort()).isEqualTo("code");
        assertThat(criteria.getDirection()).isEqualTo("desc");
    }

}
