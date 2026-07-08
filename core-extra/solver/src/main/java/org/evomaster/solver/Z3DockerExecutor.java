package org.evomaster.solver;

import org.evomaster.solver.smtlib.SMTResultParser;
import org.evomaster.solver.smtlib.value.SMTLibValue;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Z3DockerExecutor is responsible for executing the Z3 SMT solver inside a Docker container.
 * It manages the lifecycle of the Docker container, ensures the solver is executed correctly,
 * and parses the results.
 */
public class Z3DockerExecutor implements AutoCloseable {

    public static final String Z3_DOCKER_IMAGE = "ghcr.io/z3prover/z3:ubuntu-20.04-bare-z3-sha-ba8d8f0";
    // The Docker entrypoint that keeps it running indefinitely
    public static final String ENTRYPOINT = "while :; do sleep 1000 ; done";
    private final String containerPath = "/smt2-resources/";
    private final GenericContainer<?> z3Prover;

    /**
     * Constructor initializes the Docker container with the Z3 solver.
     * It binds a host directory to a directory in the container to facilitate the transfer of SMT-LIB files.
     * In this case, it is reading from a file each time it needs to solve a problem.
     * As the file generation is dynamical, the Docker container needs to have access to the file system.
     *
     * @param resourcesFolder the path of the host directory to bind to the container
     */
    public Z3DockerExecutor(String resourcesFolder) {

        // Build a Docker image with the specified entrypoint
        ImageFromDockerfile image = new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder -> builder
                        .from(Z3_DOCKER_IMAGE)
                        .entryPoint(ENTRYPOINT)
                        .build());

        // Initialize the Docker container with the specified image and bind the file system
        z3Prover = new GenericContainer<>(image)
                .withFileSystemBind(resourcesFolder, containerPath, BindMode.READ_WRITE);

        // Start the Docker container
        z3Prover.start();
    }

    /**
     * Executes the Z3 solver on an SMT-LIB file located in the container.
     * The file must be in the directory specified by containerPath.
     *
     * @param fileName the name of the SMT-LIB file to read and solve
     * @return a {@link Z3Result} with status SAT (and the solution), UNSAT, UNKNOWN, or ERROR
     */
    public Z3Result solveFromFile(String fileName) {
        return solveFromFile(fileName, 0);
    }

    /**
     * Executes the Z3 solver on an SMT-LIB file located in the container.
     * The file must be in the directory specified by containerPath.
     *
     * @param fileName  the name of the SMT-LIB file to read and solve
     * @param timeoutMs soft per-query timeout in milliseconds. When greater than 0, it is passed
     *                  to Z3 as {@code -t:<ms>}; if exceeded, Z3 returns {@code unknown} for the
     *                  query (mapped to {@link Z3Result.Status#UNKNOWN}) rather than running
     *                  unbounded. A value {@code <= 0} disables the timeout.
     * @return a {@link Z3Result} with status SAT (and the solution), UNSAT, UNKNOWN, or ERROR
     */
    public Z3Result solveFromFile(String fileName, long timeoutMs) {
        try {
            Container.ExecResult result = timeoutMs > 0
                    ? z3Prover.execInContainer("z3", "-t:" + timeoutMs, containerPath + fileName)
                    : z3Prover.execInContainer("z3", containerPath + fileName);

            if (result.getExitCode() != 0) {
                return Z3Result.error("Z3 exited with code " + result.getExitCode()
                        + ": " + result.getStdout() + result.getStderr());
            }

            String stdout = result.getStdout();
            if (stdout == null || stdout.trim().isEmpty()) {
                return Z3Result.error("Z3 produced no output for file: " + fileName
                        + " stderr: " + result.getStderr());
            }

            String trimmed = stdout.trim();
            if (trimmed.startsWith("unsat")) {
                return Z3Result.unsat();
            }
            // "unknown" means Z3 could not decide (e.g. incomplete theory or timeout).
            // It must be handled explicitly: otherwise it would fall through and be parsed
            // as an empty model, silently masquerading as SAT.
            if (trimmed.startsWith("unknown")) {
                return Z3Result.unknown();
            }
            if (!trimmed.startsWith("sat")) {
                return Z3Result.error("Unexpected Z3 output for file " + fileName + ": " + stdout);
            }

            Map<String, SMTLibValue> assignments = SMTResultParser.parseZ3Response(stdout);
            return Z3Result.sat(new MapBasedZ3Solution(assignments));

        } catch (IOException | InterruptedException e) {
            return Z3Result.error("I/O or interruption error running Z3 on " + fileName + ": " + e.getMessage());
        } catch (RuntimeException e) {
            return Z3Result.error("Unexpected error parsing Z3 output for " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Stops the Z3 Docker container and releases any resources held by it.
     */
    @Override
    public void close() {
        if (z3Prover != null) {
            z3Prover.stop();
        }
    }
}
