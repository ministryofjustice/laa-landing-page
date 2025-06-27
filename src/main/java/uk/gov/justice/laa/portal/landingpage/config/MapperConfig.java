package uk.gov.justice.laa.portal.landingpage.config;

import com.microsoft.graph.models.User;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true)
                .setAmbiguityIgnored(true)
                .setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.addMappings(graphUserToEntraUserPropertyMap);
        addEntraUserToEntraUserDtoTypeMap(modelMapper);
        return modelMapper;
    }

    public void addEntraUserToEntraUserDtoTypeMap(ModelMapper modelMapper) {
        // Define custom converter
        Converter<EntraUser, String> fullNameConverter = context -> {
            EntraUser source = context.getSource();
            if (source.getFirstName() == null) {
                return null;
            }
            String lastName = source.getLastName() != null ? source.getLastName() : "";
            return String.format("%s %s", source.getFirstName(), lastName).trim();
        };

        // Map Source to Destination
        modelMapper.typeMap(EntraUser.class, EntraUserDto.class).addMappings(mapper ->
                mapper.using(fullNameConverter).map(src -> src, EntraUserDto::setFullName)
        );
    }

    private static final PropertyMap<User, EntraUser> graphUserToEntraUserPropertyMap = new PropertyMap<>() {

        @Override
        protected void configure() {

            // Skip populating ID from graph user as this is auto-generated.
            skip(destination.getId());
            // First name mapping
            using(converter -> {
                String displayName = (String) converter.getSource();
                return getNamePartFromUser(displayName, 0);
            }).map(source.getDisplayName(), destination.getFirstName());

            // Last name mapping
            using(converter -> {
                String displayName = (String) converter.getSource();
                return getNamePartFromUser(displayName, 1);
            }).map(source.getDisplayName(), destination.getLastName());

            // Other non-matching fields mapping
            map().setEntraUserId(source.getId());
            map().setEmail(source.getMail());
        }
    };

    private static String getNamePartFromUser(String displayName, int part) {
        if (displayName != null) {
            String[] names = displayName.split(" ");
            if (part >= 0 && part < names.length) {
                return names[part];
            }
        }
        return "";
    }


}
