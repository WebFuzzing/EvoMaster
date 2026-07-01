package com.webfuzzing.arazzo.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.webfuzzing.arazzo.models.domain.*;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolver class responsible for resolving Arazzo references.
 */
public class ArazzoReferenceResolver {
    private final String COMPONENTS_PREFIX = "$components.";
    private final String SUCCESS_ACTIONS = "successActions";
    private final String FAILURE_ACTIONS = "failureActions";
    private final String PARAMETERS = "parameters";
    private final String OPENAPI_SCHEMAS_PREFIX = "/components/schemas/";

    private Components components;
    private JsonNode arazzoJsonNode;
    private OpenAPI openApi;

    public ArazzoReferenceResolver(Components components, JsonNode arazzoJsonNode, OpenAPI openAPI) {
        this.components = components;
        this.arazzoJsonNode = arazzoJsonNode;
        this.openApi = openAPI;
    }

    public void setComponents(Components components) {
        this.components = components;
    }

    public void setArazzoJsonNode(JsonNode arazzoJsonNode) {
        this.arazzoJsonNode = arazzoJsonNode;
    }

    public void setOpenApi(OpenAPI openApi) {
        this.openApi = openApi;
    }

    /**
     * Resolve the {@link SuccessReusable} references to get a complete list of {@link SuccessAction}
     */
    public List<SuccessAction> resolveSuccessReusable(List<SuccessReusable> items) {
        if (items == null) {
            return null;
        }

        return items.stream().map(item -> {
            if (item instanceof SuccessReusable.Success) {
                SuccessReusable.Success action = (SuccessReusable.Success) item;
                return action.getAction();
            }
            SuccessReusable.ReusableObj reusableObj = (SuccessReusable.ReusableObj) item;
            return (SuccessAction) resolveReusableWithPrefix(reusableObj.getReusable(), this.SUCCESS_ACTIONS);
        }).collect(Collectors.toList());
    }

    /**
     * Resolve the {@link FailureReusable} references to get a complete list of {@link FailureAction}
     */
    public List<FailureAction> resolveFailureReusable(List<FailureReusable> items) {
        if (items == null) {
            return null;
        }

        return items.stream().map(item -> {
            if (item instanceof FailureReusable.Failure) {
                FailureReusable.Failure action = (FailureReusable.Failure) item;
                return action.getAction();
            }
            FailureReusable.ReusableObj reusableObj = (FailureReusable.ReusableObj) item;
            return (FailureAction) resolveReusableWithPrefix(reusableObj.getReusable(), this.FAILURE_ACTIONS);
        }).collect(Collectors.toList());
    }

    /**
     * Resolve the {@link ParameterReusable} references to get a complete list of {@link Parameter}
     */
    public List<Parameter> resolveParametersReusable(List<ParameterReusable> items) {
        if (items == null) {
            return null;
        }

        return items.stream().map(item -> {
            if (item instanceof ParameterReusable.Param) {
                ParameterReusable.Param param = (ParameterReusable.Param) item;
                return param.getParameter();
            }
            ParameterReusable.ReusableObj reusableObj = (ParameterReusable.ReusableObj) item;
            return (Parameter) resolveReusableWithPrefix(reusableObj.getReusable(), this.PARAMETERS);
        }).collect(Collectors.toList());
    }

    private Object resolveReusableWithPrefix(Reusable reusable, String expectedPrefix) {
        if (components == null) {
            throw new IllegalArgumentException("Arazzo Parsing Error: Can't reference with no Components");
        }

        String reference = reusable.getReference();
        if (!reference.startsWith(this.COMPONENTS_PREFIX)) {
            throw new IllegalArgumentException("Arazzo Parsing Error: Invalid reference (" + reference + "). Expected to point to '" + this.COMPONENTS_PREFIX + "'");
        }

        String referenceWithoutComponents = reference.substring(this.COMPONENTS_PREFIX.length());
        if (!referenceWithoutComponents.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("Arazzo Parsing Error: Invalid reference (" + referenceWithoutComponents + "). Expected to point to '" + expectedPrefix +"'");
        }

        String referenceClean = referenceWithoutComponents.substring((expectedPrefix + ".").length());
        Object resolve;
        switch (expectedPrefix) {
            case SUCCESS_ACTIONS:
                resolve = (components.getSuccessAction() != null) ? components.getSuccessAction().get(referenceClean) : null;
                break;
            case FAILURE_ACTIONS:
                resolve = (components.getFailureAction() != null) ? components.getFailureAction().get(referenceClean) : null;
                break;
            case PARAMETERS:
                resolve = (components.getParameters() != null) ? components.getParameters().get(referenceClean) : null;
                break;
            default:
                resolve = null;
                break;
        }

        if (resolve == null) {
            throw new IllegalArgumentException("Arazzo Parsing Error: The " + expectedPrefix + ": '" + referenceClean + "' is not in the components.");
        }

        return resolve;

    }

    public Schema<?> resolveJsonPointer(String reference) {
        if (reference.startsWith("#/")) {
            return resolveJsonPointerLocal(reference);
        }
        return resolveJsonPointerExternal(reference);
    }

    private Schema<?> resolveJsonPointerLocal(String reference) {
        if (arazzoJsonNode == null) {
            throw new IllegalArgumentException("Arazzo Parsing Error: Can't reference with no Arazzo Document");
        }

        String jsonPointer = reference.substring(1);

        JsonNode result = arazzoJsonNode.at(jsonPointer);

        if (result.isMissingNode()) {
            throw new IllegalArgumentException("Arazzo Parsing Error: Can't reference '" + reference + "'");
        }

        return Json.mapper().convertValue(result, Schema.class);
    }

    private Schema<?> resolveJsonPointerExternal(String reference) {
        if (openApi == null) {
            throw new IllegalArgumentException("Arazzo Parsing Error: Can't reference with no OpenApi Document");
        }

        String[] tokens = reference.split("#");
        if (tokens.length < 2) {
            throw new IllegalArgumentException("Arazzo Parsing Error: Error reference (" + reference + "). '#' Is mandatory");
        }

        String jsonPointer = tokens[1];
        if (!jsonPointer.startsWith(this.OPENAPI_SCHEMAS_PREFIX)) {
            throw new IllegalArgumentException("Arazzo Parsing Error: Error reference (" + reference + "). \"" + this.OPENAPI_SCHEMAS_PREFIX + "\" Is mandatory for references to OpenApi");
        }

        String schemaName = jsonPointer.substring(this.OPENAPI_SCHEMAS_PREFIX.length());
        Schema<?> result = null;
        if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
            result = openApi.getComponents().getSchemas().get(schemaName);
        }

        if (result == null) {
            throw new IllegalArgumentException("Arazzo Parsing Error: (" + reference + ") reference does not exist in the OpenApi document");
        }

        return result;
    }

}
