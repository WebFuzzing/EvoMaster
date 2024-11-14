package org.evomaster.client.java.controller.api.dto.database.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a database.
 * We use the general term "schema", although it should NOT be confused with "schema" in a SQL database such as Postgres.
 * It is at a higher level.
 * It is rather representing all the info identifying a database "instance" that can be inferred from one "connection"
 * to such an instance.
 */
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
