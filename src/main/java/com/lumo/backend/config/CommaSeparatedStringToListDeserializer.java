package com.lumo.backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CommaSeparatedStringToListDeserializer extends JsonDeserializer<ArrayList<String>> {
    @Override
    public ArrayList<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.hasToken(JsonToken.START_ARRAY)) {
            ArrayList<String> list = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                String text = p.getValueAsString();
                if (text != null) {
                    list.add(text.trim());
                }
            }
            return list;
        }

        String value = p.getValueAsString();
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // If it starts with [ and ends with ], it might be a JSON array string sent incorrectly
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .map(s -> s.replaceAll("^\"|\"$", "")) // remove quotes if any
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
