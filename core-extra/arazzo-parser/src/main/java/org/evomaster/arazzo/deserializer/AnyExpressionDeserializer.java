package org.evomaster.arazzo.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.evomaster.arazzo.models.domain.AnyExpression;

import java.io.IOException;

/**
 * Custom Jackson deserializer for {@link AnyExpression}.
 * It resolves dynamic JSON payloads by mapping plain text to a {@link AnyExpression.Expression},
 * and any other complex structure to a generic JSON node.
 */
public class AnyExpressionDeserializer extends JsonDeserializer<AnyExpression> {

    @Override
    public AnyExpression deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.isTextual()) {
            return new AnyExpression.Expression(node.asText());
        }
        JsonNode any = jsonParser.getCodec().treeToValue(node, JsonNode.class);
        return new AnyExpression.Any(any);
    }

}
