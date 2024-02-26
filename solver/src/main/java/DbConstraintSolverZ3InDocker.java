import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.gene.collection.EnumGene;
import org.evomaster.core.search.gene.numeric.*;
import org.evomaster.core.search.gene.optional.NullableGene;
import org.evomaster.core.sql.SqlAction;
import org.evomaster.core.sql.SqlInsertBuilder;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

/**
 * A smt2 solver implementation using Z3 in a Docker container.
 */
public class DbConstraintSolverZ3InDocker implements DbConstraintSolver {

    public static final String Z3_DOCKER_IMAGE = "ghcr.io/z3prover/z3:ubuntu-20.04-bare-z3-sha-ba8d8f0";

    // The default entrypoint runs directly z3, we need to keep it running
    public static final String ENTRYPOINT = "while :; do sleep 1000 ; done";

    private final String containerPath = "/smt2-resources/";
    private final GenericContainer<?>  z3Prover;

    private final String resourcesFolder;
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

        this.resourcesFolder = resourcesFolder;
        String instant = Long.toString(Instant.now().getEpochSecond());
        this.tmpFolderPath = "tmp_" + instant + "/";
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
            FileUtils.deleteDirectory(new File(this.resourcesFolder + this.tmpFolderPath));
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

    private List<SqlAction> solveFromTmp(Smt2Writer writer, String filename) {
        String model = solveFromFile(this.tmpFolderPath + filename);
        Map<String, String> solvedConstraints = toSolvedConstraintsMap(model);
        return toSqlAction(writer, solvedConstraints);
    }

    private Map<String, String> toSolvedConstraintsMap(String model) {
        String[] lines = model.split("\n");
        Map<String, String> solved = new HashMap<>();

        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].substring(2, lines[i].length()-2).split(" ");

            solved.put(values[0], values[1]);
        }
        return solved;
    }

    private List<SqlAction> toSqlAction(Smt2Writer writer, Map<String, String> solvedConstraints) {

        List<SqlAction> actions = new LinkedList<>();
        for (TableDto table : this.schemaDto.tables) {

            String tableName = table.name;
            List<String> history = new LinkedList<>();
            Set<String> columnNames = new HashSet<>(Collections.singletonList("*"));

            List<SqlAction> newActions = sqlInsertBuilder.createSqlInsertionAction(tableName,
                    columnNames, history, false, false, false);

            newActions.forEach(action -> action.seeTopGenes().forEach(currentGene -> {
                String tableVariableKey = writer.asTableVariableKey(table.name, currentGene.getName());
                if (solvedConstraints.containsKey(tableVariableKey)) {

                    if (currentGene instanceof NullableGene) {
                        currentGene = ((NullableGene) currentGene).getGene();
                    }

                    String solvedValue = solvedConstraints.get(tableVariableKey);

                    if (currentGene instanceof EnumGene) {
                        // There is nothing to do, as the enum has the values already parsed
                        return;
                    }

                    if (currentGene instanceof IntegerGene) {
                        Integer value =  Integer.parseInt(solvedValue);
                        Gene geneWithSolvedValue = new IntegerGene(
                                currentGene.getName(),
                                value,
                                null,
                                null,
                                null,
                                false,
                                false);
                        currentGene.copyValueFrom(geneWithSolvedValue);
                    } else if (currentGene instanceof LongGene) {
                        Long value =  Long.parseLong(solvedValue);
                        Gene geneWithSolvedValue = new LongGene(
                                currentGene.getName(),
                                value,
                                null,
                                null,
                                null,
                                false,
                                false);
                        currentGene.copyValueFrom(geneWithSolvedValue);
                    } else if (currentGene instanceof BigIntegerGene) {
                        BigInteger value = new BigInteger(solvedValue);
                        Gene geneWithSolvedValue = new BigIntegerGene(
                                currentGene.getName(),
                                value,
                                null,
                                null,
                                null,
                                false,
                                false);
                        currentGene.copyValueFrom(geneWithSolvedValue);
                    } else if (currentGene instanceof DoubleGene) {
                        Double value =  Double.parseDouble(solvedValue);
                        Gene geneWithSolvedValue = new DoubleGene(
                                currentGene.getName(),
                                value,
                                null,
                                null,
                                false,
                                false,
                                null,
                                null);
                        currentGene.copyValueFrom(geneWithSolvedValue);
                    } else if (currentGene instanceof FloatGene) {
                        Float value =  Float.parseFloat(solvedValue);
                        Gene geneWithSolvedValue = new FloatGene(
                                currentGene.getName(),
                                value,
                                null,
                                null,
                                false,
                                false,
                                null,
                                null);
                        currentGene.copyValueFrom(geneWithSolvedValue);

                    } else {
                        log.warn("There was a solved value for the gene, but it was not parsed: " + currentGene.getName());
                    }
                } else {
                    log.warn("Gene not found in the model: " + currentGene.getName());
                }
            }));

            actions.addAll(newActions);
        }
        return actions;
    }

    /**
     * Given the constraint list, creates a Smt2Writer with all of them, and then write them in a smt2 file.
     * After that, run the Z3 Docker solver with the reference to the file and return the check model as string.
     * @return a list of Sql with the necessary inserts according to the constraints
     */
    @Override
    public List<SqlAction> solve() {
        Smt2Writer writer = new Smt2Writer(this.schemaDto.databaseType);

        for (TableDto table : this.schemaDto.tables) {
            table.tableCheckExpressions.forEach(constraint -> {
                boolean succeed = writer.addTableCheckExpression(table.name, constraint);
                if (!succeed) {
                    throw new RuntimeException("Constraint not supported: " + constraint);
                }
            });
        }

        String fileName = storeToTmpFile(writer);

        List<SqlAction> solution = solveFromTmp(writer, fileName);

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
            Files.createDirectories(Paths.get(this.resourcesFolder + this.tmpFolderPath));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating tmp folder '%s'. ", this.tmpFolderPath), e);
        }

        Path fullPath = Paths.get(  this.resourcesFolder + this.tmpFolderPath + fileName);
        writer.writeToFile(fullPath.toString());

        return fileName;
    }

    private void deleteFile(String fileName) throws IOException {
        Path fileToDelete = Paths.get(this.resourcesFolder + this.tmpFolderPath + fileName);

        Files.delete(fileToDelete);
    }
}
