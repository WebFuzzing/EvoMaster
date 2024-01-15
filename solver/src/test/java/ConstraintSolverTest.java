import org.evomaster.client.java.sql.internal.constraint.DbTableCheckExpression;
import org.evomaster.client.java.sql.internal.constraint.DbTableConstraint;
import org.evomaster.core.search.gene.Gene;
import org.evomaster.core.search.gene.numeric.IntegerGene;
import org.evomaster.core.sql.SqlAction;
import org.evomaster.core.sql.schema.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;


public class ConstraintSolverTest {

    private static DbConstraintSolverZ3InDocker solver;

    @BeforeAll
    static void setup() {
        String resourcesFolder = System.getProperty("user.dir") + "/src/test/resources/";
        solver = new DbConstraintSolverZ3InDocker(resourcesFolder);
    }

    @AfterAll
    static void tearDown() {
        solver.close();
    }


    // ************************************************* //
    // ********** Tests for solving from file ********** //
    // ************************************************* //

    /**
     * Test satisfiability with a small example
     */
    @Test
    public void satisfiabilityExample() {

        String response = solver.solveFromFile("example.smt2");

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
            response = solver.solveFromFile("example2.smt2");
        } finally {
            Files.delete(copied);
        }

        assertEquals("sat", response.trim());
    }

    /**
     * Test solving a model with the example of returning two unique unsigned integers
     */
    @Test
    public void unique_uint() {
        String response = solver.solveFromFile("unique_uint.smt2");

        assertTrue(response.contains("sat"));
        assertTrue(response.contains("(id_1 2)"));
        assertTrue(response.contains("(id_2 3)"));
    }

    // **************************************************************** //
    // ********** Tests for creating the file and then solve ********** //
    // **************************************************************** //

    @Test
    public void fromConstraintList() {
        Table table = new Table("products", emptySet(), emptySet(), emptySet());
        List<DbTableConstraint> constraintList = Collections.singletonList(
                new DbTableCheckExpression("products", "CHECK (price>100)"));

        List<SqlAction> response = solver.solve(table, constraintList);

        SqlAction action = response.get(0);
        assertEquals("price", action.getName());
        Gene gene = action.seeTopGenes().get(0);

        if (gene instanceof IntegerGene) {
            assertEquals(101, ((IntegerGene) gene).getValue());
//            assertEquals(101, ((IntegerGene) response).getMin());
//            assertEquals(101, ((IntegerGene) response).getMaximum());
//            assertEquals(101, ((IntegerGene) response).getMaximum());
            assertFalse(((IntegerGene) gene).getMinInclusive());
            assertFalse(((IntegerGene) gene).getMaxInclusive());
        } else {
            fail("The response is not an IntegerGene");
        }
    }
}