import org.evomaster.core.sql.SqlAction;

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
