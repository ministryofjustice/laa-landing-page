package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for {@link DisableType} that treats both {@code NULL} and empty-string {@code ""}
 * database values as {@code null} in Java. This guards against legacy rows that were written with
 * an empty string instead of {@code NULL} before the column was introduced.
 */
@Converter
public class DisableTypeConverter implements AttributeConverter<DisableType, String> {

    @Override
    public String convertToDatabaseColumn(DisableType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public DisableType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return DisableType.valueOf(dbData);
    }
}
