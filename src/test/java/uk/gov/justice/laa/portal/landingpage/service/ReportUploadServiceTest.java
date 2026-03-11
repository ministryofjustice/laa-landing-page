//package uk.gov.justice.laa.portal.landingpage.service;
//
//import com.microsoft.graph.drives.DrivesRequestBuilder;
//import com.microsoft.graph.drives.item.DriveItemRequestBuilder;
//import com.microsoft.graph.drives.item.items.DriveItemsRequestBuilder;
//import com.microsoft.graph.drives.item.items.item.DriveItemItemRequestBuilder;
//import com.microsoft.graph.drives.item.items.item.content.DriveItemContentRequestBuilder;
//import com.microsoft.graph.models.Drive;
//import com.microsoft.graph.models.DriveItem;
//import com.microsoft.graph.models.Site;
//import com.microsoft.graph.serviceclient.GraphServiceClient;
//import com.microsoft.graph.sites.SitesRequestBuilder;
//import com.microsoft.graph.sites.item.SiteItemRequestBuilder;
//import com.microsoft.graph.sites.item.drive.DriveRequestBuilder;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import uk.gov.justice.laa.portal.landingpage.config.GraphClientConfig;
//
//import java.io.File;
//import java.io.InputStream;
//import java.lang.reflect.Field;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.times;
//
//@ExtendWith(MockitoExtension.class)
//class ReportUploadServiceTest {
//
//    @Mock
//    private GraphClientConfig graphClientConfig;
//
//    @Mock
//    private GraphServiceClient graphClient;
//
//    @Mock
//    private Site site;
//
//    @Mock
//    private Drive drive;
//
//    @Mock
//    private DriveItem driveItem;
//
//    @InjectMocks
//    private ReportUploadService reportUploadService;
//
//    @BeforeEach
//    void setup() throws Exception {
//        // Inject private field @Value via reflection
//        Field sharepointField = ReportUploadService.class.getDeclaredField("SHAREPOINT_URL");
//        sharepointField.setAccessible(true);
//        sharepointField.set(reportUploadService, "test-site-id");
//    }
//
//    @Test
//    void uploadCsvToSharePoint_success() throws Exception {
//        // Arrange
//        File tempFile = File.createTempFile("test", ".csv");
//
//        // Mock graph client
//        when(graphClientConfig.graphUploadClient()).thenReturn(graphClient);
//
//        // ========== MOCK CHAIN: sites().bySiteId().get() ==========
//        SitesRequestBuilder sitesRequestBuilder = mock(SitesRequestBuilder.class);
//        SiteItemRequestBuilder siteItemRequestBuilder = mock(SiteItemRequestBuilder.class);
//
//        when(graphClient.sites()).thenReturn(sitesRequestBuilder);
//        when(sitesRequestBuilder.bySiteId(anyString())).thenReturn(siteItemRequestBuilder);
//        when(siteItemRequestBuilder.get()).thenReturn(site);
//        when(site.getId()).thenReturn("mock-site-id");
//
//        // ========== MOCK CHAIN: sites().bySiteId().drive().get() ==========
//        DriveRequestBuilder driveRequestBuilder = mock(DriveRequestBuilder.class);
//
//        when(siteItemRequestBuilder.drive()).thenReturn(driveRequestBuilder);
//        when(driveRequestBuilder.get()).thenReturn(drive);
//        when(drive.getId()).thenReturn("mock-drive-id");
//
//        // ========== MOCK CHAIN: drives().byDriveId()...content().put() ==========
//        DrivesRequestBuilder drivesRequestBuilder = mock(DrivesRequestBuilder.class);
//        DriveItemRequestBuilder driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
//        DriveItemsRequestBuilder driveItemsRequestBuilder = mock(DriveItemsRequestBuilder.class);
//        DriveItemItemRequestBuilder driveItemItemRequestBuilder = mock(DriveItemItemRequestBuilder.class);
//        DriveItemContentRequestBuilder contentRequestBuilder = mock(DriveItemContentRequestBuilder.class);
//
//        when(graphClient.drives()).thenReturn(drivesRequestBuilder);
//        when(drivesRequestBuilder.byDriveId(anyString())).thenReturn(driveItemRequestBuilder);
//        when(driveItemRequestBuilder.items()).thenReturn(driveItemsRequestBuilder);
//        when(driveItemsRequestBuilder.byDriveItemId(anyString())).thenReturn(driveItemItemRequestBuilder);
//        when(driveItemItemRequestBuilder.content()).thenReturn(contentRequestBuilder);
//        when(contentRequestBuilder.put(any(InputStream.class))).thenReturn(driveItem);
//
//        // Act
//        reportUploadService.uploadCsvToSharePoint(tempFile, "folderA");
//
//        // Assert
//        verify(contentRequestBuilder, times(1)).put(any(InputStream.class));
//    }
//
//    @Test
//    void uploadCsvToSharePoint_failure() throws Exception {
//        // Arrange
//        File tempFile = File.createTempFile("test", ".csv");
//
//        when(graphClientConfig.graphUploadClient()).thenReturn(graphClient);
//
//        SitesRequestBuilder sitesRequestBuilder = mock(SitesRequestBuilder.class);
//        SiteItemRequestBuilder siteItemRequestBuilder = mock(SiteItemRequestBuilder.class);
//
//        // site + drive mocks
//        when(graphClient.sites()).thenReturn(sitesRequestBuilder);
//        when(sitesRequestBuilder.bySiteId(anyString())).thenReturn(siteItemRequestBuilder);
//        when(siteItemRequestBuilder.get()).thenReturn(site);
//        when(site.getId()).thenReturn("mock-site-id");
//
//        DriveRequestBuilder driveRequestBuilder = mock(DriveRequestBuilder.class);
//        when(siteItemRequestBuilder.drive()).thenReturn(driveRequestBuilder);
//        when(driveRequestBuilder.get()).thenReturn(drive);
//        when(drive.getId()).thenReturn("mock-drive-id");
//
//        // Upload returns null → failure path
//        DrivesRequestBuilder drivesRequestBuilder = mock(DrivesRequestBuilder.class);
//        DriveItemRequestBuilder driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
//        DriveItemsRequestBuilder driveItemsRequestBuilder = mock(DriveItemsRequestBuilder.class);
//        DriveItemItemRequestBuilder driveItemItemRequestBuilder = mock(DriveItemItemRequestBuilder.class);
//        DriveItemContentRequestBuilder contentRequestBuilder = mock(DriveItemContentRequestBuilder.class);
//
//        when(graphClient.drives()).thenReturn(drivesRequestBuilder);
//        when(drivesRequestBuilder.byDriveId(anyString())).thenReturn(driveItemRequestBuilder);
//        when(driveItemRequestBuilder.items()).thenReturn(driveItemsRequestBuilder);
//        when(driveItemsRequestBuilder.byDriveItemId(anyString())).thenReturn(driveItemItemRequestBuilder);
//        when(driveItemItemRequestBuilder.content()).thenReturn(contentRequestBuilder);
//        when(contentRequestBuilder.put(any(InputStream.class))).thenReturn(null);
//
//        // Act
//        reportUploadService.uploadCsvToSharePoint(tempFile, "folderA");
//
//        // Assert
//        verify(contentRequestBuilder, times(1)).put(any(InputStream.class));
//    }
//}
