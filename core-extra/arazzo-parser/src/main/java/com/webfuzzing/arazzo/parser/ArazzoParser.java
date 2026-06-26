package com.webfuzzing.arazzo.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.OpenAPI;
import javafx.util.Pair;
import com.webfuzzing.arazzo.mapper.ArazzoMapper;
import com.webfuzzing.arazzo.models.domain.ArazzoSpecifications;
import com.webfuzzing.arazzo.models.dto.ArazzoSpecificationsDTO;
import com.webfuzzing.arazzo.resolver.ArazzoReferenceResolver;

/**
 * Parse a String containing an Arazzo document into a complete model in ArazzoSpecifications
 */
public class ArazzoParser {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    public static ArazzoSpecifications parse(String schemaText, OpenAPI openAPI) {
        Pair<ArazzoSpecificationsDTO, JsonNode> parsed = parseSchemaText(schemaText);
        ArazzoReferenceResolver resolver = new ArazzoReferenceResolver(parsed.getKey().getComponents(), parsed.getValue(), openAPI);
        ArazzoMapper mapper = new ArazzoMapper(resolver);
        return mapper.toDomain(parsed.getKey());
    }

    private static Pair<ArazzoSpecificationsDTO, JsonNode> parseSchemaText(String schemaText) {
        String schemaTextClean = schemaText.replaceAll("^\\s+", "");

        ArazzoSpecificationsDTO arazzoSpecificationsDTO;
        JsonNode arazzoJsonNode;

        try {
            if (schemaTextClean.startsWith("{")) {
                arazzoSpecificationsDTO = JSON_MAPPER.readValue(schemaTextClean, ArazzoSpecificationsDTO.class);
                arazzoJsonNode = JSON_MAPPER.readTree(schemaTextClean);
            } else {
                arazzoSpecificationsDTO = YAML_MAPPER.readValue(schemaTextClean, ArazzoSpecificationsDTO.class);
                arazzoJsonNode = YAML_MAPPER.readTree(schemaTextClean);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Problems parsing the Arazzo document", e);
        }

        return new Pair<>(arazzoSpecificationsDTO, arazzoJsonNode);
    }
}
