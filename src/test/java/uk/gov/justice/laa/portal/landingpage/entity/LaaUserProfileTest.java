package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LaaUserProfileTest extends BaseEntityTest {

    @Test
    public void testLaaUserProfile() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isEmpty();
        assertNotNull(laaUserProfile);
        assertNotNull(laaUserProfile.getCreatedBy());
        assertThat(laaUserProfile.getCreatedBy()).isEqualTo("test");
        assertNotNull(laaUserProfile.getCreatedDate());
    }

    @Test
    public void testLaaUserProfileNullName() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();
        update(laaUserProfile, laaProf -> {
        });

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isEmpty();
        assertNotNull(laaUserProfile);
    }

    @Test
    public void testLaaUserProfileNullCreatedBy() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();
        update(laaUserProfile, laaProf -> laaProf.setCreatedBy(null));

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Created by must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("createdBy");
    }

    @Test
    public void testLaaUserProfileEmptyCreatedBy() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();
        update(laaUserProfile, l -> l.setCreatedBy(""));

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Created by must be provided", "Created by must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("createdBy");
    }

    @Test
    public void testLaaUserProfileCreatedByTooLong() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();
        update(laaUserProfile, f -> f.setCreatedBy("TestCreatedByThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Created by must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("createdBy");
    }

    @Test
    public void testLaaUserProfileNullCreatedDate() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();
        update(laaUserProfile,  l -> l.setCreatedDate(null));

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Created date must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("createdDate");
    }

    @Test
    public void testLaaUserProfileEmptyLastModifiedBy() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();
        update(laaUserProfile,  l -> l.setLastModifiedBy(""));

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Last modified by must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("lastModifiedBy");
    }

    @Test
    public void testLaaUserProfileLastModifiedByTooLong() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();
        update(laaUserProfile,  l -> l.setLastModifiedBy("TheLastModifiedByIsTooLong".repeat(15)));

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Last modified by must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("lastModifiedBy");
    }

    @Test
    public void testLaaUserProfileNullLastModifiedBy() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();
        update(laaUserProfile,  l -> l.setLastModifiedBy(null));

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testLaaUserProfileNullUserType() {
        LaaUserProfile laaUserProfile = buildTestLaaUserProfile();
        update(laaUserProfile, lup -> lup.setUserType(null));

        Set<ConstraintViolation<LaaUserProfile>> violations = validator.validate(laaUserProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User type must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("userType");
    }

    @Test
    public void testLaaUserProfileInvalidUserType() {
        assertThrows(IllegalArgumentException.class, () -> LaaUserProfile.builder().entraUser(buildTestEntraUser())
                .userType(UserType.valueOf("INVALID"))
                .createdDate(LocalDateTime.now()).createdBy("test").build());
    }

}