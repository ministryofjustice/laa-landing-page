package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FirmOfficesCriteriaTest {


    @Test
    void testDefaultsWithNoArgsConstructor() {
        FirmOfficesCriteria criteria = new FirmOfficesCriteria();

        assertThat(criteria.getOfficeCode()).isNull();
        assertThat(criteria.getOfficeAddress()).isNull();
        assertThat(criteria.isOfficeStatus()).isFalse();
        
        assertThat(criteria.getSize()).isEqualTo(10);
        assertThat(criteria.getPage()).isEqualTo(1);
        assertThat(criteria.getSort()).isEqualTo("code");
        assertThat(criteria.getDirection()).isEqualTo("asc");
    }


    @Test
    void testAllArgsConstructor() {
        FirmOfficesCriteria criteria = new FirmOfficesCriteria(
                "LDN01",
                "1 Bishopsgate, London",
                true,
                50,
                2,
                "officeCode",
                "desc"
        );

        assertThat(criteria.getOfficeCode()).isEqualTo("LDN01");
        assertThat(criteria.getOfficeAddress()).isEqualTo("1 Bishopsgate, London");
        assertThat(criteria.isOfficeStatus()).isTrue();
        assertThat(criteria.getSize()).isEqualTo(50);
        assertThat(criteria.getPage()).isEqualTo(2);
        assertThat(criteria.getSort()).isEqualTo("officeCode");
        assertThat(criteria.getDirection()).isEqualTo("desc");
    }

}

