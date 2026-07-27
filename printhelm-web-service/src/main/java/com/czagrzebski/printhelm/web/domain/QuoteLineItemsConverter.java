package com.czagrzebski.printhelm.web.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class QuoteLineItemsConverter implements AttributeConverter<List<QuoteLineItem>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<QuoteLineItem> items) {
        if (items == null || items.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(items);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<QuoteLineItem> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<List<QuoteLineItem>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
