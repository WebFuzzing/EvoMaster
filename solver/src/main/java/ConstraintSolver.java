import org.evomaster.client.java.sql.internal.constraint.DbTableConstraint;

import java.util.List;

public interface ConstraintSolver extends AutoCloseable {
    // TODO: Input parameter should be a set of constraints and not the file name
    String solve(List<DbTableConstraint> constraintList);
}
