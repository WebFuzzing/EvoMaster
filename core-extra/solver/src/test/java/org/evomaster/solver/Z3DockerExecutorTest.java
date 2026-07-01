package org.evomaster.solver;

import org.evomaster.solver.smtlib.value.LongValue;
import org.evomaster.solver.smtlib.value.SMTLibValue;
import org.evomaster.solver.smtlib.value.StringValue;
import org.evomaster.solver.smtlib.value.StructValue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Z3DockerExecutorTest {

    private static Z3DockerExecutor executor;

    @BeforeAll
    static void setup() {
        String resourcesFolder = System.getProperty("user.dir") + "/src/test/resources/";
        executor = new Z3DockerExecutor(resourcesFolder);
    }

    @AfterAll
    static void tearDown() {
        executor.close();
    }

    // ************************************************* //
    // ********** Tests for solving from file ********** //
    // ************************************************* //

    /**
     * Test satisfiability with a small example
     */
    @Test
    public void satisfiabilityExample() {
        Z3Result result = executor.solveFromFile("example.smt");

        assertEquals(Z3Result.Status.SAT, result.status);
        assertEquals(2, result.model.size());

        assertEquals(new LongValue(0L), result.model.get("y"));
        assertEquals(new LongValue((long) -4), result.model.get("x"));
    }

    /**
     * Creates a file at runtime and runs z3 with it as a parameter
     */
    @Test
    public void dynamicFile() throws IOException {
        String resourcesFolder = System.getProperty("user.dir") + "/src/test/resources/";

        Path originalPath = Paths.get(resourcesFolder + "example.smt");
        Path copied = Paths.get(resourcesFolder + "example2.smt");

        Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);

        Z3Result result;
        try {
            result = executor.solveFromFile("example2.smt");
        } finally {
            Files.deleteIfExists(copied);
        }

        assertEquals(Z3Result.Status.SAT, result.status);
    }

    /**
     * Test solving a model with the example of returning two unique unsigned integers
     */
    @Test
    public void uniqueUInt() {
        Z3Result result = executor.solveFromFile("unique_uint.smt");

        assertEquals(Z3Result.Status.SAT, result.status, "Response should be SAT for unique_uint.smt");
        assertEquals(new LongValue(2L), result.model.get("id_1"), "The value for id_1 should be 2");
        assertEquals(new LongValue(3L), result.model.get("id_2"), "The value for id_2 should be 3");
    }

    /**
     * Test solving a model with composed types
     */
    @Test
    public void composedTypes() {
        Z3Result result = executor.solveFromFile("composed_types.smt");

        assertEquals(Z3Result.Status.SAT, result.status, "Response should be SAT for composed_types.smt");

        assertTrue(result.model.containsKey("users1"), "Response should contain users1");
        Map<String, SMTLibValue> users1Expected = new HashMap<>();
        users1Expected.put("ID", new LongValue(3L));
        users1Expected.put("NAME", new StringValue("agus"));
        users1Expected.put("AGE", new LongValue(31L));
        users1Expected.put("POINTS", new LongValue(7L));

        assertEquals(new StructValue(users1Expected), result.model.get("users1"), "The value for users1 is incorrect");

        assertTrue(result.model.containsKey("users2"), "Response should contain users2");
        Map<String, SMTLibValue> users2Expected = new HashMap<>();
        users2Expected.put("ID", new LongValue(3L));
        users2Expected.put("NAME", new StringValue("agus"));
        users2Expected.put("AGE", new LongValue(31L));
        users2Expected.put("POINTS", new LongValue(7L));
        assertEquals(new StructValue(users2Expected), result.model.get("users2"), "The value for users2 is incorrect");
    }

    /**
     * Test solving with an invalid file to ensure proper error handling
     */
    @Test
    public void whenSolvingInvalidFileItReturnsError() {
        Z3Result result = executor.solveFromFile("invalid.smt");
        assertEquals(Z3Result.Status.ERROR, result.status);
        assertNotNull(result.errorMessage);
    }

    /**
     * Test handling an empty file
     */
    @Test
    public void whenSolvingEmptyFileItReturnsError() {
        Z3Result result = executor.solveFromFile("empty.smt");
        assertEquals(Z3Result.Status.ERROR, result.status);
        assertNotNull(result.errorMessage);
    }
}
