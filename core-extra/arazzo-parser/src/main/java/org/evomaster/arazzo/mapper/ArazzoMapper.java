package org.evomaster.arazzo.mapper;

import org.evomaster.arazzo.models.domain.ArazzoSpecifications;
import org.evomaster.arazzo.models.domain.Step;
import org.evomaster.arazzo.models.domain.Workflow;
import org.evomaster.arazzo.models.dto.ArazzoSpecificationsDTO;
import org.evomaster.arazzo.models.dto.StepDTO;
import org.evomaster.arazzo.models.dto.WorkflowDTO;
import org.evomaster.arazzo.resolver.ArazzoReferenceResolver;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper class responsible for converting Arazzo Specification (DTOs)
 * into their corresponding domain models.
 */
public class ArazzoMapper {
    private ArazzoReferenceResolver resolver;

    public ArazzoMapper(ArazzoReferenceResolver resolver) {
        this.resolver = resolver;
    }

    public ArazzoSpecifications toDomain(ArazzoSpecificationsDTO arazzoSpecificationsDTO) {
        return ArazzoSpecifications.builder()
                .arazzo(arazzoSpecificationsDTO.getArazzo())
                .info(arazzoSpecificationsDTO.getInfo())
                .sourceDescriptions(arazzoSpecificationsDTO.getSourceDescriptions())
                .workflows(arazzoSpecificationsDTO.getWorkflows().stream()
                        .map(this::toDomain)
                        .collect(Collectors.toList()))
                .components(arazzoSpecificationsDTO.getComponents())
                .build();
    }

    public Workflow toDomain(WorkflowDTO workflowDTO) {
        return Workflow.builder()
                .workflowId(workflowDTO.getWorkflowId())
                .summary(workflowDTO.getSummary())
                .description(workflowDTO.getDescription())
                .inputs(this.toDomain(workflowDTO.getInputs()))
                .dependsOn(workflowDTO.getDependsOn())
                .steps(workflowDTO.getSteps().stream()
                        .map(this::toDomain)
                        .collect(Collectors.toList()))
                .successActions(resolver.resolveSuccessReusable(workflowDTO.getSuccessActions()))
                .failureActions(resolver.resolveFailureReusable(workflowDTO.getFailureActions()))
                .outputs(workflowDTO.getOutputs())
                .parameters(resolver.resolveParametersReusable(workflowDTO.getParameters()))
                .build();
    }

    public Step toDomain(StepDTO stepDTO) {
        return Step.builder()
                .description(stepDTO.getDescription())
                .stepId(stepDTO.getStepId())
                .operationId(stepDTO.getOperationId())
                .operationPath(stepDTO.getOperationPath())
                .workflowId(stepDTO.getWorkflowId())
                .parameters(resolver.resolveParametersReusable(stepDTO.getParameters()))
                .requestBody(stepDTO.getRequestBody())
                .successCriteria(stepDTO.getSuccessCriteria())
                .onSuccess(resolver.resolveSuccessReusable(stepDTO.getOnSuccess()))
                .onFailure(resolver.resolveFailureReusable(stepDTO.getOnFailure()))
                .outputs(stepDTO.getOutputs())
                .build();
    }

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
