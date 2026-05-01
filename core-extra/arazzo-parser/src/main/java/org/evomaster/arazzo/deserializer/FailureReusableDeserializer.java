package org.evomaster.arazzo.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.evomaster.arazzo.models.domain.*;

import java.io.IOException;

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
