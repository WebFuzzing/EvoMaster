package org.evomaster.client.java.controller.neo4j;

import org.evomaster.client.java.controller.api.dto.database.execution.Neo4jFailedQuery;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jEdgeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jNodeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto;
import org.evomaster.client.java.controller.neo4j.conditions.AndCondition;
import org.evomaster.client.java.controller.neo4j.conditions.AnyLabelCondition;
import org.evomaster.client.java.controller.neo4j.conditions.ComparisonCondition;
import org.evomaster.client.java.controller.neo4j.conditions.CypherCondition;
import org.evomaster.client.java.controller.neo4j.conditions.CypherConditionVisitor;
import org.evomaster.client.java.controller.neo4j.conditions.LabelCondition;
import org.evomaster.client.java.controller.neo4j.conditions.LiteralOperand;
import org.evomaster.client.java.controller.neo4j.conditions.NotCondition;
import org.evomaster.client.java.controller.neo4j.conditions.Operand;
import org.evomaster.client.java.controller.neo4j.conditions.OrCondition;
import org.evomaster.client.java.controller.neo4j.conditions.ParameterOperand;
import org.evomaster.client.java.controller.neo4j.conditions.PropertyCondition;
import org.evomaster.client.java.controller.neo4j.conditions.PropertyOperand;
import org.evomaster.client.java.controller.neo4j.conditions.RawCondition;
import org.evomaster.client.java.controller.neo4j.conditions.TypeCondition;
import org.evomaster.client.java.controller.neo4j.conditions.XorCondition;
import org.evomaster.client.java.controller.neo4j.heuristics.Neo4jPatternExpander;
import org.evomaster.client.java.controller.neo4j.operations.MatchOperation;
import org.evomaster.client.java.controller.neo4j.operations.PatternEdge;
import org.evomaster.client.java.controller.neo4j.operations.PatternNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a MATCH query that found nothing into the smallest insertion that would make it match: one
 * node per node of the pattern with its labels, one relationship per edge with its type, and a
 * property for each condition comparing a property of one of them against a literal or a resolved
 * parameter. The value compared against is the seed of the property; for a strict inequality it lies
 * on the boundary, and the search moves it from there.
 */
public class Neo4jInsertionTemplateBuilder {

    public Neo4jInsertionTemplateBuilder() {
    }

    /**
     * @param query the parsed query
     * @param parameters the values captured with the query, by name without the leading {@code $}
     * @param cypher the text of the query, kept in the result for reporting
     * @return the insertion that would satisfy the query, or {@code null} if the pattern cannot be
     *         created as it is
     */
    public static Neo4jFailedQuery build(MatchOperation query, Map<String, Object> parameters, String cypher) {
        Neo4jPatternExpander.ExpandedQuery expanded = new Neo4jPatternExpander().expand(query);

        Map<String, Neo4jNodeInsertionDto> nodesByVariable = new LinkedHashMap<>();
        for (PatternNode node : expanded.pattern.getNodes()) {
            Neo4jNodeInsertionDto dto = new Neo4jNodeInsertionDto();
            dto.id = (long) nodesByVariable.size();
            nodesByVariable.put(node.getVariableName(), dto);
        }

        Map<String, Neo4jEdgeInsertionDto> edgesByVariable = new LinkedHashMap<>();
        List<Neo4jEdgeInsertionDto> edges = new ArrayList<>();
        for (PatternEdge edge : expanded.pattern.getEdges()) {
            Neo4jNodeInsertionDto from = nodesByVariable.get(edge.getSourceVariable());
            Neo4jNodeInsertionDto to = nodesByVariable.get(edge.getTargetVariable());
            if (from == null || to == null) {
                return null;
            }
            Neo4jEdgeInsertionDto dto = new Neo4jEdgeInsertionDto();
            dto.fromNodeId = from.id;
            dto.toNodeId = to.id;
            edges.add(dto);
            if (edge.getVariableName() != null) {
                edgesByVariable.put(edge.getVariableName(), dto);
            }
        }

        Collector collector = new Collector(nodesByVariable, edgesByVariable, parameters);
        for (CypherCondition condition : expanded.conditions) {
            condition.accept(collector);
        }

        for (Neo4jEdgeInsertionDto edge : edges) {
            if (edge.type == null) {
                return null;
            }
        }

        return new Neo4jFailedQuery(cypher, new ArrayList<>(nodesByVariable.values()), edges);
    }

    /**
     * Walks the conditions and writes what each one pins down into the node or relationship it names.
     * The first label, type or property value found for an element wins over later ones.
     */
    private static final class Collector implements CypherConditionVisitor<Void> {

        /** The nodes of the insertion, by the variable that names them in the query. */
        private final Map<String, Neo4jNodeInsertionDto> nodes;

