package org.evomaster.arazzo.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.evomaster.arazzo.models.domain.Reusable;
import org.evomaster.arazzo.models.domain.SuccessAction;
import org.evomaster.arazzo.models.domain.SuccessReusable;

import java.io.IOException;

public class SuccessReusableDeserializer extends JsonDeserializer<SuccessReusable> {

    @Override
    public SuccessReusable deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.has("reference")) {
            Reusable reusable = jsonParser.getCodec().treeToValue(node, Reusable.class);
            return new SuccessReusable.ReusableObj(reusable);
        }

        SuccessAction action = jsonParser.getCodec().treeToValue(node, SuccessAction.class);
        return new SuccessReusable.Success(action);
    }

}
