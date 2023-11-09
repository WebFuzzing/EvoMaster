package org.evomaster.core.problem.rest.resource.dependency

import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto
import org.evomaster.core.sql.SQLKey
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.util.inference.model.MatchedInfo

/**
 * related info between resource and tables
 * @property key refers to an resource node
 */
class ResourceRelatedToTable(val key: String) {

    companion object {
        private const val FROM_EVO_DTO = "____FROM_DTO____"

        fun generateFromDtoMatchedInfo(table: String) : MatchedInfo = MatchedInfo(FROM_EVO_DTO, table, 1.0, -1, -1)
    }
    /**
     * key is table name
     * value is MatchedInfo, [MatchedInfo.targetMatched] refers to a table name, [MatchedInfo.input] is either segment or referType
     *
     * the map is derived based on
     *  1) type, for instance /A/{b}/B/{b}, body param {c} refer to type C. (input indicator is 0)
     *  2) segments, for instance there are two segments (i.e., /A and /B) regarding the path /A/{a}/B/{b}. for B, the input indicator is 0, for A, the input indicator is 1
     * then detect whether there are tables related to C, A and B.
     */
    val derivedMap : MutableMap<String, MutableList<MatchedInfo>> = mutableMapOf()

    /**
     * key is id of param
     * value is matched info
     */
    val paramToTable : MutableMap<String, ParamRelatedToTable> = mutableMapOf()

    /**
     * key is table name
     * value is boolean, indicating whether the relationship is direct
     *
     * whether the related table is confirmed by evomaster driver, i.e., return [ExecutionDto]
     *
     * to bind param of action regarding table,
     *      we need to further detect whether the relationship between resource and table is direct
     */
    val confirmedSet : MutableMap<String, Boolean> = mutableMapOf()

    /**
     * key is verb of the action
     * value is related table with its fields
     *
     * Note that same action may execute different SQL commends due to e.g., cookies or different values
     * When updating [actionToTables], we check if existing [ActionRelatedToTable] subsumes [ExecutionDto] or [ExecutionDto] subsume [ActionRelatedToTable] regarding involved tables.
     * if subsumed, we update existing [ActionRelatedToTable]; else create [ActionRelatedToTable]
     */
    private val actionToTables : MutableMap<String, MutableList<ActionRelatedToTable>> = mutableMapOf()


    fun updateActionRelatedToTable(verb : String, dto: org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto, existingTables : Set<String>) : Boolean{

        val tables = mutableListOf<String>().plus(dto.deletedData).plus(dto.updatedData.keys).plus(dto.insertedData.keys).plus(dto.queriedData.keys)
                .filter { existingTables.contains(it) || existingTables.any { e->e.toLowerCase() == it.toLowerCase() }}.toHashSet()

        if (tables.isEmpty()) return false

        var access = actionToTables
                .getOrPut(verb){ mutableListOf()}
                .find { it.doesSubsume(tables, true) || it.doesSubsume(tables, false)}

        val doesUpdateParamTable = (access == null)

        if(access== null){
            access = ActionRelatedToTable(verb)
            actionToTables[verb]!!.add(access)
        }

        access.updateTableWithFields(dto.deletedData.map { Pair(it, mutableSetOf<String>()) }.toMap(), SQLKey.DELETE)
        access.updateTableWithFields(dto.insertedData, SQLKey.INSERT)
        access.updateTableWithFields(dto.queriedData, SQLKey.SELECT)
        access.updateTableWithFields(dto.updatedData, SQLKey.UPDATE)

        return doesUpdateParamTable
    }

    fun getConfirmedDirectTables() : Set<String>{
        return derivedMap.keys.filter { t-> confirmedSet.any { it.key.equals(t, ignoreCase = true) && it.value } }.toHashSet()
    }


    fun findBestTableForParam(tables: Set<String>, simpleP2Table : SimpleParamRelatedToTable, onlyConfirmedColumn : Boolean = false) : Pair<Set<String>, Double>? {
        val map = simpleP2Table.derivedMap.filter { tables.any { t-> t.equals(it.key, ignoreCase = true) } }
                .map {
                    Pair(it.key, it.value.similarity)
                }.toMap()
        if(map.isEmpty()) return null
        val best = map.asSequence().sortedBy { it.value }.last().value
        return Pair(map.filter { it.value == best }.keys, best)
    }

    /**
     * return Pair.first is name of table, Pair.second is column of Table
     */
    fun getSimpleParamToSpecifiedTable(table: String, simpleP2Table : SimpleParamRelatedToTable, onlyConfirmedColumn : Boolean = false) : Pair<String, String>? {
        simpleP2Table.derivedMap.filter { table.equals(it.key, ignoreCase = true) }.let { map->
            if (map.isEmpty()) return null
            else return Pair(table, map.values.first().targetMatched)
        }
    }


    /**
     * @return
     *      key is field name
     *      value is a pair, the first of pair of related table and the second is similarity
     */
    fun findBestTableForParam(tables: Set<String>, bodyP2Table : BodyParamRelatedToTable, onlyConfirmedColumn : Boolean = false) : MutableMap<String, Pair<Set<String>, Double>>?{
        val fmap = bodyP2Table.fieldsMap
                .filter { it.value.derivedMap.any { m-> tables.any { t-> t.equals(m.key, ignoreCase = true) } } }
        if(fmap.isEmpty()) return null

        val result : MutableMap<String, Pair<Set<String>, Double>> = mutableMapOf()
        fmap.forEach { t, u ->
            val related = u.derivedMap.filter { m-> tables.any { t-> t.equals(m.key, ignoreCase = true) }}
            if (related.isNotEmpty()){
                val best = related.map { it.value.similarity }.maxOrNull()!!
                result.put(t, Pair(related.filter { it.value.similarity == best }.keys, best))
            }
        }
        return result
    }

