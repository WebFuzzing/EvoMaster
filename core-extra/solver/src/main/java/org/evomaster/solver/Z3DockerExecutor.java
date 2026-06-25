package org.evomaster.solver;

import org.evomaster.solver.smtlib.SMTResultParser;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;

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
     *
     * @param fileName the name of the SMT-LIB file to read and solve
     * @return Z3Result.sat(model) if satisfiable, Z3Result.unsat() if unsatisfiable,
     *         or Z3Result.error(message) on any failure
     */
    public Z3Result solveFromFile(String fileName) {
        try {
            Container.ExecResult result = z3Prover.execInContainer("z3", containerPath + fileName);
            if (result.getExitCode() != 0) {
                return Z3Result.error("Error executing Z3 solver: \n" + result.getStdout() + "\n" + result.getStderr());
            }
            String stdout = result.getStdout();
            if (stdout == null || stdout.isEmpty()) {
                return Z3Result.error("No result after solving file: " + result.getStderr());
            }
            if (stdout.trim().startsWith("unsat")) {
                return Z3Result.unsat();
            }
            return Z3Result.sat(SMTResultParser.parseZ3Response(stdout));
        } catch (IOException | InterruptedException e) {
            return Z3Result.error("I/O or interrupt error: " + e.getMessage());
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
