package org.evomaster.client.java.controller.neo4j.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jDatabaseCommandsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jEdgeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jNodeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * DSL for Neo4j insertions in generated tests.
 */
public final class Neo4jDsl implements Neo4jSequenceDsl, Neo4jStatementDsl {

    /** The commands built so far, {@code null} once {@link #dtos()} handed them over. */
    private Neo4jDatabaseCommandsDto commands = new Neo4jDatabaseCommandsDto();

    /** Properties of the node or relationship being defined, {@code null} before the first one. */
    private List<Neo4jInsertionEntryDto> current;

    private Neo4jDsl() {
    }

    /**
     * @return a new Neo4j insertion sequence
     */
    public static Neo4jSequenceDsl neo4j() {
        return new Neo4jDsl();
    }

    @Override
    public Neo4jStatementDsl createNode(Long id, String... labels) {
        checkOpen();
        if (id == null) {
            throw new IllegalArgumentException("Unspecified node id");
        }
        Neo4jNodeInsertionDto node = new Neo4jNodeInsertionDto();
        node.id = id;
        if (labels != null) {
            node.labels.addAll(Arrays.asList(labels));
        }
        commands.nodes.add(node);
        current = node.properties;
        return this;
    }

    @Override
    public Neo4jStatementDsl createEdge(String type, Long fromNodeId, Long toNodeId) {
        checkOpen();
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Unspecified relationship type");
        }
        if (fromNodeId == null || toNodeId == null) {
            throw new IllegalArgumentException("Unspecified relationship endpoint");
        }
        Neo4jEdgeInsertionDto edge = new Neo4jEdgeInsertionDto();
        edge.type = type;
        edge.fromNodeId = fromNodeId;
        edge.toNodeId = toNodeId;
        commands.edges.add(edge);
        current = edge.properties;
        return this;
    }

    @Override
    public Neo4jStatementDsl d(String propertyKey, String printableValue) {
        checkOpen();
        if (current == null) {
            throw new IllegalStateException("Call createNode or createEdge before adding properties");
        }
        if (propertyKey == null || propertyKey.isEmpty()) {
            throw new IllegalArgumentException("Unspecified property key");
        }
        if (printableValue == null) {
            throw new IllegalArgumentException("Unspecified property value");
        }
        current.add(toEntry(propertyKey, printableValue));
        return this;
    }

    @Override
    public Neo4jDatabaseCommandsDto dtos() {
        checkOpen();
        Neo4jDatabaseCommandsDto result = commands;
        commands = null;
        current = null;
        return result;
    }

    private Neo4jInsertionEntryDto toEntry(String propertyKey, String printableValue) {
        if (isStringLiteral(printableValue)) {
            String value = printableValue.substring(1, printableValue.length() - 1).replace("''", "'");
            return new Neo4jInsertionEntryDto(propertyKey, Neo4jPropertyTypeDto.STRING, value);
        }
        if ("true".equalsIgnoreCase(printableValue) || "false".equalsIgnoreCase(printableValue)) {
            return new Neo4jInsertionEntryDto(propertyKey, Neo4jPropertyTypeDto.BOOLEAN,
                    Boolean.toString(Boolean.parseBoolean(printableValue)));
        }
        if (isLong(printableValue)) {
            return new Neo4jInsertionEntryDto(propertyKey, Neo4jPropertyTypeDto.INTEGER, printableValue);
        }
        try {
            new BigDecimal(printableValue);
            return new Neo4jInsertionEntryDto(propertyKey, Neo4jPropertyTypeDto.FLOAT, printableValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unsupported Neo4j property value: " + printableValue, e);
        }
    }

    /**
     * @param value value to inspect
     * @return whether the value is a whole number that fits the 64 bits of a Neo4j INTEGER
     */
    private boolean isLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param value value to inspect
     * @return whether the value is enclosed in single quotes
     */
    private boolean isStringLiteral(String value) {
        return value.length() >= 2 && value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'';
    }

    private void checkOpen() {
        if (commands == null) {
            throw new IllegalStateException("DTO was already built for this object");
        }
    }
}
