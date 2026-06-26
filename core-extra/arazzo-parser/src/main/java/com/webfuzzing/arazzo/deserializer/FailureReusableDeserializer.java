package com.webfuzzing.arazzo.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.webfuzzing.arazzo.models.domain.FailureAction;
import com.webfuzzing.arazzo.models.domain.FailureReusable;
import com.webfuzzing.arazzo.models.domain.Reusable;
import org.evomaster.arazzo.models.domain.*;

import java.io.IOException;

/**
 * Custom Jackson deserializer for {@link FailureReusable}.
 * It differentiates the incoming JSON payload based on the presence of the "reference" field,
 * mapping it to a {@link FailureReusable.ReusableObj} if present, or to a {@link FailureReusable.Failure} otherwise.
 */
public class FailureReusableDeserializer extends JsonDeserializer<FailureReusable> {

    @Override
    public FailureReusable deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.has("reference")) {
            Reusable reusable = jsonParser.getCodec().treeToValue(node, Reusable.class);
            return new FailureReusable.ReusableObj(reusable);
        }

        FailureAction action = jsonParser.getCodec().treeToValue(node, FailureAction.class);
        return new FailureReusable.Failure(action);
    }
}
