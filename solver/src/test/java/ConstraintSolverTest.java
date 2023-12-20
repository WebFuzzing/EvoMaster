import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ConstraintSolverTest {

    private static ConstraintSolver solver;

    @BeforeAll
    static void setup() {
        String resourcesFolder = System.getProperty("user.dir") + "/src/test/resources/";
        solver = new ConstraintSolver(resourcesFolder);
    }

    @AfterAll
    static void tearDown() {
        solver.close();
    }

    /**
     * Test satisfiability with a small example
     */
    @Test
    public void satisfiabilityExample() throws IOException, InterruptedException {

        String response = solver.solve("example.smt2");

        assertEquals("sat", response.trim());
    }

    /**
     * Creates a file in runtime and runs z3 with it as parameter
     */
    @Test
    public void dynamicFile() throws IOException {

        String resourcesFolder = System.getProperty("user.dir") + "/src/test/resources/";

        Path originalPath = Paths.get( resourcesFolder + "example.smt2");
        Path copied = Paths.get( resourcesFolder + "example2.smt2");

        Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);

        String response;
        try {
            response = solver.solve("example2.smt2");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            Files.delete(copied);
        }

        assertEquals("sat", response.trim());
    }

    /**
     * Test solving a model with the example of returning two unique unsigned integers
     */
    @Test
    public void unique_uint() throws IOException, InterruptedException {
        String response = solver.solve("unique_uint.smt2");

        assertTrue(response.contains("sat"));
        assertTrue(response.contains("(id_1 2)"));
        assertTrue(response.contains("(id_2 3)"));
    }
}