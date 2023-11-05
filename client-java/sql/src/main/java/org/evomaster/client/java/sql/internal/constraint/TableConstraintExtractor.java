package org.evomaster.client.java.sql.internal.constraint;

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Unfortunately JDBC only provides a limited support for extracting
 * constraints. For example, primary keys and foreign keys are supported, while
 * unique and check expressions are not. Each time support for a new database
 * engine is added to EvoMaster it has to be provided a custom extractor
 * to collect the <code>TableConstraint</code>.
 * <p>
 * Whenever a new TableConstraintExtractor is implemented, it has to be
 * register in the <code>TableConstraintExtractorFactory</code>.
 */
public abstract class TableConstraintExtractor {

    public abstract List<DbTableConstraint> extract(Connection connectionToDatabase, DbSchemaDto schemaDto) throws SQLException;


}
