import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.sql.internal.constraint.DbTableConstraint;
import org.evomaster.core.sql.SqlAction;
import org.evomaster.core.sql.schema.Table;

import java.util.List;

/**
 * The interface for the constraint solver only for Database Constraints.
 * Such as Check, Unique, Primary Key, Foreign Key, etc.
 */
public interface DbConstraintSolver extends AutoCloseable {

    /**
     * Solves the given constraints and returns the Db Gene to insert in the database
     * @return a list of SQLAction with the inserts in the db for the given constraints
     */
    List<SqlAction> solve();
}
