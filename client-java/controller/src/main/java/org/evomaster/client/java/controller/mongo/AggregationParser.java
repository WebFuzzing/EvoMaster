package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import org.evomaster.client.java.controller.mongo.operations.aggregations.*;

import java.util.*;

/**
 * Translates an aggregation pipeline into the elements of its AST, which are in
 * {@code org.evomaster.client.java.controller.mongo.operations.aggregations}.
 * It is to aggregation pipelines what {@link QueryParser} is to queries.
 * <p>
 * The filter of a $match stage is parsed with {@link QueryParser}, so it is an AST of queries.
 * <p>
 * What is not valid in MongoDB is rejected. What is valid, but cannot be evaluated by the heuristics,
 * is kept in the AST as an {@link UnsupportedStage} or an {@link UnsupportedExpression}.
 * Documents are handled as maps, as org.bson.Document is one.
 */
public class AggregationParser {

    private static final String ID_FIELD = "_id";
    private static final String ROOT_VARIABLE = "$$ROOT";
    private static final String CURRENT_VARIABLE = "$$CURRENT";
    private static final String LITERAL_OPERATOR = "$literal";

    private final QueryParser queryParser = new QueryParser();

    /**
     * Parses a pipeline.
     *
     * @param pipeline the stages of the pipeline. Each stage is an instance of org.bson.Document.
     * @return the AST of the pipeline
     * @throws IllegalArgumentException if the pipeline is not valid
     */
    public AggregationPipeline parse(List<?> pipeline) {
        Objects.requireNonNull(pipeline, "pipeline cannot be null");

        final List<AggregationStage> stages = new ArrayList<>();
        for (Object stage : pipeline) {
            stages.add(parseStage(stage));
        }
        return new AggregationPipeline(stages);
    }

    private AggregationStage parseStage(Object stage) {
        final Map<String, Object> stageDocument = asMap(stage, "A stage must be a document");
        if (stageDocument.size() != 1) {
            throw new IllegalArgumentException("A stage must have exactly one field: " + stage);
        }
        final String name = stageDocument.keySet().iterator().next();
        final Object specification = stageDocument.get(name);

        switch (name) {
            case MatchStage.NAME:
                return parseMatch(specification);
            case GroupStage.NAME:
                return parseGroup(specification);
            case ProjectStage.NAME:
                return parseProject(specification);
            case SortStage.NAME:
                return parseSort(specification);
            case LimitStage.NAME:
                return parseLimit(specification);
            case LookupStage.NAME:
                return parseLookup(specification);
            case UnionWithStage.NAME:
                return parseUnionWith(specification);
            default:
                return new UnsupportedStage(name, "Stage not supported: " + name);
        }
    }

    // ---------------------------------------------------------------- stages

    private AggregationStage parseMatch(Object specification) {
        final QueryOperation filter = queryParser.parse(specification);
        if (filter == null) {
            throw new IllegalArgumentException("The filter of a $match stage is not valid: " + specification);
        }
        return new MatchStage(filter);
    }

    private AggregationStage parseGroup(Object specification) {
        final Map<String, Object> map = asMap(specification, "The specification of a $group stage must be a document");
        if (!map.containsKey(ID_FIELD)) {
            throw new IllegalArgumentException("A $group stage needs an _id field: " + specification);
        }
        final AggregationExpression id = parseExpression(map.get(ID_FIELD));

        final List<GroupAccumulator> accumulators = new ArrayList<>();
        for (Map.Entry<String, Object> field : map.entrySet()) {
            if (!field.getKey().equals(ID_FIELD)) {
                accumulators.add(parseAccumulator(field.getKey(), field.getValue()));
            }
        }
        return new GroupStage(id, accumulators);
    }

    private GroupAccumulator parseAccumulator(String fieldName, Object accumulator) {
        final Map<String, Object> map = asMap(accumulator, "The accumulator of field " + fieldName + " must be a document");
        if (map.size() != 1) {
            throw new IllegalArgumentException("An accumulator must have exactly one operator: " + accumulator);
        }
        final String operatorName = map.keySet().iterator().next();
        final AccumulatorOperator operator = AccumulatorOperator.fromName(operatorName);
        if (operator == null) {
            return new GroupAccumulator(fieldName, operatorName, null, null);
        }
        // $count has no argument
        final AggregationExpression argument = operator == AccumulatorOperator.COUNT
                ? null
                : parseExpression(map.get(operatorName));
        return new GroupAccumulator(fieldName, operatorName, operator, argument);
    }

    private AggregationStage parseProject(Object specification) {
        final Map<String, Object> map = asMap(specification, "The specification of a $project stage must be a document");

        final List<ProjectField> fields = new ArrayList<>();
        for (Map.Entry<String, Object> field : map.entrySet()) {
            if (isZero(field.getValue())) {
                fields.add(new ProjectField(field.getKey(), ProjectFieldType.EXCLUDE, null));
            } else if (isOne(field.getValue())) {
                fields.add(new ProjectField(field.getKey(), ProjectFieldType.INCLUDE, null));
            } else {
                fields.add(new ProjectField(field.getKey(), ProjectFieldType.COMPUTED, parseExpression(field.getValue())));
            }
        }

        final ProjectStage stage = new ProjectStage(fields);
        // _id does not count: it can be excluded in an inclusion, and included in an exclusion
        final boolean inclusion = fields.stream().anyMatch(f -> !f.getFieldPath().equals(ID_FIELD)
                && f.getType() != ProjectFieldType.EXCLUDE);
        if (stage.isExclusion() && inclusion) {
            throw new IllegalArgumentException("A $project cannot include and exclude fields: " + specification);
        }
        return stage;
    }

