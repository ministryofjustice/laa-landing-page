package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;

import static org.assertj.core.api.Assertions.assertThat;

public class MapperConfigTest {

    MapperConfig mapperConfig = new MapperConfig();

    @Test
    public void testMappingAppRoleDtoToViewModel() {
        // Arrange
        AppDto appDto = new AppDto();
        appDto.setName("TestApp");
        appDto.setOrdinal(5);

        AppRoleDto dto = new AppRoleDto();
        dto.setId("a1r1");
        dto.setName("Admin");
        dto.setDescription("Administrator role");
        dto.setOrdinal(1);
        dto.setApp(appDto);

        // Act
        AppRoleViewModel viewModel = mapperConfig.modelMapper().map(dto, AppRoleViewModel.class);

        // Assert
        assertThat(dto.getId()).isEqualTo(viewModel.getId());
        assertThat(dto.getName()).isEqualTo(viewModel.getName());
        assertThat(dto.getDescription()).isEqualTo(viewModel.getDescription());
        assertThat(dto.getOrdinal()).isEqualTo(viewModel.getOrdinal());
        assertThat(appDto.getName()).isEqualTo(viewModel.getAppName());
        assertThat(appDto.getOrdinal()).isEqualTo(viewModel.getAppOrdinal());
    }

}
