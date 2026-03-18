package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.Site;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.config.GraphClientConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportUploadService {

    @Value("${report.sharepoint.site.id}")
    private String sharepointUrl;

    private final GraphClientConfig graphClientConfig;

    public void uploadCsvToSharePoint(File file, String folderPath) throws FileNotFoundException {

        FileInputStream fileInputStream = new FileInputStream(file);

        Site site = graphClientConfig.graphUploadClient()
            .sites()
            .bySiteId(sharepointUrl)
            .get();

        final String siteId = site.getId();

        Drive drive = graphClientConfig.graphUploadClient()
                .sites()
                .bySiteId(siteId)
                .drive()
                .get();

        String driveId = drive.getId();


        try {
            DriveItem existing = graphClientConfig.graphUploadClient()
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId("root:/" + folderPath + "/" + file.getName() + ":")
                .get();

            if (existing != null) {
                log.warn("File '{}' already exists in SharePoint. Skipping upload.", file.getName());
                return;
            }

        } catch (Exception ex) {
            if (ex.getMessage().contains("404")) {
                log.info("File '{}' does not exist in SharePoint. Proceeding with upload.", file.getName());
            }
        }


        log.info("Uploading report to SharePoint: {}", file.getName());

        DriveItem uploadCsv =
                graphClientConfig.graphUploadClient()
                        .drives()
                        .byDriveId(driveId)
                        .items()
                        .byDriveItemId("root:/" + folderPath + "/" + file.getName() + ":")
                        .content()
                        .put(fileInputStream);

        if (uploadCsv != null) {
            log.info("File uploaded successfully");
        } else {
            log.error("File upload failed");
        }
    }
}

