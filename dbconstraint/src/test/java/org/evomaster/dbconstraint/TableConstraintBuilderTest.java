package org.evomaster.dbconstraint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableConstraintBuilderTest {

    @Test
    public void testBrokenCheckExpression() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn this is not a correct check expression");
        assertTrue(constraint instanceof UnsupportedTableConstraint);
    }


    @Test
    public void testUpperBound() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn <= 100");
        assertTrue(constraint instanceof UpperBoundConstraint);
    }


    @Test
    public void testLowerBound() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn >= 100");
        assertTrue(constraint instanceof LowerBoundConstraint);
    }

    @Test
    public void testEnumConstraint() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn IN ('A','B')");
        assertTrue(constraint instanceof EnumConstraint);
    }

    @Test
    public void testLikeConstraint() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn LIKE 'hello'");
        assertTrue(constraint instanceof LikeConstraint);
        LikeConstraint likeConstraint = (LikeConstraint) constraint;
        assertEquals("hello", likeConstraint.getPattern());
    }

    @Test
    public void testSimilarTo() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn ~ similar_to('hello',NULL)");
        assertTrue(constraint instanceof SimilarToConstraint);
        SimilarToConstraint similarToConstraint = (SimilarToConstraint) constraint;
        assertEquals("hello", similarToConstraint.getPattern());
    }


    @Test
    public void testOrConstraint() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn < 10 OR fooColumn >10");
        assertTrue(constraint instanceof OrConstraint);
    }

    @Test
    public void testAndConstraint() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn < 10 AND fooColumn >10");
        assertTrue(constraint instanceof AndConstraint);
    }

    @Test
    public void testRangeConstraint() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn = 10 ");
        assertTrue(constraint instanceof RangeConstraint);
    }

    @Test
    public void testEqualsOfTwoFormulas() {
        TableConstraintBuilder builder = new TableConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("x", "((STATUS = 'b') = (P_AT IS NOT NULL))");
        assertFalse(constraint instanceof UnsupportedTableConstraint);
        assertTrue(constraint instanceof IffConstraint);
    }


}
