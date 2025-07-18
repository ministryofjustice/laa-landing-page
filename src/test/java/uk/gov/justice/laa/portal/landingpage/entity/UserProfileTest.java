package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserProfileTest extends BaseEntityTest {

    @Test
    public void testLaaUserProfile() {
        UserProfile userProfile = buildTestLaaUserProfile();

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isEmpty();
        assertNotNull(userProfile);
        assertNotNull(userProfile.getCreatedBy());
        assertThat(userProfile.getCreatedBy()).isEqualTo("test");
        assertNotNull(userProfile.getCreatedDate());
    }

    @Test
    public void testLaaUserProfileNullName() {
        UserProfile userProfile = buildTestLaaUserProfile();
        update(userProfile, laaProf -> {
        });

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isEmpty();
        assertNotNull(userProfile);
    }

    @Test
    public void testLaaUserProfileNullLegacyId() {
        UserProfile userProfile = buildTestLaaUserProfile();
        update(userProfile, laaProf -> {
        });

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isEmpty();
        assertNotNull(userProfile);
    }

    @Test
    public void testLaaUserProfileEmptyLegacyId() {
        UserProfile userProfile = buildTestLaaUserProfile();
        update(userProfile, laaProf -> {
        });

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isEmpty();
        assertNotNull(userProfile);
    }

    @Test
    public void testLaaUserProfileLegacyIdGenerated() {
        UserProfile userProfile = buildTestLaaUserProfile();
        userProfile.setLegacyUserId(null);

        assertThat(userProfile.getLegacyUserId()).isNull();

        userProfile.prePersist();

        assertThat(userProfile.getLegacyUserId()).isNotNull();
    }

    @Test
    public void testLaaUserProfileNullCreatedBy() {
        UserProfile userProfile = buildTestLaaUserProfile();
        update(userProfile, laaProf -> laaProf.setCreatedBy(null));

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Created by must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("createdBy");
    }

    @Test
    public void testLaaUserProfileEmptyCreatedBy() {
        UserProfile userProfile = buildTestLaaUserProfile();
        update(userProfile, l -> l.setCreatedBy(""));

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(2);
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertThat(messages).hasSameElementsAs(Set.of("Created by must be provided", "Created by must be between 1 and 255 characters"));
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("createdBy");
    }

    @Test
    public void testLaaUserProfileCreatedByTooLong() {
        UserProfile userProfile = buildTestLaaUserProfile();
        update(userProfile, f -> f.setCreatedBy("TestCreatedByThatIsTooLong".repeat(15)));

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Created by must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("createdBy");
    }

    @Test
    public void testLaaUserProfileNullCreatedDate() {
        UserProfile userProfile = buildTestLaaUserProfile();
        update(userProfile, l -> l.setCreatedDate(null));

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Created date must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("createdDate");
    }

    @Test
    public void testLaaUserProfileEmptyLastModifiedBy() {
        UserProfile userProfile = buildTestLaaUserProfile();
        update(userProfile, l -> l.setLastModifiedBy(""));

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Last modified by must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("lastModifiedBy");
    }

    @Test
    public void testLaaUserProfileLastModifiedByTooLong() {
        UserProfile userProfile = buildTestLaaUserProfile();
        update(userProfile, l -> l.setLastModifiedBy("TheLastModifiedByIsTooLong".repeat(15)));

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Last modified by must be between 1 and 255 characters");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("lastModifiedBy");
    }

    @Test
    public void testLaaUserProfileNullLastModifiedBy() {
        UserProfile userProfile = buildTestLaaUserProfile();
        update(userProfile, l -> l.setLastModifiedBy(null));

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isEmpty();
    }

    @Test
    public void testLaaUserProfileNullUserType() {
        UserProfile userProfile = buildTestLaaUserProfile();
        update(userProfile, lup -> lup.setUserType(null));

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(userProfile);

        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User type must be provided");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("userType");
    }

    @Test
    public void testLaaUserProfileInvalidUserType() {
        assertThrows(IllegalArgumentException.class, () -> UserProfile.builder().entraUser(buildTestEntraUser())
                .userType(UserType.valueOf("INVALID"))
                .createdDate(LocalDateTime.now()).createdBy("test").build());
    }

}