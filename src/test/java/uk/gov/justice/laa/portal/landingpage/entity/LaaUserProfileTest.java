package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaaUserProfileTest extends BaseEntityTest {

    @Test
    public void testLaaUserProfile() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isEmpty();
        assertNotNull(laaUserProfile);
        assertTrue(laaUserProfile.isAdmin());
        assertTrue(laaUserProfile.isMultiFirm());
        assertThat(laaUserProfile.getCreatedBy()).isNotEmpty();
        assertNotNull(laaUserProfile.getCreatedDate());
    }

    @Test
    public void testLaaUserProfileNullName() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();
        update(laaUserProfile, laaProf -> {
            laaProf.setAdmin(false);
            laaProf.setMultiFirm(false);
        });

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isEmpty();
        assertNotNull(laaUserProfile);
        assertFalse(laaUserProfile.isAdmin());
        assertFalse(laaUserProfile.isMultiFirm());
    }
}