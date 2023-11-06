package org.evomaster.client.java.controller.api.dto.database.schema;

import org.evomaster.client.java.controller.api.dto.constraint.ElementConstraintsDto;

/**
 * These represent extra constraints that are not directly expressed in the database.
 * On JVM, this could be for example inferred from JPA annotations on Entity classes.
 */
public class ExtraConstraintsDto {

    public String tableName;

    public String columnName;

    public ElementConstraintsDto constraints;

    //TODO custom constraints, and intra-column constraints
}
