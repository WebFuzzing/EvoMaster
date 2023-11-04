package org.evomaster.client.java.controller.api.dto.database.schema;

import java.util.ArrayList;
import java.util.List;

public class DbSchemaDto {

    public DatabaseType databaseType;

    public String name;

    public List<TableDto> tables = new ArrayList<>();

    public List<EnumeratedTypeDto> enumeraredTypes = new ArrayList<>();

    public boolean employSmartDbClean;

    public List<CompositeTypeDto> compositeTypes = new ArrayList<>();

    /**
     * This is inferred with heuristics, eg parsing JPA Entities
     */
    public List<ExtraConstraintsDto> extraConstraintDtos = new ArrayList<>();
}
