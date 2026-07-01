package org.example.civitaswebapp.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persists a {@code List<String>} as a single delimited column. Used for notification message
 * arguments, which are short, ordered, and few — a delimited column avoids both an extra
 * collection table and the lazy-loading hazards of {@code @ElementCollection} when the entity is
 * serialized outside a Hibernate session (e.g. the JSON dropdown endpoint).
 *
 * <p>The delimiter is the ASCII Unit Separator ({@code U+001F}), which does not occur in human
 * names, event titles, or statuses.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITER = "";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream()
                .map(s -> s == null ? "" : s)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(dbData.split(DELIMITER, -1));
    }
}
