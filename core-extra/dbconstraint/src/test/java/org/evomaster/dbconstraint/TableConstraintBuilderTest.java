package org.evomaster.dbconstraint;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableConstraintBuilderTest {

    @Test
    public void testBrokenCheckExpression() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn this is not a correct check expression", ConstraintDatabaseType.H2);
        assertTrue(constraint instanceof UnsupportedTableConstraint);
    }


    @Test
    public void testUpperBound() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn <= 100", ConstraintDatabaseType.H2);
        assertTrue(constraint instanceof UpperBoundConstraint);
    }


    @Test
    public void testLowerBound() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn >= 100", ConstraintDatabaseType.H2);
        assertTrue(constraint instanceof LowerBoundConstraint);
    }

    @Test
    public void testEnumConstraint() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn IN ('A','B')", ConstraintDatabaseType.H2);
        assertTrue(constraint instanceof EnumConstraint);
    }

    @Test
    public void testLikeConstraint() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn LIKE 'hello'", ConstraintDatabaseType.POSTGRES);
        assertTrue(constraint instanceof LikeConstraint);
        LikeConstraint likeConstraint = (LikeConstraint) constraint;
        assertEquals("hello", likeConstraint.getPattern());
    }

    @Test
    public void testSimilarTo() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn ~ similar_to('hello',NULL)", ConstraintDatabaseType.POSTGRES);
        assertTrue(constraint instanceof SimilarToConstraint);
        SimilarToConstraint similarToConstraint = (SimilarToConstraint) constraint;
        assertEquals("hello", similarToConstraint.getPattern());
    }


    @Test
    public void testOrConstraint() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn < 10 OR fooColumn >10", ConstraintDatabaseType.H2);
        assertTrue(constraint instanceof OrConstraint);
    }

    @Test
    public void testAndConstraint() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn < 10 AND fooColumn >10", ConstraintDatabaseType.H2);
        assertTrue(constraint instanceof AndConstraint);
    }

    @Test
    public void testRangeConstraint() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn = 10 ", ConstraintDatabaseType.H2);
        assertTrue(constraint instanceof RangeConstraint);
    }

    @Test
    public void testEqualsOfTwoFormulas() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("x", "((STATUS = 'b') = (P_AT IS NOT NULL))", ConstraintDatabaseType.H2);
        assertTrue(constraint instanceof IffConstraint);
        IffConstraint iffConstraint = (IffConstraint) constraint;
        assertTrue(iffConstraint.getLeft() instanceof EnumConstraint);
        assertTrue(iffConstraint.getRight() instanceof IsNotNullConstraint);

        EnumConstraint enumConstraint = (EnumConstraint) iffConstraint.getLeft();
        assertEquals("STATUS", enumConstraint.getColumnName());
        IsNotNullConstraint isNotNullConstraint = (IsNotNullConstraint) iffConstraint.getRight();
        assertEquals("P_AT", isNotNullConstraint.getColumnName());

        assertEquals(Arrays.asList("b"), enumConstraint.getValuesAsStrings());

    }


}