        /** The relationships of the insertion, by the variable that names them in the query. */
        private final Map<String, Neo4jEdgeInsertionDto> edges;

        /** The values captured with the query, by parameter name. */
        private final Map<String, Object> parameters;

        private Collector(Map<String, Neo4jNodeInsertionDto> nodes, Map<String, Neo4jEdgeInsertionDto> edges,
                  Map<String, Object> parameters) {
            this.nodes = nodes;
            this.edges = edges;
            this.parameters = parameters;
        }

        @Override
        public Void visitLabel(LabelCondition condition) {
            Neo4jNodeInsertionDto node = nodes.get(condition.getVariableName());
            if (node != null && !node.labels.contains(condition.getLabel())) {
                node.labels.add(condition.getLabel());
            }
            return null;
        }

        @Override
        public Void visitAnyLabel(AnyLabelCondition condition) {
            return null;
        }

        @Override
        public Void visitType(TypeCondition condition) {
            Neo4jEdgeInsertionDto edge = edges.get(condition.getVariableName());
            if (edge != null && edge.type == null) {
                edge.type = condition.getType();
            }
            return null;
        }

        @Override
        public Void visitProperty(PropertyCondition condition) {
            setProperty(condition.getVariableName(), condition.getPropertyKey(), resolve(condition.getValue()));
            return null;
        }

        @Override
        public Void visitComparison(ComparisonCondition condition) {
            switch (condition.getOperator()) {
                case EQUALS:
                case LESS_THAN:
                case LESS_THAN_OR_EQUALS:
                case GREATER_THAN:
                case GREATER_THAN_OR_EQUALS:
                case STARTS_WITH:
                case ENDS_WITH:
                case CONTAINS:
                    break;
                default:
                    return null;
            }
            if (condition.getLeft() instanceof PropertyOperand) {
                PropertyOperand property = (PropertyOperand) condition.getLeft();
                setProperty(property.getVariableName(), property.getPropertyKey(), resolve(condition.getRight()));
            } else if (condition.getRight() instanceof PropertyOperand) {
                PropertyOperand property = (PropertyOperand) condition.getRight();
                setProperty(property.getVariableName(), property.getPropertyKey(), resolve(condition.getLeft()));
            }
            return null;
        }

        @Override
        public Void visitAnd(AndCondition condition) {
            for (CypherCondition c : condition.getConditions()) {
                c.accept(this);
            }
            return null;
        }

        @Override
        public Void visitOr(OrCondition condition) {
            return null;
        }

        @Override
        public Void visitXor(XorCondition condition) {
            return null;
        }

        @Override
        public Void visitNot(NotCondition condition) {
            return null;
        }

        @Override
        public Void visitRaw(RawCondition condition) {
            return null;
        }

        /**
         * @return the value of a literal or of a parameter captured with the query, {@code null} for
         *         anything else, including a parameter that is missing or bound to {@code null}
         */
        private Object resolve(Operand operand) {
            if (operand instanceof LiteralOperand) {
                return ((LiteralOperand) operand).getValue();
            }
            if (operand instanceof ParameterOperand) {
                return parameters.get(((ParameterOperand) operand).getName());
            }
            return null;
        }

        private void setProperty(String variable, String key, Object value) {
            Neo4jPropertyTypeDto type = typeOf(value);
            if (type == null) {
                return;
            }
            List<Neo4jInsertionEntryDto> properties = propertiesOf(variable);
            if (properties == null) {
                return;
            }
            for (Neo4jInsertionEntryDto property : properties) {
                if (property.propertyKey.equals(key)) {
                    return;
                }
            }
            properties.add(new Neo4jInsertionEntryDto(key, type, String.valueOf(value)));
        }

        /** @return the properties of the node or relationship named {@code variable}, or {@code null} if there is none */
        private List<Neo4jInsertionEntryDto> propertiesOf(String variable) {
            Neo4jNodeInsertionDto node = nodes.get(variable);
            if (node != null) {
                return node.properties;
            }
            Neo4jEdgeInsertionDto edge = edges.get(variable);
            return edge != null ? edge.properties : null;
        }

        /** @return the type a value is stored with, or {@code null} for a value the insertion cannot carry */
        private static Neo4jPropertyTypeDto typeOf(Object value) {
            if (value instanceof String) {
                return Neo4jPropertyTypeDto.STRING;
            }
            if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
                return Neo4jPropertyTypeDto.INTEGER;
            }
            if (value instanceof Double || value instanceof Float) {
                return Neo4jPropertyTypeDto.FLOAT;
            }
            if (value instanceof Boolean) {
                return Neo4jPropertyTypeDto.BOOLEAN;
            }
            return null;
        }
    }
}
