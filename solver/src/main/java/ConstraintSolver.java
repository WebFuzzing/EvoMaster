public interface ConstraintSolver extends AutoCloseable {
    // TODO: Input parameter should be a set of constraints and not the file name
    String solve(String fileName);
}
