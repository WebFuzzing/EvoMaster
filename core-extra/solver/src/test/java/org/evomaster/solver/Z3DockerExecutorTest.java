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

        assertEquals(Z3Result.Status.SAT, result.getStatus());
        assertEquals(2, result.getSolution().size());

        assertEquals(new LongValue(0L), result.getSolution().get("y"));
        assertEquals(new LongValue((long) -4), result.getSolution().get("x"));
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

        assertEquals(Z3Result.Status.SAT, result.getStatus());
    }

    /**
     * Test solving a model with the example of returning two unique unsigned integers
     */
    @Test
    public void uniqueUInt() {
        Z3Result result = executor.solveFromFile("unique_uint.smt");

        assertEquals(Z3Result.Status.SAT, result.getStatus(), "Response should be SAT for unique_uint.smt");
        assertEquals(new LongValue(2L), result.getSolution().get("id_1"), "The value for id_1 should be 2");
        assertEquals(new LongValue(3L), result.getSolution().get("id_2"), "The value for id_2 should be 3");
    }

    /**
     * Test solving a model with composed types
     */
    @Test
    public void composedTypes() {
        Z3Result result = executor.solveFromFile("composed_types.smt");

        assertEquals(Z3Result.Status.SAT, result.getStatus(), "Response should be SAT for composed_types.smt");

        assertTrue(result.getSolution().containsKey("users1"), "Response should contain users1");
        assertTrue(result.getSolution().containsKey("users2"), "Response should contain users2");

        // NOTE: assert the individual field VALUES, not StructValue equality: StructValue.equals only
        // compares the set of field names, so a whole-struct assertEquals would pass regardless of the
        // actual values Z3 returned. We assert the fully-constrained fields exactly (NAME, POINTS) and
        // the underconstrained fields against their SMT-LIB constraints (AGE range, distinct IDs).
        StructValue users1 = (StructValue) result.getSolution().get("users1");
        StructValue users2 = (StructValue) result.getSolution().get("users2");

        // NAME and POINTS are pinned by the SMT-LIB (NAME = "Alice"/"Bob", POINTS = 7), so deterministic.
        assertEquals(new StringValue("Alice"), users1.getField("NAME"), "NAME users1");
        assertEquals(new StringValue("Bob"), users2.getField("NAME"), "NAME users2");
        assertEquals(new LongValue(7L), users1.getField("POINTS"), "POINTS users1");
        assertEquals(new LongValue(7L), users2.getField("POINTS"), "POINTS users2");

        // AGE is constrained to (30, 100): assert the constraint holds rather than a specific value.
        long age1 = ((LongValue) users1.getField("AGE")).getValue();
        long age2 = ((LongValue) users2.getField("AGE")).getValue();
        assertTrue(age1 > 30 && age1 < 100, "AGE users1 out of range: " + age1);
        assertTrue(age2 > 30 && age2 < 100, "AGE users2 out of range: " + age2);

        // IDs must be distinct (the 'distinct' assertion over the PK).
        long id1 = ((LongValue) users1.getField("ID")).getValue();
        long id2 = ((LongValue) users2.getField("ID")).getValue();
        assertNotEquals(id1, id2, "user IDs must be distinct");
    }

    /**
     * Test solving with an invalid file to ensure proper error handling
     */
    @Test
    public void whenSolvingInvalidFileItReturnsError() {
        Z3Result result = executor.solveFromFile("invalid.smt");
        assertEquals(Z3Result.Status.ERROR, result.getStatus());
        assertNotNull(result.getErrorMessage());
    }

    /**
     * Test handling an empty file
     */
    @Test
    public void whenSolvingEmptyFileItReturnsError() {
        Z3Result result = executor.solveFromFile("empty.smt");
        assertEquals(Z3Result.Status.ERROR, result.getStatus());
        assertNotNull(result.getErrorMessage());
    }

    /**
     * A hard non-linear problem solved with a small soft timeout: Z3 cannot decide
     * in time and returns 'unknown', which must be reported as UNKNOWN (not SAT).
     */
    @Test
    public void whenTimeoutExceededItReturnsUnknown() {
        Z3Result result = executor.solveFromFile("hard_factoring.smt", 1);
        assertEquals(Z3Result.Status.UNKNOWN, result.getStatus());
        assertNull(result.getSolution());
    }
}
