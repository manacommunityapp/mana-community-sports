package com.manacommunity.sports.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SportsFormatDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            String text = p.getText();
            return (text == null || text.isBlank()) ? null : text.trim();
        }
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            List<String> items = new ArrayList<>();
            for (JsonNode item : node) {
                if (item != null && !item.isNull()) {
                    String text = item.asText();
                    if (text != null && !text.isBlank()) {
                        items.add(text.trim());
                    }
                }
            }
            return items.isEmpty() ? null : String.join(",", items);
        }
        String text = node.asText();
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
