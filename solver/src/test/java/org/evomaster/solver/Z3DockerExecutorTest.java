package org.evomaster.solver;

import org.evomaster.solver.smtlib.value.IntValue;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
    }

    /**
     * Creates a file in runtime and runs z3 with it as parameter
     */
    @Test
    public void dynamicFile() throws IOException {

        String resourcesFolder = System.getProperty("user.dir") + "/src/test/resources/";

        Path originalPath = Paths.get( resourcesFolder + "example.smt");
        Path copied = Paths.get( resourcesFolder + "example2.smt");

        Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);

        Optional<Map<String, SMTLibValue>> response;
        try {
            response = executor.solveFromFile("example2.smt");
        } finally {
            Files.delete(copied);
        }

        assertTrue(response.isPresent());
    }

    /**
     * Test solving a model with the example of returning two unique unsigned integers
     */
    @Test
    public void unique_uint() {
        Optional<Map<String, SMTLibValue>> response = executor.solveFromFile("unique_uint.smt");

        assertTrue(response.isPresent());
        assertTrue(response.get().containsKey("id_1"));
        assertEquals(new IntValue(2), response.get().get("id_1"));
        assertTrue(response.get().containsKey("id_2"));
        assertEquals(new IntValue(3), response.get().get("id_2"));
    }

    /**
     * Test solving a model with composed types, it assumes that the constructor follows the sintax of:
     * field1-field2-field3 value1 value2 value3
     * In this example the struct is defined by:
     * (declare-datatypes () ((UsersRow (id-name-age-points (ID Int) (NAME String) (AGE Int) (POINTS Int) ))))
     * And the response is:
     * ((users1 (id-name-age-points 3 "agus" 31 7)))
     */
    @Test
    public void composedTypes() {
        Optional<Map<String, SMTLibValue>> response = executor.solveFromFile("composed_types.smt");

        assertTrue(response.isPresent());

        assertTrue(response.get().containsKey("users1"));
        Map<String, SMTLibValue> users1Expected = new java.util.HashMap<>();
        users1Expected.put("ID", new IntValue(3));
        users1Expected.put("NAME", new StringValue("agus"));
        users1Expected.put("AGE", new IntValue(31));
        users1Expected.put("POINTS", new IntValue(7));
        assertEquals(new StructValue(users1Expected), response.get().get("users1"));

        assertTrue(response.get().containsKey("users2"));
        Map<String, SMTLibValue> users2Expected = new java.util.HashMap<>();
        users2Expected.put("ID", new IntValue(3));
        users2Expected.put("NAME", new StringValue("agus"));
        users2Expected.put("AGE", new IntValue(31));
        users2Expected.put("POINTS", new IntValue(7));
        assertEquals(new StructValue(users2Expected), response.get().get("users2"));
    }



}