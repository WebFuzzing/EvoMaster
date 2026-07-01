package com.webfuzzing.arazzo.mapper;

import com.webfuzzing.arazzo.models.domain.ArazzoSpecifications;
import com.webfuzzing.arazzo.models.domain.Step;
import com.webfuzzing.arazzo.models.domain.Workflow;
import com.webfuzzing.arazzo.models.unresolved.UnresolvedArazzoSpecifications;
import com.webfuzzing.arazzo.models.unresolved.UnresolvedStep;
import com.webfuzzing.arazzo.models.unresolved.UnresolvedWorkflow;
import com.webfuzzing.arazzo.resolver.ArazzoReferenceResolver;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper class responsible for converting unresolved Arazzo Specification models
 * into their corresponding domain models.
 */
public class ArazzoMapper {
    private ArazzoReferenceResolver resolver;

    public ArazzoMapper(ArazzoReferenceResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Maps UnresolvedArazzoSpecifications to ArazzoSpecifications
     */
    public ArazzoSpecifications toDomain(UnresolvedArazzoSpecifications unresolvedArazzoSpecifications) {
        return ArazzoSpecifications.builder()
                .arazzo(unresolvedArazzoSpecifications.getArazzo())
                .info(unresolvedArazzoSpecifications.getInfo())
                .sourceDescriptions(unresolvedArazzoSpecifications.getSourceDescriptions())
                .workflows(unresolvedArazzoSpecifications.getWorkflows().stream()
                        .map(this::toDomain)
                        .collect(Collectors.toList()))
                .components(unresolvedArazzoSpecifications.getComponents())
                .build();
    }

    /**
     * Maps UnresolvedWorkflow to Workflow
     */
    public Workflow toDomain(UnresolvedWorkflow unresolvedWorkflow) {
        return Workflow.builder()
                .workflowId(unresolvedWorkflow.getWorkflowId())
                .summary(unresolvedWorkflow.getSummary())
                .description(unresolvedWorkflow.getDescription())
                .inputs(this.toDomain(unresolvedWorkflow.getInputs()))
                .dependsOn(unresolvedWorkflow.getDependsOn())
                .steps(unresolvedWorkflow.getSteps().stream()
                        .map(this::toDomain)
                        .collect(Collectors.toList()))
                .successActions(resolver.resolveSuccessReusable(unresolvedWorkflow.getSuccessActions()))
                .failureActions(resolver.resolveFailureReusable(unresolvedWorkflow.getFailureActions()))
                .outputs(unresolvedWorkflow.getOutputs())
                .parameters(resolver.resolveParametersReusable(unresolvedWorkflow.getParameters()))
                .build();
    }

    /**
     * Maps UnresolvedStep to Step
     */
    public Step toDomain(UnresolvedStep unresolvedStep) {
        return Step.builder()
                .description(unresolvedStep.getDescription())
                .stepId(unresolvedStep.getStepId())
                .operationId(unresolvedStep.getOperationId())
                .operationPath(unresolvedStep.getOperationPath())
                .workflowId(unresolvedStep.getWorkflowId())
                .parameters(resolver.resolveParametersReusable(unresolvedStep.getParameters()))
                .requestBody(unresolvedStep.getRequestBody())
                .successCriteria(unresolvedStep.getSuccessCriteria())
                .onSuccess(resolver.resolveSuccessReusable(unresolvedStep.getOnSuccess()))
                .onFailure(resolver.resolveFailureReusable(unresolvedStep.getOnFailure()))
                .outputs(unresolvedStep.getOutputs())
                .build();
    }

    /**
     * Map Schema to Schema with reference resolved
     */
    public Schema<?> toDomain(Schema<?> schema) {
        if (schema != null && schema.get$ref() != null && !schema.get$ref().trim().isEmpty()) {
            Schema<?> reference = toDomain(resolver.resolveJsonPointer(schema.get$ref()));
            if (reference != null) {
                if (reference.getProperties() != null) {
                    Map<String, Schema> updatedProperties = reference.getProperties().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey, entry -> toDomain(entry.getValue())
                            ));

                    reference.setProperties(updatedProperties);
                }

                return reference;
            }
        }

        return schema;
    }
}
