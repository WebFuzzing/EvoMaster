package com.webfuzzing.arazzo.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.OpenAPI;
import com.webfuzzing.arazzo.mapper.ArazzoMapper;
import com.webfuzzing.arazzo.models.domain.ArazzoSpecifications;
import com.webfuzzing.arazzo.models.unresolved.UnresolvedArazzoSpecifications;
import com.webfuzzing.arazzo.resolver.ArazzoReferenceResolver;
import java.util.AbstractMap;

/**
 * Parse a String containing an Arazzo document into a complete model in ArazzoSpecifications
 */
public class ArazzoParser {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    /**
     * Parses an Arazzo document along with the corresponding OpenAPI document and returns an instance of ArazzoSpecifications.
     */
    public static ArazzoSpecifications parse(String schemaText, OpenAPI openAPI) {
        AbstractMap.SimpleEntry<UnresolvedArazzoSpecifications, JsonNode> parsed = parseSchemaText(schemaText);
        ArazzoReferenceResolver resolver = new ArazzoReferenceResolver(parsed.getKey().getComponents(), parsed.getValue(), openAPI);
        ArazzoMapper mapper = new ArazzoMapper(resolver);
        return mapper.toDomain(parsed.getKey());
    }

    private static AbstractMap.SimpleEntry<UnresolvedArazzoSpecifications, JsonNode> parseSchemaText(String schemaText) {
        UnresolvedArazzoSpecifications unresolvedArazzoSpecifications;
        JsonNode arazzoJsonNode;

        try {
            unresolvedArazzoSpecifications = JSON_MAPPER.readValue(schemaText, UnresolvedArazzoSpecifications.class);
            arazzoJsonNode = JSON_MAPPER.readTree(schemaText);
        } catch (JsonProcessingException jsonException) {
            try {
                unresolvedArazzoSpecifications = YAML_MAPPER.readValue(schemaText, UnresolvedArazzoSpecifications.class);
                arazzoJsonNode = YAML_MAPPER.readTree(schemaText);
            } catch (Exception yamlException) {
                throw new IllegalArgumentException("Problems parsing the Arazzo document", yamlException);
            }
        }

        return new AbstractMap.SimpleEntry<>(unresolvedArazzoSpecifications, arazzoJsonNode);
    }
}
