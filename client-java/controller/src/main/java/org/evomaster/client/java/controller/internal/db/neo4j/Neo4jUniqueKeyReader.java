package org.evomaster.client.java.controller.internal.db.neo4j;

import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jDbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityPropertyDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jUniqueKeyDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto;
import org.evomaster.client.java.controller.neo4j.Neo4jScriptRunner;
import org.evomaster.client.java.controller.neo4j.ReflectionBasedNeo4jClient;
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
     * Columns of SHOW CONSTRAINTS that are read, plus the alias the values of a key are returned under.
     * They build the queries as well as read the records back, so the name asked for can never drift
     * from the name read.
     */
    private static final String TYPE_FIELD = "type";
    private static final String ENTITY_TYPE_FIELD = "entityType";
    private static final String LABELS_FIELD = "labelsOrTypes";
    private static final String PROPERTIES_FIELD = "properties";
    private static final String VALUES_FIELD = "values";

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

    /**
     * @param client client over the SUT's Neo4j driver
     * @param entities the entities of the SUT, whose ids are unique keys too
     * @return the unique keys, each with the values the database already holds for it
     */
    public Neo4jDbInfoDto read(ReflectionBasedNeo4jClient client, List<Neo4jEntityDto> entities) {
        Neo4jDbInfoDto dto = new Neo4jDbInfoDto();
        Object session = client.session();
        try {
            Map<String, Neo4jUniqueKeyDto> keys = new LinkedHashMap<>();
            readConstraintKeys(client, session).forEach(k -> keys.putIfAbsent(identity(k), k));
            entityKeys(entities).forEach(k -> keys.putIfAbsent(identity(k), k));

            for (Neo4jUniqueKeyDto key : keys.values()) {
                key.existingValues = readExistingValues(client, session, key);
                dto.uniqueKeys.add(key);
            }
        } finally {
            client.close(session);
        }
        return dto;
    }

    /**
     * The node keys enforced by the database. When the constraints cannot be listed or read (eg, a
     * version without {@code SHOW CONSTRAINTS}, or a column it does not have), there are none to
     * report, and the entity ids still are.
     */
    private List<Neo4jUniqueKeyDto> readConstraintKeys(ReflectionBasedNeo4jClient client, Object session) {
        List<Neo4jUniqueKeyDto> keys = new ArrayList<>();
        try {
            for (Object record : client.runAndList(session, CONSTRAINTS_QUERY)) {
                String type = client.asString(client.get(record, TYPE_FIELD));
                String entityType = client.asString(client.get(record, ENTITY_TYPE_FIELD));
                if (!NODE_ENTITY_TYPE.equals(entityType) || !isUniqueType(type)) {
                    continue;
                }
                List<String> properties = toStrings(client.asList(client.get(record, PROPERTIES_FIELD)));
                for (String label : toStrings(client.asList(client.get(record, LABELS_FIELD)))) {
                    keys.add(key(label, properties));
                }
            }
        } catch (RuntimeException e) {
            SimpleLogger.warn("Failed to read the Neo4j constraints: " + e.getMessage());
            return Collections.emptyList();
        }
        return keys;
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
            for (Neo4jEntityPropertyDto p : entity.properties) {
                if (Boolean.TRUE.equals(p.isId) && p.name != null) {
                    keys.add(key(entity.labels.get(0), Collections.singletonList(p.name)));
                }
            }
        }
        return keys;
    }

    /**
     * The values of the key on the nodes that hold all of its properties. A node whose value has a type
     * an insertion cannot hold (eg, a list or a date) cannot clash with one, so it is left out.
     */
    private List<List<Neo4jInsertionEntryDto>> readExistingValues(ReflectionBasedNeo4jClient client,
                                                                  Object session, Neo4jUniqueKeyDto key) {
        StringBuilder cypher = new StringBuilder("MATCH (n:").append(Neo4jScriptRunner.escapeName(key.label)).append(")");
        List<String> accesses = new ArrayList<>();
        for (int i = 0; i < key.propertyKeys.size(); i++) {
            String access = "n." + Neo4jScriptRunner.escapeName(key.propertyKeys.get(i));
            accesses.add(access);
            cypher.append(i == 0 ? " WHERE " : " AND ").append(access).append(" IS NOT NULL");
        }
        cypher.append(" RETURN [").append(String.join(", ", accesses)).append("] AS ").append(VALUES_FIELD);

        List<List<Neo4jInsertionEntryDto>> existing = new ArrayList<>();
        for (Object record : client.runAndList(session, cypher.toString())) {
            List<?> values = client.asList(client.get(record, VALUES_FIELD));
            List<Neo4jInsertionEntryDto> entries = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                Neo4jInsertionEntryDto entry = toEntry(key.propertyKeys.get(i), values.get(i));
                if (entry == null) {
                    break;
                }
                entries.add(entry);
            }
            if (entries.size() == key.propertyKeys.size()) {
                existing.add(entries);
            }
        }
        return existing;
    }

    /**
     * @return the value with the type and text an insertion would give it, or null for a type
     * insertions do not support
     */
    private Neo4jInsertionEntryDto toEntry(String propertyKey, Object value) {
        Neo4jPropertyTypeDto type;
        if (value instanceof String) {
            type = Neo4jPropertyTypeDto.STRING;
        } else if (value instanceof Long || value instanceof Integer) {
            type = Neo4jPropertyTypeDto.INTEGER;
            value = ((Number) value).longValue();
        } else if (value instanceof Double || value instanceof Float) {
            type = Neo4jPropertyTypeDto.FLOAT;
            value = ((Number) value).doubleValue();
        } else if (value instanceof Boolean) {
            type = Neo4jPropertyTypeDto.BOOLEAN;
        } else {
            return null;
        }
        return new Neo4jInsertionEntryDto(propertyKey, type, String.valueOf(value));
    }

    private Neo4jUniqueKeyDto key(String label, List<String> properties) {
        Neo4jUniqueKeyDto key = new Neo4jUniqueKeyDto();
        key.label = label;
        key.propertyKeys = new ArrayList<>(properties);
        return key;
    }

    private String identity(Neo4jUniqueKeyDto key) {
        return key.label + "|" + String.join("|", key.propertyKeys);
    }

    private List<String> toStrings(List<?> values) {
        List<String> strings = new ArrayList<>();
        for (Object v : values) {
            strings.add(String.valueOf(v));
        }
        return strings;
    }
}
