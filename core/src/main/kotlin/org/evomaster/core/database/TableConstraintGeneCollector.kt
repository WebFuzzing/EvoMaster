package org.evomaster.core.database

import org.evomaster.core.search.gene.Gene
import org.evomaster.dbconstraint.*
import java.util.stream.Collectors

/**
 * Collects all genes that are involved in the evaluation
 * of the constraint.
 */
class TableConstraintGeneCollector()
    : TableConstraintVisitor<Set<Gene>, DbAction> {


    /**
     * Return all genes in left and right constraints
     */
    override fun visit(constraint: AndConstraint, dbAction: DbAction): Set<Gene> {
        val leftGenes = constraint.left.accept(this, dbAction)
        val rightGenes = constraint.right.accept(this, dbAction)
        return leftGenes + rightGenes
    }

    /**
     * Return all genes in all child constraints
     */
    override fun visit(constraint: OrConstraint, dbAction: DbAction): Set<Gene> {
        return constraint.constraintList
                .stream()
                .map {
                    it.accept(this, dbAction)
                }.collect(Collectors.toSet()).flatten().toSet()
    }

    /**
     * Return all genes in left and right constraints
     */
    override fun visit(constraint: IffConstraint, dbAction: DbAction): Set<Gene> {
        val leftGenes = constraint.left.accept(this, dbAction)
        val rightGenes = constraint.right.accept(this, dbAction)
        return leftGenes + rightGenes
    }

    /**
     * Return the gene in the not null constraint
     */
    override fun visit(constraint: IsNotNullConstraint, dbAction: DbAction): Set<Gene> {
        // if the action is not mentioned to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (dbAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return dbAction.seeGenes().filter { it.name == constraint.columnName }.toSet()
    }

    /**
     * Return the gene in the lower bound constraint
     */
    override fun visit(constraint: LowerBoundConstraint, dbAction: DbAction): Set<Gene> {
        // if the action is not mentioned to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (dbAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return dbAction.seeGenes().filter { it.name == constraint.columnName }.toSet()
    }

    /**
     * Return the gene in the upper bound constraint
     */
    override fun visit(constraint: UpperBoundConstraint, dbAction: DbAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (dbAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return dbAction.seeGenes().filter { it.name == constraint.columnName }.toSet()
    }


    /**
     * Return the gene in the range constraint
     */
    override fun visit(constraint: RangeConstraint, dbAction: DbAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (dbAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return dbAction.seeGenes().filter { it.name == constraint.columnName }.toSet()
    }


    /**
     * Return the gene in the enum constraint (if the table name matches)
     */
    override fun visit(constraint: EnumConstraint, dbAction: DbAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (dbAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return dbAction.seeGenes().filter { it.name == constraint.columnName }.toSet()
    }

    /**
     * Return all genes in the unique constraint (if the table name matches)
     */
    override fun visit(constraint: UniqueConstraint, dbAction: DbAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (dbAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name in the unique constraint (none, one or many)
        return dbAction.seeGenes().filter { constraint.uniqueColumnNames.contains(it.name) }.toSet()
    }


    /**
     * Unsupported constraints return no genes
     */
    override fun visit(constraint: UnsupportedTableConstraint, dbAction: DbAction): Set<Gene> {
        return setOf()
    }


    /**
     * Return the gene in the like constraint (if the table name matches)
     */
    override fun visit(constraint: LikeConstraint, dbAction: DbAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (dbAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return dbAction.seeGenes().filter { it.name == constraint.columnName }.toSet()
    }

    /**
     * Return the gene in the similarTo constraint (if the table name matches)
     */
    override fun visit(constraint: SimilarToConstraint, dbAction: DbAction): Set<Gene> {
        // if the action is not referred to this constraint's table, we safely conclude
        // that the genes do not affect the evaluation of this constraint
        if (dbAction.table.name != constraint.tableName) {
            return setOf()
        }
        // return all columns that match the table name/column name (expected: one or none)
        return dbAction.seeGenes().filter { it.name == constraint.columnName }.toSet()
    }


}