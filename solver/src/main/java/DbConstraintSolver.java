import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;

import java.time.Instant;

public class DbConstraintSolver implements AutoCloseable {

    private final Z3SolverExecutor executor;
    private final SMTGenerator generator;
    private final String tmpFolderPath;
    private final String resourcesFolder;

    DbConstraintSolver (DbSchemaDto schemaDto, String resourcesFolder) {
        String instant = Long.toString(Instant.now().getEpochSecond());
        tmpFolderPath = "tmp_" + instant + "/";
        this.resourcesFolder = resourcesFolder + tmpFolderPath;
        executor = new Z3SolverExecutor(resourcesFolder);
        generator = new SMTGenerator(schemaDto);
    }

    /**
     * Solves the given constraints and returns the Db Gene to insert in the database
     * @return a list of SQLAction with the inserts in the db for the given constraints
     */
    // TODO: this should return a sqlaction list    List<SqlAction> solve();
    String solve(String sqlQuery) {
        String fileName = "smt2_" + System.currentTimeMillis() + ".smt2";
        try {
            generator.generateSMTFile(sqlQuery,resourcesFolder + fileName);
            return executor.solveFromFile(tmpFolderPath + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void close() {
        executor.close();
    }
}
