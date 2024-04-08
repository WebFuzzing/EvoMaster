import com.google.common.collect.ImmutableMap;
import org.evomaster.dbconstraint.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SolverTest {

    private static Solver solver;

    private static List<TableConstraint> productsConstraints = new LinkedList<>();

    @BeforeAll
    static void setup() {
        String resourcesFolder = System.getProperty("user.dir") + "/src/test/resources/";

        storeProductsConstraints();

        solver = new Solver(resourcesFolder);
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
        String model = solver.solveFromFile("example.smt2");

        assertEquals("sat", model.trim());
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
    public void valuesExample() {
        Map<String, String> response = solver.solve(productsConstraints);

        assertEquals(ImmutableMap.of("min_price", "1", "price", "100", "stock", "100", "sec_num", "\"12\"", "lucky_number", "3000"), response);
    }


    private static void storeProductsConstraints() {
        String tableName = "products";

        //  "ALTER TABLE products add CHECK (price>100 AND price<9999);\n"
        RangeConstraint products_price = new RangeConstraint(tableName, "price", 100, 9999);
        productsConstraints.add(products_price);
        //  "ALTER TABLE products add CHECK (min_price>1);\n"
        LowerBoundConstraint products_min_price = new LowerBoundConstraint(tableName, "min_price", 1);
        productsConstraints.add(products_min_price); // TODO: Lower constraints doesnt consider the <= vs <
        //  "ALTER TABLE products add CHECK (stock>=5 OR stock = 100);\n"
        LowerBoundConstraint products_stock_left = new LowerBoundConstraint(tableName, "stock", 5);
        RangeConstraint products_stock_right = new RangeConstraint(tableName, "stock", 100, 100); // TODO: There is no way to say equal
        OrConstraint products_stock = new OrConstraint(tableName, products_stock_left, products_stock_right);
        productsConstraints.add(products_stock);
        //  "ALTER TABLE users add CHECK (lucky_number<3000);\n"
        UpperBoundConstraint products_lucky_number = new UpperBoundConstraint(tableName, "lucky_number", 3000);
        productsConstraints.add(products_lucky_number);
        //  "ALTER TABLE users add CHECK (sec_num IN (12, 13, 14));\n"
        EnumConstraint products_sec_num = new EnumConstraint(tableName, "sec_num", asList("12", "13", "14"));
        productsConstraints.add(products_sec_num);
    }

}
