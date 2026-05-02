package org.evomaster.arazzo.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.evomaster.arazzo.models.domain.CriterionExpression;
import org.evomaster.arazzo.models.domain.CriterionType;

import java.io.IOException;

/**
 * Custom Jackson deserializer for {@link CriterionType}.
 * It resolves dynamic JSON payloads by mapping plain text to a {@link CriterionType.Simple},
 * and a JSON object to a {@link CriterionType.Complex}.
 */
public class CriterionTypeDeserializer extends JsonDeserializer<CriterionType> {

    @Override
    public CriterionType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.isTextual()) {
            return new CriterionType.Simple(node.asText());
        } else if (node.isObject()) {
            CriterionExpression complex = jsonParser.getCodec().treeToValue(node, CriterionExpression.class);
            return new CriterionType.Complex(complex);
        } else {
            throw new IllegalArgumentException("Arazzo Parsing Error: Invalid " + node.getNodeType() + ". Expected string or Criterion Expression");
        }

    }

}