    /**
     * return Pair.first is name of table, key of the second is field, value is the column
     */
    fun getBodyParamToSpecifiedTable(table:String, bodyP2Table : BodyParamRelatedToTable, onlyConfirmedColumn : Boolean = false) : Pair<String, Map<String, String>>? {
        val fmap = bodyP2Table.fieldsMap
                .filter { it.value.derivedMap.any { m->m.key.toLowerCase() == table.toLowerCase() } }
        if(fmap.isEmpty()) return null
        else{
            return Pair(table, fmap.map { f-> Pair(f.key, f.value.getRelatedColumn(table)!!.first())}.toMap())
        }
    }

    /**
     * return Pair.first is name of table, key of the second is field, value is the column
     *
     * @param onlyConfirmedColumn will be used to only find the field that are confirmed based on feedback from evomaster driver
     */
    fun getBodyParamToSpecifiedTable(table:String, bodyP2Table : BodyParamRelatedToTable, fieldName : String, onlyConfirmedColumn : Boolean = false) : Pair<String, Pair<String, String>>? {
        val fmap = bodyP2Table.fieldsMap[fieldName]?: return null
        fmap.derivedMap.filter { it.key.toLowerCase() == table.toLowerCase() }.let {
            if (it.isEmpty()) return null
            else return Pair(table, Pair(fieldName, it.values.first().targetMatched))
        }
    }

    fun getTablesInDerivedMap() : Set<String> = derivedMap.keys

    fun getTablesInDerivedMap(segs : List<String>) : Map<String, String>{
        return segs.map { input->
            Pair(input, getTablesInDerivedMap(input).run {
                if(this.isEmpty()) ""
                else this.asSequence().sortedBy { it.similarity }.last().targetMatched
            }) }.toMap()
    }

    private fun getTablesInDerivedMap(input : String) : List<MatchedInfo>{
        return derivedMap.values.flatMap {
            it.filter { m-> m.input.toLowerCase() == input.toLowerCase()}
        }
    }

}
/**
 * related info between an rest action and tables, which is further extracted based on feedback from evomaster driver
 * @property key refers to an rest action
 */
class ActionRelatedToTable(
        val key: String,
        /**
         * key is table name
         * value is how it accessed by dbaction
         */
        val tableWithFields: MutableMap<String, MutableList<AccessTable>> = mutableMapOf()
) {


    /**
     * key of result is table name
     * value of result is a set of manipulated columns
     */
    fun updateTableWithFields(results : Map<String, Set<String>>, method: SQLKey) {
        var doesUpdateTarget = false
        results.forEach { t, u ->
            doesUpdateTarget = doesUpdateTarget || tableWithFields.containsKey(t)
            tableWithFields.getOrPut(t){ mutableListOf() }.run {
                var target = find { it.method == method }
                if (target == null){
                    target = AccessTable(method, t, mutableSetOf())
                    this.add(target)
                }
                target.field.addAll(u)
            }
        }
    }

    fun doesSubsume(tables : Set<String>, subsumeThis : Boolean) : Boolean{
        return if(subsumeThis) tables.toHashSet().containsAll(tableWithFields.keys)
        else tableWithFields.keys.containsAll(tables)
    }

    /**
     * @property method a sql method
     * @property table related table
     * @property field what fields are assess by the sql command
     */
    class AccessTable(val method : SQLKey, val table : String, val field : MutableSet<String>)
}

/**
 * related info between a param and a table
 * @property key refers to a param
 */
abstract class ParamRelatedToTable (
        val key: String){

    /**
     * key is table name
     * value is [MatchedInfo], [MatchInfo.targetMatched] is name of column of the table
     */
    val derivedMap : MutableMap<String, MatchedInfo> = mutableMapOf()

    val confirmedColumn : MutableSet<String> = mutableSetOf()

    open fun getRelatedColumn(table: String) : Set<String>?  = derivedMap.filterKeys { it.equals(table, ignoreCase = true) }.values.firstOrNull().run { if (this == null) null else setOf(this.targetMatched)  }
}

/**
 * related info between a Param (not BodyParam) and a table
 */
class SimpleParamRelatedToTable (key: String, val referParam: Param): ParamRelatedToTable(key)

/**
 * related info between a BodyParam and a table
 */
class BodyParamRelatedToTable(key: String, val referParam: Param): ParamRelatedToTable(key){
    init {
        assert(referParam is BodyParam)
    }

    /**
     * key is field name of [referParam]
     * value is related to table
     */
    val fieldsMap : MutableMap<String, ParamFieldRelatedToTable> = mutableMapOf()

    override fun getRelatedColumn(table: String) : Set<String>? = fieldsMap.values.filter { it.derivedMap.any {  m-> m.key.toLowerCase() == table.toLowerCase() }}.run {
        if (this.isEmpty()) return null
        else
            this.map { f->f.derivedMap.filterKeys { k->k.toLowerCase() == table.toLowerCase() }.asSequence().first().value.targetMatched}.toHashSet()
    }

}
/**
 * related info between a field of BodyParam and a table
 */
class ParamFieldRelatedToTable(key: String) : ParamRelatedToTable(key){

    override fun getRelatedColumn(table: String) :  Set<String>? {
        derivedMap.filter { it.key.toLowerCase() == table.toLowerCase() }.let {
            if(it.isEmpty()) return null
            else return it.values.map {m-> m.targetMatched}.toSet()
        }
    }
}



