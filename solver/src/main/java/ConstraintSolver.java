import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.containers.BindMode;

import java.io.IOException;

public class ConstraintSolver implements AutoCloseable  {

    public static final String Z3_DOCKER_IMAGE = "ghcr.io/z3prover/z3:ubuntu-20.04-bare-z3-sha-ba8d8f0";

    // The default entrypoint runs directly z3, we need to keep it running
    public static final String ENTRYPOINT = "while :; do sleep 1000 ; done";

    private final String containerPath = "/smt2-resources/";
    private final GenericContainer<?>  z3Prover;

    public ConstraintSolver() {

        String resourcesFolder = System.getProperty("user.dir") + "/src/test/resources/";

        ImageFromDockerfile image = new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder -> builder
                        .from(Z3_DOCKER_IMAGE)
                        .entryPoint(ENTRYPOINT)
                        .build());

        z3Prover = new GenericContainer<>(image)
                        .withFileSystemBind(resourcesFolder, containerPath, BindMode.READ_WRITE);

        z3Prover.start();
    }

    @Override
    public void close() {
        z3Prover.stop();
    }

    public String solve(String exampleName) throws IOException, InterruptedException {

        Container.ExecResult result = z3Prover.execInContainer("z3", containerPath + exampleName);

        return result.getStdout(); // TODO: Check stderr
    }


}
