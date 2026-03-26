package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.drives.item.items.ItemsRequestBuilder;
import com.microsoft.graph.drives.item.items.item.DriveItemItemRequestBuilder;
import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.Site;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.sites.SitesRequestBuilder;
import com.microsoft.graph.sites.item.SiteItemRequestBuilder;
import com.microsoft.graph.sites.item.drive.DriveRequestBuilder;
import com.microsoft.graph.drives.DrivesRequestBuilder;
import com.microsoft.graph.drives.item.DriveItemRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.config.GraphClientConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ReportUploadServiceTest {

    @Mock
    private GraphClientConfig graphClientConfig;

    @Mock
    private GraphServiceClient graphClient;

    @InjectMocks
    private ReportUploadService reportUploadService;

    private File testFile;

    @BeforeEach
    void setup() throws Exception {
        // Create a temporary test CSV file
        testFile = File.createTempFile("test-report", ".csv");
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("Firm Name,Firm Code,User Count\n");
            writer.write("Test Firm,TEST,5\n");
        }

        Field sharepointField = ReportUploadService.class.getDeclaredField("sharepointUrl");
        sharepointField.setAccessible(true);
        sharepointField.set(reportUploadService, "test-site-id");
    }

    @Test
    void uploadCsvToSharePoint_success() throws Exception {

        Site mockSite = mock(Site.class);
        when(mockSite.getId()).thenReturn("site-123");

        Drive mockDrive = mock(Drive.class);
        when(mockDrive.getId()).thenReturn("drive-456");

        DriveItem mockDriveItem = mock(DriveItem.class);

        SitesRequestBuilder sitesBuilder = mock(SitesRequestBuilder.class);
        SiteItemRequestBuilder siteItemBuilder = mock(SiteItemRequestBuilder.class);
        DriveRequestBuilder driveBuilder = mock(DriveRequestBuilder.class);

        when(graphClient.sites()).thenReturn(sitesBuilder);
        when(sitesBuilder.bySiteId(anyString())).thenReturn(siteItemBuilder);
        when(siteItemBuilder.get()).thenReturn(mockSite);
        when(siteItemBuilder.drive()).thenReturn(driveBuilder);
        when(driveBuilder.get()).thenReturn(mockDrive);

        DrivesRequestBuilder drivesBuilder = mock(DrivesRequestBuilder.class);
        DriveItemRequestBuilder driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
        ItemsRequestBuilder itemsBuilder = mock(ItemsRequestBuilder.class);
        DriveItemItemRequestBuilder driveItemItemBuilder = mock(DriveItemItemRequestBuilder.class);
        com.microsoft.graph.drives.item.items.item.content.ContentRequestBuilder contentBuilder =
            mock(com.microsoft.graph.drives.item.items.item.content.ContentRequestBuilder.class);

        when(graphClient.drives()).thenReturn(drivesBuilder);
        when(drivesBuilder.byDriveId(anyString())).thenReturn(driveItemRequestBuilder);
        when(driveItemRequestBuilder.items()).thenReturn(itemsBuilder);

        when(itemsBuilder.byDriveItemId(anyString())).thenReturn(driveItemItemBuilder);
        when(driveItemItemBuilder.get()).thenThrow(new RuntimeException("404"));

        when(driveItemItemBuilder.content()).thenReturn(contentBuilder);
        when(contentBuilder.put(any())).thenReturn(mockDriveItem);

        when(graphClientConfig.graphUploadClient()).thenReturn(graphClient);

        reportUploadService.uploadCsvToSharePoint(testFile, "reports");

        verify(graphClient, times(2)).sites();
        verify(graphClient, times(2)).drives();
        verify(driveItemRequestBuilder, times(2)).items();
    }

    @Test
    void uploadCsvToSharePoint_fileNotFound() throws Exception {
        File nonExistentFile = new File("/nonexistent/path/file.csv");

        try {
            reportUploadService.uploadCsvToSharePoint(nonExistentFile, "reports");
        } catch (java.io.FileNotFoundException e) {
            // Expected exception, test passes
        }
    }

    @Test
    void uploadCsvToSharePoint_withDifferentFolderPath() {
        Site mockSite = mock(Site.class);
        when(mockSite.getId()).thenReturn("site-789");

        Drive mockDrive = mock(Drive.class);
        when(mockDrive.getId()).thenReturn("drive-101");

        DriveItem mockDriveItem = mock(DriveItem.class);

        SitesRequestBuilder sitesBuilder = mock(SitesRequestBuilder.class);
        SiteItemRequestBuilder siteItemBuilder = mock(SiteItemRequestBuilder.class);
        DriveRequestBuilder driveBuilder = mock(DriveRequestBuilder.class);

        when(graphClient.sites()).thenReturn(sitesBuilder);
        when(sitesBuilder.bySiteId(anyString())).thenReturn(siteItemBuilder);
        when(siteItemBuilder.get()).thenReturn(mockSite);
        when(siteItemBuilder.drive()).thenReturn(driveBuilder);
        when(driveBuilder.get()).thenReturn(mockDrive);

        DrivesRequestBuilder drivesBuilder = mock(DrivesRequestBuilder.class);
        DriveItemRequestBuilder driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
        ItemsRequestBuilder itemsBuilder = mock(ItemsRequestBuilder.class);
        DriveItemItemRequestBuilder driveItemItemBuilder = mock(DriveItemItemRequestBuilder.class);
        com.microsoft.graph.drives.item.items.item.content.ContentRequestBuilder contentBuilder =
            mock(com.microsoft.graph.drives.item.items.item.content.ContentRequestBuilder.class);

        when(graphClient.drives()).thenReturn(drivesBuilder);
        when(drivesBuilder.byDriveId(anyString())).thenReturn(driveItemRequestBuilder);
        when(driveItemRequestBuilder.items()).thenReturn(itemsBuilder);
        when(itemsBuilder.byDriveItemId(anyString())).thenReturn(driveItemItemBuilder);
        when(driveItemItemBuilder.get()).thenThrow(new RuntimeException("404"));
        when(driveItemItemBuilder.content()).thenReturn(contentBuilder);
        when(contentBuilder.put(any())).thenReturn(mockDriveItem);

        when(graphClientConfig.graphUploadClient()).thenReturn(graphClient);

        try {
            reportUploadService.uploadCsvToSharePoint(testFile, "archive/reports");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        verify(graphClient, times(2)).sites();
        verify(graphClient, times(2)).drives();
    }
}
