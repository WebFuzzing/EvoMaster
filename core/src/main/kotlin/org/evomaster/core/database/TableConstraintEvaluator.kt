package org.evomaster.core.database

import org.evomaster.dbconstraint.*

class TableConstraintEvaluator : TableConstraintVisitor<Boolean, RowColumnValues> {

    override fun visit(constraint: AndConstraint, rowColumnValues: RowColumnValues): Boolean {
        val leftValue = constraint.left.accept(this, rowColumnValues)
        val rightValue = constraint.right.accept(this, rowColumnValues)
        return leftValue && rightValue
    }

    override fun visit(constraint: EnumConstraint, rowColumnValues: RowColumnValues): Boolean {
        val currentValue = rowColumnValues[constraint.tableName, constraint.columnName]
        if (currentValue == null)
            return false

        return constraint.valuesAsStrings
                .stream()
                .anyMatch {
                    it.equals(currentValue)
                }

    }

    override fun visit(constraint: LikeConstraint, rowColumnValues: RowColumnValues): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(constraint: LowerBoundConstraint, rowColumnValues: RowColumnValues): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(constraint: OrConstraint, rowColumnValues: RowColumnValues): Boolean {
        return constraint.constraintList
                .stream()
                .anyMatch {
                    it.accept(this, rowColumnValues)
                }
    }

    override fun visit(constraint: RangeConstraint, rowColumnValues: RowColumnValues): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(constraint: SimilarToConstraint, rowColumnValues: RowColumnValues): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(constraint: UniqueConstraint, rowColumnValues: RowColumnValues): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(constraint: UpperBoundConstraint, rowColumnValues: RowColumnValues): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(constraint: UnsupportedTableConstraint, rowColumnValues: RowColumnValues): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(constraint: IffConstraint, rowColumnValues: RowColumnValues): Boolean {
        val leftValue = constraint.left.accept(this, rowColumnValues)
        val rightValue = constraint.right.accept(this, rowColumnValues)
        return leftValue == rightValue
    }

    override fun visit(constraint: IsNotNullConstraint, rowColumnValues: RowColumnValues): Boolean = rowColumnValues[constraint.tableName, constraint.columnName] != null

}