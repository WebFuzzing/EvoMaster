package org.evomaster.core.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.dbconstraint.*
import java.util.stream.Collectors

/**
 * Collects all genes that are involved in the evaluation
 * of the constraint.
 */
class TableConstraintGeneCollector()
    : TableConstraintVisitor<Set<Gene>, SqlAction> {


    /**
     * Return all genes in left and right constraints
     */
    override fun visit(constraint: AndConstraint, sqlAction: SqlAction): Set<Gene> {
        val leftGenes = constraint.left.accept(this, sqlAction)
        val rightGenes = constraint.right.accept(this, sqlAction)
        return leftGenes + rightGenes
    }

    /**
     * Return all genes in all child constraints
     */
    override fun visit(constraint: OrConstraint, sqlAction: SqlAction): Set<Gene> {
        return constraint.constraintList
                .stream()
                .map {
                    it.accept(this, sqlAction)
                }.collect(Collectors.toSet()).flatten().toSet()
    }

    /**
     * Return all genes in left and right constraints
     */
    override fun visit(constraint: IffConstraint, sqlAction: SqlAction): Set<Gene> {
        val leftGenes = constraint.left.accept(this, sqlAction)
        val rightGenes = constraint.right.accept(this, sqlAction)
        return leftGenes + rightGenes
    }

    /**
     * Return the gene in the not null constraint
     */
    override fun visit(constraint: IsNotNullConstraint, sqlAction: SqlAction): Set<Gene> {
        // if the action is not mentioned to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (sqlAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return sqlAction.seeTopGenes().filter { it.name == constraint.columnName }.toSet()
    }

    /**
     * Return the gene in the lower bound constraint
     */
    override fun visit(constraint: LowerBoundConstraint, sqlAction: SqlAction): Set<Gene> {
        // if the action is not mentioned to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (sqlAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return sqlAction.seeTopGenes().filter { it.name == constraint.columnName }.toSet()
    }

    /**
     * Return the gene in the upper bound constraint
     */
    override fun visit(constraint: UpperBoundConstraint, sqlAction: SqlAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (sqlAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return sqlAction.seeTopGenes().filter { it.name == constraint.columnName }.toSet()
    }


    /**
     * Return the gene in the range constraint
     */
    override fun visit(constraint: RangeConstraint, sqlAction: SqlAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (sqlAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return sqlAction.seeTopGenes().filter { it.name == constraint.columnName }.toSet()
    }


    /**
     * Return the gene in the enum constraint (if the table name matches)
     */
    override fun visit(constraint: EnumConstraint, sqlAction: SqlAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (sqlAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return sqlAction.seeTopGenes().filter { it.name == constraint.columnName }.toSet()
    }

    /**
     * Return all genes in the unique constraint (if the table name matches)
     */
    override fun visit(constraint: UniqueConstraint, sqlAction: SqlAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (sqlAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name in the unique constraint (none, one or many)
        return sqlAction.seeTopGenes().filter { constraint.uniqueColumnNames.contains(it.name) }.toSet()
    }


    /**
     * Unsupported constraints return no genes
     */
    override fun visit(constraint: UnsupportedTableConstraint, sqlAction: SqlAction): Set<Gene> {
        return setOf()
    }


    /**
     * Return the gene in the like constraint (if the table name matches)
     */
    override fun visit(constraint: LikeConstraint, sqlAction: SqlAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (sqlAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return sqlAction.seeTopGenes().filter { it.name == constraint.columnName }.toSet()
    }

    /**
     * Return the gene in the similarTo constraint (if the table name matches)
     */
    override fun visit(constraint: SimilarToConstraint, sqlAction: SqlAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (sqlAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return sqlAction.seeTopGenes().filter { it.name == constraint.columnName }.toSet()
    }


}