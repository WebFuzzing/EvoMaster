package org.evomaster.client.java.controller.neo4j;

import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityPropertyDto;
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityRelationshipDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto;
import org.evomaster.client.java.instrumentation.Neo4jEntity;
import org.evomaster.client.java.instrumentation.Neo4jEntityProperty;
import org.evomaster.client.java.instrumentation.Neo4jEntityRelationship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the entities the instrumentation found in the SUT into the DTOs sent to the core.
 * Relationships point to a class in the instrumentation, and to the labels of that class in the DTO,
 * as the core only ever sees labels.
 */
public class Neo4jEntityDtoBuilder {

    public Neo4jEntityDtoBuilder() {
    }

    /**
     * @param entities the entities found in the SUT
     * @return one DTO per entity, with relationships towards classes that are not entities themselves
     *         left out
     */
    public static List<Neo4jEntityDto> build(List<Neo4jEntity> entities) {
        Map<String, List<String>> labelsByClass = new HashMap<>();
        for (Neo4jEntity e : entities) {
            labelsByClass.put(e.getClassName(), e.getLabels());
        }
        List<Neo4jEntityDto> dtos = new ArrayList<>();
        for (Neo4jEntity e : entities) {
            Neo4jEntityDto dto = new Neo4jEntityDto();
            dto.className = e.getClassName();
            dto.labels = new ArrayList<>(e.getLabels());
            for (Neo4jEntityProperty p : e.getProperties()) {
                dto.properties.add(buildProperty(p));
            }
            for (Neo4jEntityRelationship r : e.getRelationships()) {
                List<String> target = labelsByClass.get(r.getTargetClassName());
                if (target == null) {
                    continue;
                }
                Neo4jEntityRelationshipDto rd = new Neo4jEntityRelationshipDto();
                rd.type = r.getType();
                rd.targetLabels = new ArrayList<>(target);
                rd.isOutgoing = r.isOutgoing();
                dto.relationships.add(rd);
            }
            dtos.add(dto);
        }
        return dtos;
    }

    private static Neo4jEntityPropertyDto buildProperty(Neo4jEntityProperty p) {
        Neo4jEntityPropertyDto pd = new Neo4jEntityPropertyDto();
        pd.name = p.getName();
        pd.type = Neo4jPropertyTypeDto.valueOf(p.getType());
        pd.isId = p.isId();
        pd.isGenerated = p.isGenerated();
        pd.minValue = p.getMinValue();
        pd.maxValue = p.getMaxValue();
        pd.enumValues = new ArrayList<>(p.getEnumValues());
        return pd;
    }
}
