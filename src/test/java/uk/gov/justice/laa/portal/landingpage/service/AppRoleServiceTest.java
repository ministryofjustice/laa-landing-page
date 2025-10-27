package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppRoleServiceTest {

    private AppRoleRepository appRoleRepository;
    private ModelMapper modelMapper;
    private AppRoleService appRoleService;

    @BeforeEach
    void setUp() {
        appRoleRepository = mock(AppRoleRepository.class);
        modelMapper = mock(ModelMapper.class);
        appRoleService = new AppRoleService(appRoleRepository, modelMapper);
    }

    @Test
    void testGetByIds_allIdsFound_shouldReturnMappedDtos() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<String> ids = List.of(id1.toString(), id2.toString());

        AppRole role1 = AppRole.builder().id(id1).name("role 1").build();
        AppRole role2 = AppRole.builder().id(id2).name("role 2").build();

        AppRoleDto dto1 = new AppRoleDto();
        AppRoleDto dto2 = new AppRoleDto();

        when(appRoleRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(role1, role2));
        when(modelMapper.map(role1, AppRoleDto.class)).thenReturn(dto1);
        when(modelMapper.map(role2, AppRoleDto.class)).thenReturn(dto2);

        // Act
        List<AppRoleDto> result = appRoleService.getByIds(ids);

        // Assert
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).contains(dto1);
        assertThat(result).contains(dto2);
    }

    @Test
    void testGetByIds_someIdsMissing_shouldThrowException() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<String> ids = List.of(id1.toString(), id2.toString());

        AppRole role1 = AppRole.builder().id(id1).name("role 1").build();

        when(appRoleRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(role1));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                appRoleService.getByIds(ids));

        assertThat(exception.getMessage()).contains("Failed to load all app roles");
    }
}
