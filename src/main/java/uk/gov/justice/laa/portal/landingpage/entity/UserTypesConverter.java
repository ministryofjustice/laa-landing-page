package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class UserTypesConverter implements AttributeConverter<Set<UserType>, String> {

    public static final String SEPERATOR = ",";

    @Override
    public String convertToDatabaseColumn(Set<UserType> userTypes) {
        if (userTypes == null || userTypes.isEmpty()) {
            return null;
        }
        return userTypes.stream().map(UserType::toString)
                .collect(Collectors.joining(SEPERATOR));
    }

    @Override
    public Set<UserType> convertToEntityAttribute(String string) {
        if (string == null || string.isEmpty()) {
            return Set.of();
        }

        return Arrays.stream(string.split(SEPERATOR))
                .map(UserType::valueOf).collect(Collectors.toSet());
    }
}
