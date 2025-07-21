package uk.gov.justice.laa.portal.landingpage.entity;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserTypesConverterTest {

    @Test
    public void testConvertToDatabaseColumnNoUserTypes() {
        UserTypesConverter converter = new UserTypesConverter();

        String result = converter.convertToDatabaseColumn(null);
        Assertions.assertThat(result).isNull();

        result = converter.convertToDatabaseColumn(Collections.emptySet());
        Assertions.assertThat(result).isNull();
    }

    @Test
    public void testConvertToDatabaseColumnWithUserTypes() {
        UserTypesConverter converter = new UserTypesConverter();

        String result = converter.convertToDatabaseColumn(Set.of(UserType.INTERNAL));
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).isEqualTo("INTERNAL");

        result = converter.convertToDatabaseColumn(Set.of(UserType.INTERNAL, UserType.EXTERNAL_SINGLE_FIRM));
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).isEqualTo("INTERNAL,EXTERNAL_SINGLE_FIRM");
    }

    @Test
    public void testConvertToEntityAttributeNoUserTypes() {
        UserTypesConverter converter = new UserTypesConverter();

        Set<UserType> result = converter.convertToEntityAttribute(null);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).isEmpty();

        result = converter.convertToEntityAttribute("");
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void testConvertToEntityAttributeWithUserTypes() {
        UserTypesConverter converter = new UserTypesConverter();

        Set<UserType> result = converter.convertToEntityAttribute("INTERNAL");
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.stream().findFirst().get()).isEqualTo(UserType.INTERNAL);

        result = converter.convertToEntityAttribute("INTERNAL,EXTERNAL_SINGLE_FIRM");
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).containsAll(Set.of(UserType.INTERNAL, UserType.EXTERNAL_SINGLE_FIRM));
    }

    @Test
    public void testConvertToEntityAttributeIncompatibleUserTypes() {
        UserTypesConverter converter = new UserTypesConverter();

        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("INVALID"));
    }

}
