package com.searchengine.model.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class DocumentTypeDeserializer extends JsonDeserializer<DocumentType> {
    @Override
    public DocumentType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        return DocumentType.fromValue(value);
    }
}
