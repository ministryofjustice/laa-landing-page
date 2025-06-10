package uk.gov.justice.laa.portal.landingpage.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.AppRegistrationRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

@ExtendWith(MockitoExtension.class)
class DemoDataPopulatorTest {

    @Mock
    private AppRegistrationRepository entraAppRegistrationRepository;

    @Mock
    private FirmRepository firmRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private EntraUserRepository entraUserRepository;

    @Mock
    private AppRepository laaAppRepository;

    @Mock
    private AppRoleRepository laaAppRoleRepository;

    @Mock
    private UserProfileRepository laaUserProfileRepository;

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    @InjectMocks
    private DemoDataPopulator demoDataPopulator;

    @Test
    void populateDummyDataDisabled() {
        demoDataPopulator.appReady(applicationReadyEvent);
        verifyMockCalls(0);
    }

    @Test
    void populateDummyDataEnabled() {
        ReflectionTestUtils.setField(demoDataPopulator, "populateDummyData", true);
        demoDataPopulator.appReady(applicationReadyEvent);
        verifyMockCalls(1);
    }

    private void verifyMockCalls(int times) {
        Mockito.verify(entraAppRegistrationRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(firmRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(officeRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(entraUserRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(laaAppRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(laaAppRoleRepository, Mockito.times(times)).saveAll(Mockito.anyList());
        Mockito.verify(laaUserProfileRepository, Mockito.times(times)).saveAll(Mockito.anyList());
    }

}