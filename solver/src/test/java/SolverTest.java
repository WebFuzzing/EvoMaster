import org.evomaster.dbconstraint.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SolverTest {

    private static String tableName = "products";

    private static Solver solver;

    private static List<TableConstraint> productsConstraints = new LinkedList<>();
    private static RowToSolve row;

    @BeforeAll
    static void setup() {
        String resourcesFolder = System.getProperty("user.dir") + "/src/test/resources/";

        storeProductsConstraints();
        loadRow();

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

        Path originalPath = Paths.get(resourcesFolder + "example.smt2");
        Path copied = Paths.get(resourcesFolder + "example2.smt2");

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
        Map<String, RowWithValues> response = solver.solve(Collections.singletonList(row), productsConstraints);

        RowWithValues row = response.get(tableName);

        assertEquals(new IntSolvedValue(1), row.getColumn("min_price"));
        assertEquals(new IntSolvedValue(100), row.getColumn("price"));
        assertEquals(new IntSolvedValue(100), row.getColumn("stock"));
        assertEquals(new IntSolvedValue(3000), row.getColumn("lucky_number"));
    }

    /**
     * SELECT * FROM PRODUCTS;
     */
    @Test
    public void select_all_from() {

        List<TableConstraint> queryConstraints = new LinkedList<>();

        // As we are selecting all columns, we need to add constraints for all columns
        // We are forcing them to be not null
        queryConstraints.add(new IsNotNullConstraint(tableName, "min_price"));
        queryConstraints.add(new IsNotNullConstraint(tableName, "price"));
        queryConstraints.add(new IsNotNullConstraint(tableName, "stock"));
        queryConstraints.add(new IsNotNullConstraint(tableName, "sec_num"));
        queryConstraints.add(new IsNotNullConstraint(tableName, "lucky_number"));

        String text = solver.parseConstraintsToSmtText(Collections.singletonList(row), queryConstraints);

        assertEquals(
                "(set-logic QF_SLIA)\n" +
                        "(declare-fun products__stock () Int)\n" +
                        "(declare-fun products__lucky_number () Int)\n" +
                        "(declare-fun products__min_price () Int)\n" +
                        "(declare-fun products__price () Int)\n" +
                        "(declare-fun products__sec_num () Int)\n" +
                        "(check-sat)\n" +
                        "(get-value (products__stock))\n" +
                        "(get-value (products__lucky_number))\n" +
                        "(get-value (products__min_price))\n" +
                        "(get-value (products__price))\n" +
                        "(get-value (products__sec_num))\n", text);


        Map<String, RowWithValues> values = solver.solve(Collections.singletonList(row), queryConstraints);

        assertEquals(1, values.size());
        RowWithValues solved = values.get(tableName);

        assertEquals(new IntSolvedValue(0), solved.getColumn("stock"));
        assertEquals(new IntSolvedValue(0), solved.getColumn("lucky_number"));
        assertEquals(new IntSolvedValue(0), solved.getColumn("min_price"));
        assertEquals(new IntSolvedValue(0), solved.getColumn("price"));
        assertEquals(new IntSolvedValue(0), solved.getColumn("sec_num"));
    }


    /**
     * SELECT * FROM PRODUCTS;
     * + Table constraints
     */
    @Test
    public void select_all_from_with_schema() {
        String tableName = "products";
        List<TableConstraint> queryConstraints = new LinkedList<>();

        // As we are selecting all columns, we need to add constraints for all columns
        // We are forcing them to be not null
        queryConstraints.add(new IsNotNullConstraint(tableName, "min_price"));
        queryConstraints.add(new IsNotNullConstraint(tableName, "price"));
        queryConstraints.add(new IsNotNullConstraint(tableName, "stock"));
        queryConstraints.add(new IsNotNullConstraint(tableName, "sec_num"));
        queryConstraints.add(new IsNotNullConstraint(tableName, "lucky_number"));

        // Add to the query constraints the table constraints
        queryConstraints.addAll(productsConstraints);

        String text = solver.parseConstraintsToSmtText(Collections.singletonList(row), queryConstraints);

        assertEquals(
                "(set-logic QF_SLIA)\n" +
                        "(declare-fun products__stock () Int)\n" +
                        "(declare-fun products__lucky_number () Int)\n" +
                        "(declare-fun products__min_price () Int)\n" +
                        "(declare-fun products__price () Int)\n" +
                        "(declare-fun products__sec_num () Int)\n" +
                        "(assert (and (>= products__price 100) (<= products__price 9999)))\n" +
                        "(assert (<= products__min_price 1))\n" +
                        "(assert (or (<= products__stock 5) (and (>= products__stock 100) (<= products__stock 100))))\n" +
                        "(assert (>= products__lucky_number 3000))\n" +
//                        "(assert (or (= products__sec_num 12) (or (= products__sec_num 13) (= products__sec_num 14))))\n" +
                        "(check-sat)\n" +
                        "(get-value (products__stock))\n" +
                        "(get-value (products__lucky_number))\n" +
                        "(get-value (products__min_price))\n" +
                        "(get-value (products__price))\n" +
                        "(get-value (products__sec_num))\n", text);


        Map<String, RowWithValues> values = solver.solve(Collections.singletonList(row), queryConstraints);

        assertEquals(1, values.size());
        RowWithValues solved = values.get(tableName);

        assertEquals(new IntSolvedValue(100), solved.getColumn("stock"));
        assertEquals(new IntSolvedValue(3000), solved.getColumn("lucky_number"));
        assertEquals(new IntSolvedValue(1), solved.getColumn("min_price"));
        assertEquals(new IntSolvedValue(100), solved.getColumn("price"));
        assertEquals(new IntSolvedValue(0), solved.getColumn("sec_num"));
    }
    private static void storeProductsConstraints() {

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
//        EnumConstraint products_sec_num = new EnumConstraint(tableName, "sec_num", asList("12", "13", "14"));
//        productsConstraints.add(products_sec_num);
    }

    private static void loadRow() {
        Map<String, RowValueType> columns = new HashMap<>();
        columns.put("min_price", RowValueType.INT);
        columns.put("price", RowValueType.INT);
        columns.put("stock", RowValueType.INT);
        columns.put("sec_num", RowValueType.INT);
        columns.put("lucky_number", RowValueType.INT);

        row = new RowToSolve(tableName, columns);
    }

}
