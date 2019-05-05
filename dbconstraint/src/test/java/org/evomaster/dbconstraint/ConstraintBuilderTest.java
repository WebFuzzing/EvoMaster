package org.evomaster.dbconstraint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConstraintBuilderTest {

    @Test
    public void testBrokenCheckExpression() {
        ConstraintBuilder builder = new ConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn this is not a correct check expression");
        assertTrue(constraint instanceof UnsupportedTableConstraint);
    }


    @Test
    public void testUpperBound() {
        ConstraintBuilder builder = new ConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn <= 100");
        assertTrue(constraint instanceof UpperBoundConstraint);
    }


    @Test
    public void testLowerBound() {
        ConstraintBuilder builder = new ConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn >= 100");
        assertTrue(constraint instanceof LowerBoundConstraint);
    }

    @Test
    public void testEnumConstraint() {
        ConstraintBuilder builder = new ConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn IN ('A','B')");
        assertTrue(constraint instanceof EnumConstraint);
    }

    @Test
    public void testLikeConstraint() {
        ConstraintBuilder builder = new ConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn LIKE 'hello'");
        assertTrue(constraint instanceof LikeConstraint);
    }

    @Test
    public void testSimilarTo() {
        ConstraintBuilder builder = new ConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn ~ similar_to('hello',NULL)");
        assertTrue(constraint instanceof SimilarToConstraint);
    }


    @Test
    public void testOrConstraint() {
        ConstraintBuilder builder = new ConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn < 10 OR fooColumn >10");
        assertTrue(constraint instanceof OrConstraint);
    }

    @Test
    public void testAndConstraint() {
        ConstraintBuilder builder = new ConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn < 10 AND fooColumn >10");
        assertTrue(constraint instanceof AndConstraint);
    }

    @Test
    public void testRangeConstraint() {
        ConstraintBuilder builder = new ConstraintBuilder();
        TableConstraint constraint = builder.translateToConstraint("fooTable", "fooColumn = 10 ");
        assertTrue(constraint instanceof RangeConstraint);
    }
}
