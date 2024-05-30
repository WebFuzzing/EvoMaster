import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

public class DbConstraintSolver implements AutoCloseable {

    private final Z3SolverExecutor executor;
    private final SMTGenerator generator;
    private final String resourcesFolder;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DbConstraintSolver.class.getName());


    DbConstraintSolver (DbSchemaDto schemaDto, String resourcesFolder) {
        this.resourcesFolder = setupResourcesFolder(resourcesFolder);

        generator = new SMTGenerator(schemaDto);
        executor = new Z3SolverExecutor(this.resourcesFolder);
    }

    private String setupResourcesFolder(String resourcesFolder) {
        String instant = Long.toString(Instant.now().getEpochSecond());
        String tmpFolderPath = "tmp_" + instant + "/";
        String resourcesFolderWithTmpDir = resourcesFolder + tmpFolderPath;
        createTmpFolder(resourcesFolderWithTmpDir);
        return resourcesFolderWithTmpDir;
    }

    private void createTmpFolder(String resourcesFolderWithTmpDir) {
        try {
            Files.createDirectories(Paths.get(resourcesFolderWithTmpDir));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating tmp folder '%s'. ", resourcesFolderWithTmpDir), e);
        }
    }

    @Override
    public void close() {
        executor.close();
        try {
            FileUtils.deleteDirectory(new File(this.resourcesFolder));
        } catch (IOException e) {
            log.error(String.format("Error deleting tmp folder '%s'. ", this.resourcesFolder), e);
        }
    }

    /**
     * Solves the given constraints and returns the Db Gene to insert in the database
     * @return a list of SQLAction with the inserts in the db for the given constraints
     */
    // TODO: this should return a sqlAction list    List<SqlAction> solve();
    String solve(String sqlQuery) {
        String fileName = "smt2_" + System.currentTimeMillis() + ".smt2";
        try {
            String response = generator.generateSMT(sqlQuery);
            writeToFile(response, resourcesFolder + fileName);
            return executor.solveFromFile(fileName);
        } catch (Exception e) {
            log.error(String.format("Error solving smt from sql query '%s'. ", sqlQuery), e);
            return null;
        }
    }

    private static void writeToFile(String smt, String filePath) {
        try {
            Files.write(Paths.get(filePath), smt.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
