package org.evomaster.client.java.controller.internal.db.neo4j;

import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityPropertyDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jExistingDataDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jUniqueKeyDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;
import org.evomaster.client.java.controller.neo4j.Neo4jInsertionTemplateBuilder;
import org.evomaster.client.java.controller.neo4j.ReflectionBasedNeo4jClient;
import org.evomaster.client.java.controller.neo4j.data.Neo4jGraph;
import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads which node properties must hold unique values and which of those values the database already
 * holds. A key comes from two places: the uniqueness and node key constraints of the database, and the
 * id the SUT's entities declare, which Spring Data assumes unique without the database enforcing it.
 */
public class Neo4jUniqueKeyReader {

    /*
     * Columns of SHOW CONSTRAINTS that are read. They build the query as well as read the records back,
     * so the name asked for can never drift from the name read.
     */
    private static final String TYPE_FIELD = "type";
    private static final String ENTITY_TYPE_FIELD = "entityType";
    private static final String LABELS_FIELD = "labelsOrTypes";
    private static final String PROPERTIES_FIELD = "properties";

    private static final String CONSTRAINTS_QUERY = "SHOW CONSTRAINTS YIELD "
            + TYPE_FIELD + ", " + ENTITY_TYPE_FIELD + ", " + LABELS_FIELD + ", " + PROPERTIES_FIELD;

    /** Value of the {@code entityType} column for a constraint on nodes rather than on relationships. */
    private static final String NODE_ENTITY_TYPE = "NODE";

    /**
     * End of the value of the {@code type} column for a uniqueness constraint, which Neo4j calls
     * {@code UNIQUENESS} or {@code NODE_PROPERTY_UNIQUENESS} depending on the version.
     */
    private static final String UNIQUENESS_TYPE_SUFFIX = "UNIQUENESS";

    /** Value of the {@code type} column for a node key, which implies uniqueness. */
    private static final String NODE_KEY_TYPE = "NODE_KEY";

    private final Neo4jGraphReader graphReader = new Neo4jGraphReader();

    /**
     * @param client client over the SUT's Neo4j driver
     * @param entities the entities of the SUT, whose ids are unique keys too
     * @return the unique keys, each with the values the database already holds for it
     */
    public Neo4jExistingDataDto read(ReflectionBasedNeo4jClient client, List<Neo4jEntityDto> entities) {
        Map<String, Neo4jUniqueKeyDto> keys = new LinkedHashMap<>();
        readConstraintKeys(client).forEach(k -> keys.putIfAbsent(identity(k), k));
        entityKeys(entities).forEach(k -> keys.putIfAbsent(identity(k), k));

        Neo4jExistingDataDto dto = new Neo4jExistingDataDto();
        if (keys.isEmpty()) {
            return dto;
        }

        Neo4jGraph graph = graphReader.read(client);
        for (Neo4jUniqueKeyDto key : keys.values()) {
            key.existingValues = existingValues(graph, key);
            dto.uniqueKeys.add(key);
        }
        return dto;
    }

    /**
     * The node keys enforced by the database. When the constraints cannot be listed or read (eg, a
     * version without {@code SHOW CONSTRAINTS}, or a column it does not have), there are none to
     * report, and the entity ids still are.
     */
    private List<Neo4jUniqueKeyDto> readConstraintKeys(ReflectionBasedNeo4jClient client) {
        Object session = client.session();
        try {
            List<Neo4jUniqueKeyDto> keys = new ArrayList<>();
            for (Object record : client.runAndList(session, CONSTRAINTS_QUERY)) {
                String type = client.asString(client.get(record, TYPE_FIELD));
                String entityType = client.asString(client.get(record, ENTITY_TYPE_FIELD));
                if (!NODE_ENTITY_TYPE.equals(entityType) || !isUniqueType(type)) {
                    continue;
                }
                List<String> properties = toStrings(client.asList(client.get(record, PROPERTIES_FIELD)));
                for (String label : toStrings(client.asList(client.get(record, LABELS_FIELD)))) {
                    keys.add(new Neo4jUniqueKeyDto(label, properties));
                }
            }
            return keys;
        } catch (RuntimeException e) {
            SimpleLogger.warn("Failed to read the Neo4j constraints: " + e.getMessage());
            return Collections.emptyList();
        } finally {
            client.close(session);
        }
    }

    /**
     * Whether a constraint type forbids two nodes from sharing values.
     */
    private boolean isUniqueType(String type) {
        return type != null && (type.endsWith(UNIQUENESS_TYPE_SUFFIX) || type.equals(NODE_KEY_TYPE));
    }

    /**
     * One key per entity id, on the primary label only: Spring Data looks nodes up by their primary
     * label, so two entities sharing a secondary label may still repeat an id.
     */
    private List<Neo4jUniqueKeyDto> entityKeys(List<Neo4jEntityDto> entities) {
        List<Neo4jUniqueKeyDto> keys = new ArrayList<>();
        if (entities == null) {
            return keys;
        }
        for (Neo4jEntityDto entity : entities) {
            if (entity.labels == null || entity.labels.isEmpty() || entity.properties == null) {
                continue;
            }
            String primaryLabel = entity.labels.get(0);
            for (Neo4jEntityPropertyDto property : entity.properties) {
                if (Boolean.TRUE.equals(property.isId) && property.name != null) {
                    keys.add(new Neo4jUniqueKeyDto(primaryLabel, Collections.singletonList(property.name)));
                }
            }
        }
        return keys;
    }

    /**
     * The values of the key on the nodes with its label that hold all of its properties. A node lacking
     * one of them is not bound by the key, as in Neo4j. A node whose value has a type an insertion cannot
     * hold (eg, a list or a date) cannot clash with one, so it is left out too.
     */
    private List<List<Neo4jInsertionEntryDto>> existingValues(Neo4jGraph graph, Neo4jUniqueKeyDto key) {
        List<List<Neo4jInsertionEntryDto>> existing = new ArrayList<>();
        for (Neo4jNode node : graph.getNodes()) {
            if (!node.getLabels().contains(key.label)) {
                continue;
            }
            List<Neo4jInsertionEntryDto> values = valuesOf(node, key.properties);
            if (values != null) {
                existing.add(values);
            }
        }
        return existing;
    }

    /**
     * @return the values {@code node} holds for {@code properties}, in the same order, or null if it lacks
     * one or holds one an insertion cannot carry
     */
    private List<Neo4jInsertionEntryDto> valuesOf(Neo4jNode node, List<String> properties) {
        List<Neo4jInsertionEntryDto> values = new ArrayList<>();
        for (String property : properties) {
            Object value = node.getProperty(property);
            Neo4jInsertionEntryDto entry = value == null ? null : Neo4jInsertionTemplateBuilder.entryOf(property, value);
            if (entry == null) {
                return null;
            }
            values.add(entry);
        }
        return values;
    }

    private String identity(Neo4jUniqueKeyDto key) {
        return key.label + "|" + String.join("|", key.properties);
    }

    private List<String> toStrings(List<?> values) {
        List<String> strings = new ArrayList<>();
        for (Object v : values) {
            strings.add(String.valueOf(v));
        }
        return strings;
    }
}
