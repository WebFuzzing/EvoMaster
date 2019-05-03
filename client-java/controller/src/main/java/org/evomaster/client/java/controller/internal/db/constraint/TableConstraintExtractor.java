package org.evomaster.client.java.controller.internal.db.constraint;

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public abstract class TableConstraintExtractor {

    public abstract List<DbTableConstraint> extract(Connection connectionToDatabase, DbSchemaDto schemaDto) throws SQLException;


}
