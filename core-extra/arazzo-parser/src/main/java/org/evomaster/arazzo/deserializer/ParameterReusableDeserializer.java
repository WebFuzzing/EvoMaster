package org.evomaster.arazzo.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.evomaster.arazzo.models.domain.*;

import java.io.IOException;

/**
 * Custom Jackson deserializer for {@link ParameterReusable}.
 * It differentiates the incoming JSON payload based on the presence of the "reference" field,
 * mapping it to a {@link ParameterReusable.ReusableObj} if present, or to a {@link ParameterReusable.Param} otherwise.
 */
public class ParameterReusableDeserializer extends JsonDeserializer<ParameterReusable> {

    @Override
    public ParameterReusable deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.has("reference")) {
            Reusable reusable = jsonParser.getCodec().treeToValue(node, Reusable.class);
            return new ParameterReusable.ReusableObj(reusable);
        }

        Parameter parameter = jsonParser.getCodec().treeToValue(node, Parameter.class);
        return new ParameterReusable.Param(parameter);
    }

}
