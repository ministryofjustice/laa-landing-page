package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.LaaUserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.Arrays;

public class LaaUserProfileRepositoryTest extends BaseRepositoryTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EntraUserRepository entraUserRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private LaaUserProfileRepository repository;

    @BeforeEach
    public void beforeEach() {
        repository.deleteAll();
        entraUserRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveLaaUserProfile() {
        EntraUser entraUser = buildEntraUser("test@email.com", "First Name5", "Last Name5", UserType.EXTERNAL);
        entraUserRepository.saveAndFlush(entraUser);

        LaaUserProfile laaUserProfile = buildLaaUserProfile(entraUser);
        repository.saveAndFlush(laaUserProfile);

        LaaUserProfile result = repository.getById(laaUserProfile.getId());

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(laaUserProfile.getId());
        Assertions.assertThat(result.getEntraUser()).isNotNull();
        Assertions.assertThat(result.getEntraUser().getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getEntraUser().getFirstName()).isEqualTo("First Name5");
        Assertions.assertThat(result.getEntraUser().getLastName()).isEqualTo("Last Name5");

    }

    @Test
    public void testSaveAndRetrieveMultipleLaaUserProfilesForEntraUser() {
        Firm firm = buildFirm("Firm1");

        EntraUser entraUser = buildEntraUser("test6@email.com", "First Name6", "Last Name6", UserType.EXTERNAL);
        entraUserRepository.saveAndFlush(entraUser);

        LaaUserProfile laaUserProfile1 = buildLaaUserProfile(entraUser);
        LaaUserProfile laaUserProfile2 = buildLaaUserProfile(entraUser);
        entraUser.getLaaUserProfiles().add(laaUserProfile1);
        entraUser.getLaaUserProfiles().add(laaUserProfile2);

        repository.saveAllAndFlush(Arrays.asList(laaUserProfile1, laaUserProfile2));

        LaaUserProfile result = repository.getById(laaUserProfile1.getId());

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(laaUserProfile1.getId());
        Assertions.assertThat(result.getEntraUser()).isNotNull();
        Assertions.assertThat(result.getEntraUser().getId()).isEqualTo(entraUser.getId());
        Assertions.assertThat(result.getEntraUser().getFirstName()).isEqualTo("First Name6");
        Assertions.assertThat(result.getEntraUser().getLastName()).isEqualTo("Last Name6");
        Assertions.assertThat(result.getEntraUser().getLaaUserProfiles()).isNotEmpty();
        Assertions.assertThat(result.getEntraUser().getLaaUserProfiles()).containsExactlyInAnyOrder(laaUserProfile1, laaUserProfile2);

    }
}
