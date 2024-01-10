import org.evomaster.client.java.sql.internal.constraint.DbTableConstraint;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.gene.numeric.IntegerGene;
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
import java.util.List;

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
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DbConstraintSolverZ3InDocker.class.getName());

    /**
     * The current implementation of the Z3 solver reads content either from STDIN or from a file.
     * In this case, it is reading from a file each time it needs to solve a problem.
     * As the file generation is dynamical, the Docker container needs to have access to the file system.
     * So, the Docker volume is linked to the file system folder.
     * Then, the result is returned in STDOUT.
     * @param resourcesFolder the name of the folder in the file system that will be linked to the Docker volume
     */
    public DbConstraintSolverZ3InDocker(String resourcesFolder) {

        this.tmpFolderPath = resourcesFolder + "tmp/";

        ImageFromDockerfile image = new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder -> builder
                        .from(Z3_DOCKER_IMAGE)
                        .entryPoint(ENTRYPOINT)
                        .build());

        z3Prover = new GenericContainer<>(image)
                        .withFileSystemBind(resourcesFolder, containerPath, BindMode.READ_WRITE);

        z3Prover.start();
    }

    /**
     * Deletes the tmp folder with all its content and then stops the Z3 Docker container.
     */
    @Override
    public void close() {
        try {
            FileUtils.deleteDirectory(new File(this.tmpFolderPath));
        } catch (IOException e) {
            LOGGER.error(String.format("Error deleting tmp folder '%s'. ", this.tmpFolderPath), e);
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

    private Gene solveFromTmp(String filename) {
        String model = solveFromFile("tmp/" + filename);
        return toGene(model);
    }

    /**
     * Parses the string from the smt2 format response and creates a Gene
     * @param model the string with the model
     * @return the Gene with the model
     */
    private Gene toGene(String model) {
        String[] lines = model.split("\n");
        String[] values = lines[1].substring(2, lines[1].length()-2).split(" ");
        String name = values[0];
        Integer value =  Integer.parseInt(values[1]);
        Integer min = null;
        Integer max = null;
        Integer precision = null;
        boolean minInclusive = false;
        boolean maxInclusive = false;

        return new IntegerGene(
                name,
                value,
                min,
                max,
                precision,
                minInclusive,
                maxInclusive);
    }

    /**
     * Given the constraint list, creates a Smt2Writer with all of them, and then write them in a smt2 file.
     * After that, run the Z3 Docker solver with the reference to the file and return the check model as string.
     * @param constraintList list of database constraints
     * @return a string with the model for the given constraints
     */
    @Override
    public Gene solve(List<DbTableConstraint> constraintList) {
        Smt2Writer writer = new Smt2Writer();

        for (DbTableConstraint constraint : constraintList) {
            boolean succeed = writer.addConstraint(constraint);
            if (!succeed) {
                throw new RuntimeException("Constraint not supported: " + constraint);
            }
        }

        String fileName = storeToTmpFile(writer);

        Gene solution = solveFromTmp(fileName);

        try {
            // TODO: Move this to another thread?
            deleteFile(fileName);
        }   catch (IOException e) {
            //  If this fails, then all the content from the tmp folder will be deleted on close
            LOGGER.error(String.format("Error deleting tmp file '%s'. ", fileName), e);
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
