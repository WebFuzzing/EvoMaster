package org.evomaster.core.problem.util.inference.model

import org.evomaster.core.sql.schema.TableId

/**
 * [isElementOfParam] means the gene of Param is one type of ObjectGene, MapGene, ListGene
 */
class ParamGeneBindMap(
    val paramId : String,
    val isElementOfParam: Boolean,
    val targetToBind : String,
    val tableName: TableId,
    val column: String){

    fun equalWith(p: ParamGeneBindMap) : Boolean{
        return paramId == p.paramId && isElementOfParam == p.isElementOfParam && targetToBind == p.targetToBind && tableName == p.tableName && column == p.column
    }
}