    private AggregationStage parseSort(Object specification) {
        final Map<String, Object> map = asMap(specification, "The specification of a $sort stage must be a document");
        if (map.isEmpty()) {
            throw new IllegalArgumentException("A $sort stage needs at least one field");
        }
        final List<SortField> fields = new ArrayList<>();
        for (Map.Entry<String, Object> field : map.entrySet()) {
            if (!(field.getValue() instanceof Number)) {
                // eg, {$meta: "textScore"}
                return new UnsupportedStage(SortStage.NAME, "Sort not supported: " + field);
            }
            fields.add(new SortField(field.getKey(), ((Number) field.getValue()).doubleValue() >= 0));
        }
        return new SortStage(fields);
    }

    private AggregationStage parseLimit(Object specification) {
        if (!isIntegral(specification)) {
            throw new IllegalArgumentException("The value of $limit must be an integer: " + specification);
        }
        return new LimitStage(((Number) specification).longValue());
    }

    private AggregationStage parseLookup(Object specification) {
        final Map<String, Object> map = asMap(specification, "The specification of a $lookup stage must be a document");
        if (map.containsKey("pipeline") || map.containsKey("let")) {
            return new UnsupportedStage(LookupStage.NAME, "$lookup with a pipeline is not supported");
        }
        return new LookupStage(
                requireString(map, "from"),
                requireString(map, "localField"),
                requireString(map, "foreignField"),
                requireString(map, "as"));
    }

    private AggregationStage parseUnionWith(Object specification) {
        if (specification instanceof String) {
            return new UnionWithStage((String) specification, new AggregationPipeline(Collections.emptyList()));
        }
        final Map<String, Object> map = asMap(specification, "The specification of a $unionWith stage must be a string or a document");
        final String collection = requireString(map, "coll");
        final AggregationPipeline pipeline = map.containsKey("pipeline")
                ? parse(asList(map.get("pipeline"), "The pipeline of a $unionWith stage must be an array"))
                : new AggregationPipeline(Collections.emptyList());
        return new UnionWithStage(collection, pipeline);
    }

    // ---------------------------------------------------------------- expressions

    /**
     * Parses an aggregation expression.
     */
    AggregationExpression parseExpression(Object value) {
        if (value instanceof String) {
            final String text = (String) value;
            if (text.equals(ROOT_VARIABLE) || text.equals(CURRENT_VARIABLE)) {
                return new RootVariableExpression();
            }
            if (text.startsWith("$$")) {
                return new UnsupportedExpression("Variable not supported: " + text);
            }
            if (text.startsWith("$")) {
                return new FieldPathExpression(text.substring(1));
            }
            return new LiteralExpression(text);
        }
        if (value instanceof List) {
            return new ArrayExpression(parseExpressions((List<?>) value));
        }
        if (value instanceof Map) {
            final Map<String, Object> map = asMap(value, "Expected a document");
            if (map.size() == 1 && map.keySet().iterator().next().startsWith("$")) {
                final String operator = map.keySet().iterator().next();
                return parseOperator(operator, map.get(operator));
            }
            final Map<String, AggregationExpression> fields = new LinkedHashMap<>();
            for (Map.Entry<String, Object> field : map.entrySet()) {
                fields.put(field.getKey(), parseExpression(field.getValue()));
            }
            return new DocumentExpression(fields);
        }
        // a constant: number, boolean, null, date...
        return new LiteralExpression(value);
    }

    private AggregationExpression parseOperator(String operatorName, Object argument) {
        if (operatorName.equals(LITERAL_OPERATOR)) {
            return new LiteralExpression(argument);
        }
        final ExpressionOperator operator = ExpressionOperator.fromName(operatorName);
        if (operator == null) {
            return new UnsupportedExpression("Expression operator not supported: " + operatorName);
        }

        final List<AggregationExpression> arguments;
        if (operator == ExpressionOperator.COND && argument instanceof Map) {
            // the form {if: ..., then: ..., else: ...}
            final Map<String, Object> map = asMap(argument, "Expected a document");
            arguments = Arrays.asList(
                    parseExpression(map.get("if")),
                    parseExpression(map.get("then")),
                    parseExpression(map.get("else")));
        } else if (argument instanceof List) {
            arguments = parseExpressions((List<?>) argument);
        } else {
            arguments = Collections.singletonList(parseExpression(argument));
        }
        return new OperatorExpression(operator, arguments);
    }

    private List<AggregationExpression> parseExpressions(List<?> values) {
        final List<AggregationExpression> expressions = new ArrayList<>();
        for (Object value : values) {
            expressions.add(parseExpression(value));
        }
        return expressions;
    }

    // ---------------------------------------------------------------- utilities

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value, String message) {
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException(message + ": " + value);
        }
        return (Map<String, Object>) value;
    }

    private static List<?> asList(Object value, String message) {
        if (!(value instanceof List)) {
            throw new IllegalArgumentException(message + ": " + value);
        }
        return (List<?>) value;
    }

    private static String requireString(Map<String, Object> specification, String field) {
        final Object value = specification.get(field);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Field " + field + " must be a string: " + specification);
        }
        return (String) value;
    }

    private static boolean isOne(Object value) {
        return Boolean.TRUE.equals(value) || (value instanceof Number && ((Number) value).doubleValue() == 1);
    }

    private static boolean isZero(Object value) {
        return Boolean.FALSE.equals(value) || (value instanceof Number && ((Number) value).doubleValue() == 0);
    }

    private static boolean isIntegral(Object value) {
        return value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte;
    }
}
