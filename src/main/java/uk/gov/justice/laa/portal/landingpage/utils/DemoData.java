package uk.gov.justice.laa.portal.landingpage.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;

@Component
public class DemoData {

    @Autowired
    private FirmRepository firmRepository;
    @Autowired
    private OfficeRepository repository;

    @EventListener
    public void appReady(ApplicationReadyEvent event) {
        initialTestData();
    }

    private void initialTestData() {
        Firm firm1 = buildFirm("Firm1");
        Firm firm2 = buildFirm("Firm2");
        firmRepository.saveAllAndFlush(Arrays.asList(firm1, firm2));

        Office office1 = buildOffice(firm1, "Office1", "Addr 1", "12345");
        Office office2 = buildOffice(firm2, "Office2", "Addr 2", "23456");
        Office office3 = buildOffice(firm2, "Office3", "Addr 3", "34567");
        firm1.getOffices().add(office1);
        firm2.getOffices().add(office2);
        firm2.getOffices().add(office3);

        repository.saveAllAndFlush(Arrays.asList(office1, office2, office3));

        System.out.println("has office count: " + repository.findAll().size());
    }

    private Office buildOffice(Firm firm, String name, String address, String phone) {
        return Office.builder().name(name).address(address).phone(phone)
                .firm(firm).build();
    }

    private Firm buildFirm(String name) {
        return Firm.builder().name(name).offices(HashSet.newHashSet(11))
                .type(FirmType.INDIVIDUAL).build();
    }
}
