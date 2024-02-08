import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.internal.constraint.DbTableConstraint;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.gene.numeric.IntegerGene;
import org.evomaster.core.sql.SqlAction;
import org.evomaster.core.sql.SqlInsertBuilder;
import org.evomaster.core.sql.schema.Column;
import org.evomaster.core.sql.schema.Table;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.containers.BindMode;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A smt2 solver implementation using Z3 in a Docker container.
 */
public class DbConstraintSolverZ3InDocker implements DbConstraintSolver {

    public static final String Z3_DOCKER_IMAGE = "ghcr.io/z3prover/z3:ubuntu-20.04-bare-z3-sha-ba8d8f0";

    // The default entrypoint runs directly z3, we need to keep it running
    public static final String ENTRYPOINT = "while :; do sleep 1000 ; done";

    private final String containerPath = "/smt2-resources/";
    private final GenericContainer<?>  z3Prover;
    private final String tmpFolderPath;
    private final SqlInsertBuilder sqlInsertBuilder;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DbConstraintSolverZ3InDocker.class.getName());
    private final DbSchemaDto schemaDto;

    /**
     * The current implementation of the Z3 solver reads content either from STDIN or from a file.
     * In this case, it is reading from a file each time it needs to solve a problem.
     * As the file generation is dynamical, the Docker container needs to have access to the file system.
     * So, the Docker volume is linked to the file system folder.
     * Then, the result is returned in STDOUT.
     * @param resourcesFolder the name of the folder in the file system that will be linked to the Docker volume
     */
    public DbConstraintSolverZ3InDocker(DbSchemaDto schemaDto, String resourcesFolder) {

        this.tmpFolderPath = resourcesFolder + "tmp/";
        this.schemaDto = schemaDto;

        ImageFromDockerfile image = new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder -> builder
                        .from(Z3_DOCKER_IMAGE)
                        .entryPoint(ENTRYPOINT)
                        .build());

        z3Prover = new GenericContainer<>(image)
                        .withFileSystemBind(resourcesFolder, containerPath, BindMode.READ_WRITE);

        z3Prover.start();

        this.sqlInsertBuilder = new SqlInsertBuilder(schemaDto, null);
    }

    /**
     * Deletes the tmp folder with all its content and then stops the Z3 Docker container.
     */
    @Override
    public void close() {
        try {
            FileUtils.deleteDirectory(new File(this.tmpFolderPath));
        } catch (IOException e) {
            log.error(String.format("Error deleting tmp folder '%s'. ", this.tmpFolderPath), e);
        }
        z3Prover.stop();
    }

    /**
     * Reads from a file in the container 'filename' the smt2 problem, and runs z3 with it.
     * Returns the result as string.
     * The file must be in the containerPath defined in the constructor.
     * @param fileName the name of the file to read
     * @return the result of the Z3 solver with the obtained model as string
     */
    String solveFromFile(String fileName) {
        try {
            Container.ExecResult result = z3Prover.execInContainer("z3", containerPath + fileName);
            String stdout = result.getStdout();
            if (stdout == null || stdout.isEmpty()) {
                String stderr = result.getStderr();
                throw new RuntimeException(stderr);
            }
            return stdout;
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<SqlAction> solveFromTmp(String filename) {
        String model = solveFromFile("tmp/" + filename);
        return toSqlAction(model);
    }

    /**
     * Given the list of constraints takes the first one and solve the model for that
     *
     * @param model the string with the model
     * @return the Gene with the model
     */
    private List<SqlAction> toSqlAction(String model) {
        // Create the insert based on the first constraint
        String[] lines = model.split("\n");
        Map<String, Gene> genes = new HashMap<>();

        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].substring(2, lines[i].length()-2).split(" ");
            String name = values[0];
            Integer value =  Integer.parseInt(values[1]);

            Gene gene = new IntegerGene(
                    name,
                    value,
                    null,
                    null,
                    null,
                    false,
                    false);
            genes.put(name, gene);
        }

        // TODO: Handle more than 1 table
        String tableName = this.schemaDto.tables.get(0).name;
        List<String> history = new LinkedList<>();
        Set<String> columnNames = new HashSet<>(Collections.singletonList("*"));

        List<SqlAction> actions = sqlInsertBuilder.createSqlInsertionAction(tableName,
                columnNames, history, false, false, false);

        actions.get(0).seeTopGenes().forEach(g -> {
            if (genes.containsKey(g.getName())) {
                g.copyValueFrom(genes.get(g.getName()));
            } else {
                log.warn("Gene not found in the model: " + g.getName());
            }
        });

        return actions;
    }

    /**
     * Given the constraint list, creates a Smt2Writer with all of them, and then write them in a smt2 file.
     * After that, run the Z3 Docker solver with the reference to the file and return the check model as string.
     * @return a list of Sql with the necessary inserts according to the constraints
     */
    @Override
    public List<SqlAction> solve() {
        Smt2Writer writer = new Smt2Writer();

        for (TableDto table : this.schemaDto.tables) {
            table.tableCheckExpressions.forEach(constraint -> {
                boolean succeed = writer.addTableCheckExpression(constraint);
                if (!succeed) {
                    throw new RuntimeException("Constraint not supported: " + constraint);
                }
            });
        }

        String fileName = storeToTmpFile(writer);

        List<SqlAction> solution = solveFromTmp(fileName);

        try {
            // TODO: Move this to another thread?
            deleteFile(fileName);
        }   catch (IOException e) {
            //  If this fails, then all the content from the tmp folder will be deleted on close
            log.error(String.format("Error deleting tmp file '%s'. ", fileName), e);
        }

        return solution;
    }

    /**
     * Stores the content of the Smt2Writer in a file in the tmp folder.
     * @param writer the Smt2Writer with the constraints
     * @return the name of the file
     */
    private String storeToTmpFile(Smt2Writer writer) {
        String fileName = "smt2_" + System.currentTimeMillis() + ".smt2";
        try {
            Files.createDirectories(Paths.get(this.tmpFolderPath));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating tmp folder '%s'. ", this.tmpFolderPath), e);
        }

        Path fullPath = Paths.get( this.tmpFolderPath + fileName);
        writer.writeToFile(fullPath.toString());

        return fileName;
    }

    private void deleteFile(String fileName) throws IOException {
        Path fileToDelete = Paths.get( this.tmpFolderPath + fileName);

        Files.delete(fileToDelete);
    }
}
