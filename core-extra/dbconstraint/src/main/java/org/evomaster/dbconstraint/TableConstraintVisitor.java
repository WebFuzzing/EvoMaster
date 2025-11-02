package org.evomaster.dbconstraint;

public interface TableConstraintVisitor<K, V> {

    K visit(AndConstraint constraint, V argument);

    K visit(EnumConstraint constraint, V argument);

    K visit(LikeConstraint constraint, V argument);

    K visit(LowerBoundConstraint constraint, V argument);

    K visit(OrConstraint constraint, V argument);

    K visit(RangeConstraint constraint, V argument);

    K visit(SimilarToConstraint constraint, V argument);

    K visit(UniqueConstraint constraint, V argument);

    K visit(UpperBoundConstraint constraint, V argument);

    K visit(UnsupportedTableConstraint constraint, V argument);

    K visit(IffConstraint constraint, V argument);

    K visit(IsNotNullConstraint constraint, V argument);
}
