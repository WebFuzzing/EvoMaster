package org.evomaster.solver;

import org.evomaster.solver.smtlib.value.IntValue;
import org.evomaster.solver.smtlib.value.SMTLibValue;
import org.evomaster.solver.smtlib.value.StringValue;
import org.evomaster.solver.smtlib.value.StructValue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;

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
        Optional<Map<String, SMTLibValue>> response = executor.solveFromFile("example.smt");

        assertTrue(response.isPresent());
        assertEquals(2, response.get().size());

        assertEquals(new IntValue(0), response.get().get("y"));
        assertEquals(new IntValue(-4), response.get().get("x"));
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

        Optional<Map<String, SMTLibValue>> response;
        try {
            response = executor.solveFromFile("example2.smt");
        } finally {
            Files.deleteIfExists(copied); // Ensure the file is deleted
        }

        assertTrue(response.isPresent());
    }

    /**
     * Test solving a model with the example of returning two unique unsigned integers
     */
    @Test
    public void uniqueUInt() {
        Optional<Map<String, SMTLibValue>> response = executor.solveFromFile("unique_uint.smt");

        assertTrue(response.isPresent(), "Response should be present for unique_uint.smt");
        assertEquals(new IntValue(2), response.get().get("id_1"), "The value for id_1 should be 2");
        assertEquals(new IntValue(3), response.get().get("id_2"), "The value for id_2 should be 3");
    }

    /**
     * Test solving a model with composed types
     */
    @Test
    public void composedTypes() {
        Optional<Map<String, SMTLibValue>> response = executor.solveFromFile("composed_types.smt");

        assertTrue(response.isPresent(), "Response should be present for composed_types.smt");

        assertTrue(response.get().containsKey("users1"), "Response should contain users1");
        Map<String, SMTLibValue> users1Expected = ImmutableMap.of(
                "ID", new IntValue(3),
                "NAME", new StringValue("agus"),
                "AGE", new IntValue(31),
                "POINTS", new IntValue(7)
        );
        assertEquals(new StructValue(users1Expected), response.get().get("users1"), "The value for users1 is incorrect");

        assertTrue(response.get().containsKey("users2"), "Response should contain users2");
        Map<String, SMTLibValue> users2Expected = ImmutableMap.of(
                "ID", new IntValue(3),
                "NAME", new StringValue("agus"),
                "AGE", new IntValue(31),
                "POINTS", new IntValue(7)
        );
        assertEquals(new StructValue(users2Expected), response.get().get("users2"), "The value for users2 is incorrect");
    }

    /**
     * Test solving with an invalid file to ensure proper error handling
     */
    @Test
    public void invalidFile() {
        try {
            executor.solveFromFile("invalid.smt");
        } catch (Exception e) {
            assertEquals("No result after solving file ", e.getMessage(), "Exception message should be 'error: file is empty'");
        }
    }

    /**
     * Test handling an empty file
     */
    @Test
    public void emptyFile() {
        try {
            executor.solveFromFile("empty.smt");
        } catch (Exception e) {
            assertEquals("No result after solving file ", e.getMessage(), "Exception message should be 'error: file is empty'");
        }
    }
}
