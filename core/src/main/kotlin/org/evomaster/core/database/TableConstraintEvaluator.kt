package org.evomaster.core.database

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.NumberGene
import org.evomaster.core.search.gene.sql.SqlNullable
import org.evomaster.dbconstraint.*
import java.util.regex.Pattern

/**
 * Evaluates if a given dbAction satisfies or not a given table constraint.
 * The evaluation could depend on previous dbActions (i.e. uniqueness, etc.).
 * The evaluator expects that the database is initially empty (only the previous actions
 * are considered for the evaluation).
 */
class TableConstraintEvaluator(val previousActions: List<DbAction> = listOf())
    : TableConstraintVisitor<Boolean, DbAction> {


    /**
     * Evaluates to true when both left and right expressions are evaluated to true
     */
    override fun visit(constraint: AndConstraint, dbAction: DbAction): Boolean {
        val leftValue = constraint.left.accept(this, dbAction)
        val rightValue = constraint.right.accept(this, dbAction)
        return leftValue && rightValue
    }

    /**
     * Evaluates to true when both any of the OR-expressions is evaluated to true
     */
    override fun visit(constraint: OrConstraint, dbAction: DbAction): Boolean {
        return constraint.constraintList
                .stream()
                .anyMatch {
                    it.accept(this, dbAction)
                }
    }

    /**
     * Evaluates to true when both left and right have the same evaluated value (i.e.
     * both evaluate to true or both evaluate to false)
     */
    override fun visit(constraint: IffConstraint, dbAction: DbAction): Boolean {
        val leftValue = constraint.left.accept(this, dbAction)
        val rightValue = constraint.right.accept(this, dbAction)
        return leftValue == rightValue
    }

    /**
     * Evaluates to true if the column value is different than NULL
     */
    override fun visit(constraint: IsNotNullConstraint, dbAction: DbAction): Boolean {
        // if the action is not referred to this action, we conclude
        // the action does not invalidate the constraint
        if (dbAction.table.name != constraint.tableName) {
            return true
        }

        // get the gene with the corresponding column name
        val gene = dbAction.seeGenes().firstOrNull { it.name == constraint.columnName }


        val isPresent = when (gene) {
            // if no gene is found, consider the value to be null
            null -> false
            else -> if (gene is SqlNullable) {
                // if the gene is a nullable gen, the value
                // could be null if isPresent==true
                gene.isPresent
            } else {
                // if the gene is not a nullable gene, then
                // its value is not null
                true
            }
        }
        return isPresent
    }

    /**
     * Evaluates to true if the column value is not null and the column value is a
     * numerica value that is greater than the lower bound value
     */
    override fun visit(constraint: LowerBoundConstraint, dbAction: DbAction): Boolean {
        // if the action is not referred to this action, we conclude
        // the action does not invalidate the constraint
        if (dbAction.table.name != constraint.tableName) {
            return true
        }
        val columnName = constraint.columnName

        // if the column has no value in this action, we will assume it
        // will be NULL as default value. Therefore, the range is not satisfied
        // However, it is possible to specify DEFAULT values in the
        // TODO: Handle DEFAULT column values different than NULL
        val gene = dbAction.seeGenes().firstOrNull { it.name == constraint.columnName } ?: return false
        val numberGene = gene.flatView().filterIsInstance<NumberGene<*>>().first()

        return constraint.lowerBound <= numberGene.toLong()
    }

    /**
     * Evaluates to true if the column value is lesser or equal to the constant value
     */
    override fun visit(constraint: UpperBoundConstraint, dbAction: DbAction): Boolean {
        // if the action is not referred to this action, we conclude
        // the action does not invalidate the constraint
        if (dbAction.table.name != constraint.tableName) {
            return true
        }
        val columnName = constraint.columnName

        // if the column has no value in this action, we will assume it
        // will be NULL as default value. Therefore, the range is not satisfied
        // However, it is possible to specify DEFAULT values in the
        // TODO: Handle DEFAULT column values different than NULL
        val gene = dbAction.seeGenes().firstOrNull { it.name == constraint.columnName } ?: return false
        val numberGene = gene.flatView().filterIsInstance<NumberGene<*>>().first()

        return numberGene.toLong() <= constraint.upperBound
    }


    /**
     * Evaluates the table.column in range [minValue,maxValue]
     */
    override fun visit(constraint: RangeConstraint, dbAction: DbAction): Boolean {
        // if the action is not referred to this action, we conclude
        // the action does not invalidate the constraint
        if (dbAction.table.name != constraint.tableName) {
            return true
        }
        val columnName = constraint.columnName

        // if the column has no value in this action, we will assume it
        // will be NULL as default value. Therefore, the range is not satisfied
        // However, it is possible to specify DEFAULT values in the
        // TODO: Handle DEFAULT column values different than NULL
        val gene = dbAction.seeGenes().firstOrNull { it.name == constraint.columnName } ?: return false
        val numberGene = gene.flatView().filterIsInstance<NumberGene<*>>().first()

        return constraint.minValue <= numberGene.toLong()
                && numberGene.toLong() <= constraint.maxValue
    }


    /**
     * Evaluates to true when the column value is not null and matches any of
     * the enumerated values
     */
    override fun visit(constraint: EnumConstraint, dbAction: DbAction): Boolean {
        // if the action is not referred to this action, we conclude
        // the action does not invalidate the constraint
        if (dbAction.table.name != constraint.tableName) {
            return true
        }
        val gene = dbAction.seeGenes().firstOrNull { it.name == constraint.columnName } ?: return false
        val rawString = gene.getValueAsRawString()

        return constraint.valuesAsStrings.contains(rawString)
    }

    /**
     * Checks that all the previous values to be stored in that column
     * are unique with respect to this value. Includes partial support
     * for multi-column unique constraints
     */
    override fun visit(constraint: UniqueConstraint, dbAction: DbAction): Boolean {
        val tableName = constraint.tableName

        val tuples = mutableListOf<MutableMap<String, String?>>()
        val allActions = this.previousActions + dbAction
        for (previousAction in allActions) {
            if (previousAction.table.name != tableName) {
                // if the action is not related to this action, ignore the action
                continue
            }
            val tuple = getTuple(constraint.uniqueColumnNames, previousAction.seeGenes())
            if (tuple in tuples) {
                // if the tuple was already observed, return false
                return false
            }
            tuples.add(tuple)
        }
        // none of the tuple was repeated, therefore, uniqueness is preserved
        return true
    }

    /**
     * Returns a string representation of each value for the set of columns
     */
    private fun getTuple(columnNames: List<String>, genes: List<Gene>): MutableMap<String, String?> {
        val tuple = mutableMapOf<String, String?>()
        for (uniqueColumnName in columnNames) {
            val gene = genes.firstOrNull { it.name == uniqueColumnName }
            if (gene == null) {
                // if the column is not listed then we have to assume the default value
                // for that column. Momentarly we will use NULL as default value for all
                // columns not listed.
                tuple[uniqueColumnName] = null
            } else {
                val rawString = gene.getValueAsRawString()
                tuple[uniqueColumnName] = rawString
            }
        }
        return tuple
    }

    /**
     * Unsupported constraints are true by default
     */
    override fun visit(constraint: UnsupportedTableConstraint, dbAction: DbAction): Boolean {
        return true
    }


    /**
     * Evaluates to true if the column value is not null and the non-null value
     * matches the LIKE pattern. Different Database Types (e.g. H2, POSTGRES, etc)
     * might have different pattern matching behaviour.
     */
    override fun visit(constraint: LikeConstraint, dbAction: DbAction): Boolean {
        val tableName = constraint.tableName
        val columnName = constraint.columnName
        val databaseType = constraint.databaseType
        val patternDb = constraint.pattern

        // if the action is not referred to this action, we conclude
        // the action does not invalidate the constraint
        if (dbAction.table.name != tableName) {
            return true
        }


        val gene = dbAction.seeGenes().firstOrNull { it.name == columnName } ?: return false
        val instance = gene.getValueAsRawString()

        val javaRegexPattern = when (databaseType) {
            ConstraintDatabaseType.POSTGRES -> PostgresToJavaRegExTranslator().translatePostgresLike(patternDb)
            else -> throw UnsupportedOperationException("Must implement java regex translation from %s".format(databaseType))
        }
        val pattern = Pattern.compile(javaRegexPattern)
        val matcher = pattern.matcher(instance)

        return matcher.find()
    }


    override fun visit(constraint: SimilarToConstraint, dbAction: DbAction): Boolean {
        val tableName = constraint.tableName
        val columnName = constraint.columnName
        val databaseType = constraint.databaseType
        val patternDb = constraint.pattern

        // if the action is not referred to this action, we conclude
        // the action does not invalidate the constraint
        if (dbAction.table.name != tableName) {
            return true
        }

        val gene = dbAction.seeGenes().firstOrNull { it.name == columnName } ?: return false
        val instance = gene.getValueAsRawString()

        val javaRegexPattern = when (databaseType) {
            ConstraintDatabaseType.POSTGRES -> PostgresToJavaRegExTranslator().translatePostgresSimilarTo(patternDb)
            else -> throw UnsupportedOperationException("Must implement java regex translation from %s".format(databaseType))
        }
        val pattern = Pattern.compile(javaRegexPattern)
        val matcher = pattern.matcher(instance)

        return matcher.find()
    }


}