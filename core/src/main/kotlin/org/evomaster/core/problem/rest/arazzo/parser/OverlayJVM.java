package com.webfuzzing.overlayjvm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.webfuzzing.overlayjvm.model.Action;
import com.webfuzzing.overlayjvm.model.Overlay;
import org.noear.snack4.ONode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main entry point of this library.
 */
public class OverlayJVM {


    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    static {
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }


    public static Overlay parseOverlay(File file){
        try {
            return mapper.readValue(file, Overlay.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Overlay parseOverlay(String overlayContent){
        try {
            return mapper.readValue(overlayContent, Overlay.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /**
     * Apply an Overlay set of transformations to an OpenAPI schema.
     * These file representations are passed as String, representing either JSON or YAML formats.
     * The transformed result format depends on the input OpenAPI format.
     * For example, if OpenAPI is in YAML, and Overlay is in JSON, the transformed result is in YAML.
     * <p>
     * There is no validity check on the input OpenAPI schema, besides it being a syntactically valid
     * JSON or YAML file representation.
     * So, technically speaking, the Overlay transformation can be applied to any valid JSON/YAML file.
     *
     * @param openApiSchema String representation for a JSON/YAML file, in which transformations are applied on.
     * @param overlayContent String representation for a JSON/YAML Overlay set of transformations.
     * @return TransformationResult with the result of the transformation, including possible warning messages, if any.
     * @throws IllegalArgumentException if the inputs are not valid JSON/YAML files, and if the Overlay is not correct
     *         according the Overlay's specs. No validation is done on the OpenAPI specs.
     */
    public static TransformationResult applyOverlay(String openApiSchema, String overlayContent){

        List<String> warnings = new ArrayList<>();

        Overlay overlay = parseOverlay(overlayContent);
        OpenAPIInfo schemaInfo = OpenAPIInfo.fromSchema(openApiSchema);
        Format format = schemaInfo.getType();

        if(format != Format.JSON && format != Format.YAML) {
            //shouldn't really happen, unless we add new type and forgot to handle it
            throw new IllegalArgumentException("Unsupported type: " + schemaInfo.getType());
        }

        ONode schema = ONode.ofJson(schemaInfo.getJson());
        List<Action> actions = overlay.getActions();

        for(int i=0;i<actions.size();i++) {
            Action a = actions.get(i);

            /*
                From specs: "If the target JSONPath expression selects zero nodes,
                the action succeeds without changing the target document."
            */
            if(!schema.exists(a.getTarget())){
                warnings.add("["+i+"] Target RFC 9535 JsonPath returned no nodes in the schema: " + a.getTarget());
                continue;
            }

            //TODO should check if invalid

            if(a.isRemoveAction()){
                handleRemove(schema, a);
            } else if(a.isUpdateAction()){
                handleUpdate(schema, a);
            } else if(a.isCopyAction()){
                if(! schema.exists(a.getCopy())){
                    warnings.add("["+i+"] RFC 9535 JsonPath 'copy' element returned no nodes in the schema: " + a.getCopy());
                    continue;
                }
                handleCopy(schema, a);
            } else {
                throw new IllegalArgumentException("Invalid action definition: " + a);
            }
        }

        //give back result with same format as in input
        String result = schema.toJson();

        if(schemaInfo.getType() == Format.YAML) {
            result = FormatUtils.convertFromJsonToYaml(result);
        }

        return new TransformationResult(openApiSchema,overlayContent,result,warnings);
    }

    private static void handleCopy(ONode openApi, Action a) {

        ONode target = openApi.select(a.getTarget());
        ONode copy = openApi.select(a.getCopy());

        applyUpdate(target, copy);
    }

    private static void mergeObjects(ONode x, ONode y) {
        if (!x.isObject()) {
            throw new IllegalArgumentException("Invalid object definition for x: " + x);
        }
        if (!y.isObject()) {
            throw new IllegalArgumentException("Invalid object definition for y: " + y);
        }

        for (Map.Entry<String, ONode> k : y.getObjectUnsafe().entrySet()) {
            if (!x.hasKey(k.getKey())) {
                //whole insertion, as entry was not present
                x.set(k.getKey(), k.getValue());
            } else {
                //need to merge, recursively
                if(k.getValue().isObject()) {
                    mergeObjects(x.get(k.getKey()), k.getValue());
                } else if(k.getValue().isValue()){
                    x.set(k.getKey(), k.getValue());
                } else if(k.getValue().isArray()){
                    x.get(k.getKey()).addAll(k.getValue().getArrayUnsafe());
                } else {
                    throw new IllegalArgumentException("Invalid type for: " + k.getKey());
                }
            }
        }
    }

    private static void handleUpdate(ONode openApi, Action a) {
        ONode selection = openApi.select(a.getTarget());
        ONode update = ONode.ofJson(a.getUpdate().toString());
        applyUpdate(selection, update);
    }

    private static void applyUpdate(ONode selection, ONode update) {
        if(selection.isValue()){
            selection.setValue(update.getValue());
            return;
        }

        if(selection.isObject()){
            mergeObjects(selection, update);
            return;
        }

        if(selection.isArray()) {
            /*
                currently, in snackjson there is no clear way to distinguish between a selected array
                and an array of results, apart from checking the parent
             */
            ONode parent = selection.parent();
            if(parent == null) {
                // newly created array containing result nodes
                for (ONode node : selection.getArray()) {
                    applyUpdate(node, update);
                }
            } else {
                //the target is an array itself
                if(update.isArray()) {
                    selection.addAll(update.getArrayUnsafe());
                } else {
                    selection.add(update);
                }
            }
        }
    }

    private static void handleRemove(ONode openApi, Action a) {
        boolean deleted = openApi.delete(a.getTarget());
        assert(deleted);
    }
}
