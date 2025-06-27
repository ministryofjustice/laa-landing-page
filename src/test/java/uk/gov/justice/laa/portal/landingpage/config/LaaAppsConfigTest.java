package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = LaaAppsConfig.LaaApplicationsList.class)
@TestPropertySource(properties = {"azure.client-id=test-client-id", "azure.client-secret=test-client-secret", "azure.tenant-id=test-tenant-id"})
public class LaaAppsConfigTest {

    private LaaAppsConfig.LaaApplicationsList laaApplicationsList;

    @Test
    public void noAppsShouldReturnEmptyList() {
        laaApplicationsList = new LaaAppsConfig.LaaApplicationsList();

        List<LaaApplication> apps = laaApplicationsList.getApplications();

        assertThat(apps).isNotNull();
        assertThat(apps).isEmpty();
    }

    @Test
    public void testWithApplications() {
        laaApplicationsList = new LaaAppsConfig.LaaApplicationsList();
        List<LaaApplication> applications = Arrays.asList(
                LaaApplication.builder().name("test1").build(),
                LaaApplication.builder().name("test2").build()
        );
        laaApplicationsList.setApplications(applications);

        List<LaaApplication> apps = laaApplicationsList.getApplications();

        assertThat(apps).isNotNull();
        assertThat(apps).hasSize(2);
    }

}
