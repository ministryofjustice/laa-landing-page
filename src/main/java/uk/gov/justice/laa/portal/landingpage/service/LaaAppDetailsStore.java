package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.microsoft.graph.models.Application;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.utils.HashUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A temporary store for LAA applications of their name, description, url.
 *
 * The class loads all the LAA applications details during server startup and map them against the
 * user assigned Entra app details and help populate on landing age.
 */
@Component
public class LaaAppDetailsStore {

    private static final String LAA_APP_META_FILE_PATH = "data/laa-apps-details.csv";
    private static List<LaaApplication> laaApplications;

    public static List<LaaApplication> getUserAssignedApps(List<Application> registeredApps) {
        return laaApplications.stream().filter(app -> registeredApps.stream()
                        .map(Application::getAppId).anyMatch(resId -> HashUtil.sha256(resId).equals(app.getId())))
                .collect(Collectors.toList());
    }

    public static void populateAppDetails(LaaApplication application) {
        LaaApplication laaMeta = laaApplications.stream().filter(app -> app.getId().equals(HashUtil.sha256(application.getId()))).findFirst().orElse(application);
        application.setDescription(laaMeta.getDescription());
        application.setUrl(laaMeta.getUrl());
        application.setTitle(laaMeta.getTitle());
        application.setOidGroupName(laaMeta.getOidGroupName());
    }

    @PostConstruct
    void populateLaaApps() throws IOException {
        InputStream csvFileStream = getClass().getClassLoader().getResourceAsStream(LAA_APP_META_FILE_PATH);

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper
                .schemaFor(LaaApplication.class)
                .withoutHeader();

        try (MappingIterator<LaaApplication> iterator =
                     mapper.readerFor(LaaApplication.class).with(schema).readValues(csvFileStream)) {
            laaApplications = iterator.readAll();
        }
    }

}
