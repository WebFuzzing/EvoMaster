import org.evomaster.client.java.sql.internal.constraint.DbTableConstraint;

import java.util.List;

/**
 * The interface for the constraint solver only for Database Constraints.
 * Such as Check, Unique, Primary Key, Foreign Key, etc.
 */
public interface DbConstraintSolver extends AutoCloseable {

    /**
     * Solves the given constraints and returns the Db Gene to insert in the database
     * @param constraintList list of database constraints
     * @return a string with the model for the given constraints
     * TODO: Response should be a Gene and not a string
     */
    String solve(List<DbTableConstraint> constraintList);
}
