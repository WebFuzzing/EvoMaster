package org.evomaster.core.sql

import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionEntryDto
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.sql.SqlWrapperGene


object SqlActionTransformer {

    /**
     * @param sqlIdMap is a map from Insertion Id to generated Id in database
     */
    fun transform(insertions: List<SqlAction>, sqlIdMap : Map<Long, Long> = mapOf(), previousSqlActions: MutableList<SqlAction> = mutableListOf()) : DatabaseCommandDto {

        val list = mutableListOf<InsertionDto>()
        val previous = mutableListOf<Gene>()

        previous.addAll(
            previousSqlActions.flatMap(SqlAction::seeTopGenes)
        )
        for (i in 0 until insertions.size) {

            val action = insertions[i]
            if(action.representExistingData){
                /*
                    Even if not going to be part of the DTO, should still be able
                    to point to it with FKs
                 */
                previous.addAll(action.seeTopGenes())
                continue
            }


            val insertion = InsertionDto().apply { targetTable = action.table.id.getFullQualifyingTableName() }

            for (g in action.seeTopGenes()) {
                if (g is SqlPrimaryKeyGene) {
                    /*
                        If there is more than one primary key field, this
                        will be overridden.
                        But, as we need it only for automatically generated ones,
                        this shouldn't matter, as in that case there should be just 1.

                        FIXME: not really true, as can have multi-column PKs that are
                        FKs to auto-increment values in other tables.
                     */
                    insertion.id = g.uniqueId
                }

                /*
                  At the current moment, we do allow the "printing" of auto-increment
                  values that are already existing in the database, as those
                  are marked as immutable.
                  In those cases, we do not need to keep track of those values
               */

                if (!g.isPrintable()) {
                    continue
                }

                val entry = InsertionEntryDto()

                if(g is SqlWrapperGene && g.getForeignKey() != null){
                    handleSqlForeignKey(g.getForeignKey()!!, previous, entry, sqlIdMap)
                } else {
                    entry.printableValue = g.getValueAsPrintableString(targetFormat = null)
                }

                entry.variableName = g.getVariableName()

                /*  TODO: the above code needs to be refactored to get the targetFormat from EMConfig.
                    The target format has an impact on which characters are escaped and may result in compilation errors.
                    The current version performs no escaping of characters by default (i.e. when the target format is null).
                */

                insertion.data.add(entry)
            }

            list.add(insertion)
            previous.addAll(action.seeTopGenes())
        }

        return DatabaseCommandDto().apply { this.insertions = list }
    }

    /**
     * @param sqlIdMap is a map from Insertion Id to generated Id in database.
     *
     * Note that a reference of FK must exist in either [previous] or [sqlIdMap]
     */
    private fun handleSqlForeignKey(
            g: SqlForeignKeyGene,
            previous: List<Gene>,
            entry: InsertionEntryDto,
            sqlIdMap: Map<Long, Long>
    ) : Boolean {

        var justCreated = false
        val isFkReferenceToNonPrintable = try{
            g.isReferenceToNonPrintable(previous)
        }catch(e : Exception){
            if (sqlIdMap.containsKey(g.uniqueIdOfPrimaryKey)){
                justCreated = true
                false
            }else{
                throw IllegalArgumentException(e.message)
            }
        }

        if (isFkReferenceToNonPrintable) {
            entry.foreignKeyToPreviouslyGeneratedRow = g.uniqueIdOfPrimaryKey
        } else {
            entry.printableValue = if(justCreated) sqlIdMap.getValue(g.uniqueIdOfPrimaryKey).toString() else g.getValueAsPrintableString(previous, targetFormat = null)
        }

        return isFkReferenceToNonPrintable
    }
}