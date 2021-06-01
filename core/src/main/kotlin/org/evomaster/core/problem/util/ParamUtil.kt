package org.evomaster.core.problem.util

import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestPath
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.evomaster.core.search.gene.sql.SqlNullable
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * this class used to handle binding values among params
 */
class ParamUtil {

    companion object {

        private const val DISRUPTIVE_NAME = "d_"
        private const val BODYGENE_NAME = "body"
        private const val separator = "@"

        /**
         * when identifying relationship based on the "tokens", if the token belongs to [GENERAL_NAMES],
         * we may further use up-level token.
         */
        private val GENERAL_NAMES = mutableListOf("id", "name", "value")

        private val log: Logger = LoggerFactory.getLogger(ParamUtil::class.java)


        fun selectLongestPathAction(actions: List<RestCallAction>): List<RestCallAction> {
            val max =
                actions.filter { it is RestCallAction }.asSequence().map { a -> (a as RestCallAction).path.levels() }
                    .max()!!
            return actions.filter { a -> a is RestCallAction && a.path.levels() == max }
        }

        fun appendParam(paramsText: String, paramToAppend: String): String =
            if (paramsText.isBlank()) paramToAppend else "$paramsText$separator$paramToAppend"

        fun parseParams(params: String): List<String> = params.split(separator)

        fun modifyFieldName(obj: ObjectGene, field: Gene): String {
            return if (isGeneralName(field.name)) (obj.refType ?: "") + field.name else field.name
        }

        fun findField(fieldName: String, refType: String?, name: String): Boolean {
            if (!isGeneralName(fieldName) || refType == null) return fieldName.equals(name, ignoreCase = true)
            val prefix = "$refType$fieldName".equals(name, ignoreCase = true)
            if (prefix) return true
            return "$fieldName$refType".equals(name, ignoreCase = true)
        }

        fun isAllBodyParam(params: List<Param>): Boolean {
            return numOfBodyParam(params) == params.size
        }

        fun numOfBodyParam(params: List<Param>): Int {
            return params.count { it is BodyParam }
        }

        fun existBodyParam(params: List<Param>): Boolean {
            return numOfBodyParam(params) > 0
        }

        fun compareGenesWithValue(geneA: Gene, geneB: Gene): Boolean {
            val geneAWithGeneBType = geneB.copy()
            geneAWithGeneBType.bindValueBasedOn(geneA)
            return when (geneB) {
                is StringGene -> geneB.value == (geneAWithGeneBType as StringGene).value
                is IntegerGene -> geneB.value == (geneAWithGeneBType as IntegerGene).value
                is DoubleGene -> geneB.value == (geneAWithGeneBType as DoubleGene).value
                is FloatGene -> geneB.value == (geneAWithGeneBType as FloatGene).value
                is LongGene -> geneB.value == (geneAWithGeneBType as LongGene).value
                else -> {
                    throw IllegalArgumentException("the type of $geneB is not supported")
                }
            }
        }

        fun scoreOfMatch(target: String, source: String, inner: Boolean): Int {
            val targets = target.split(separator).filter { it != DISRUPTIVE_NAME }.toMutableList()
            val sources = source.split(separator).filter { it != DISRUPTIVE_NAME }.toMutableList()
            if (inner) {
                if (sources.toHashSet().map { s -> if (target.toLowerCase().contains(s.toLowerCase())) 1 else 0 }
                        .sum() == sources.toHashSet().size)
                    return 0
            }
            if (targets.toHashSet().size == sources.toHashSet().size) {
                if (targets.containsAll(sources)) return 0
            }
            if (sources.contains(BODYGENE_NAME)) {
                val sources_rbody = sources.filter { it != BODYGENE_NAME }.toMutableList()
                if (sources_rbody.toHashSet().map { s -> if (target.toLowerCase().contains(s.toLowerCase())) 1 else 0 }
                        .sum() == sources_rbody.toHashSet().size)
                    return 0
            }
            if (targets.first() != sources.first())
                return -1
            else
                return targets.plus(sources).filter { targets.contains(it).xor(sources.contains(it)) }.size

        }

        fun geneNameMaps(parameters: List<Param>, tokensInPath: List<String>?): MutableMap<String, Gene> {
            val maps = mutableMapOf<String, Gene>()
            val pred = { gene: Gene -> (gene is DateTimeGene) }
            parameters.forEach { p ->
                p.gene.flatView(pred).filter {
                    !(it is ObjectGene ||
                            it is DisruptiveGene<*> ||
                            it is OptionalGene ||
                            it is ArrayGene<*> ||
                            it is MapGene<*>)
                }
                    .forEach { g ->
                        val names = getGeneNamesInPath(parameters, g)
                        if (names != null)
                            maps.put(genGeneNameInPath(names, tokensInPath), g)
                    }
            }

            return maps
        }


        fun isGeneralName(text: String): Boolean {
            return GENERAL_NAMES.any { it.equals(text, ignoreCase = true) }
        }

        private fun genGeneNameInPath(names: MutableList<String>, tokensInPath: List<String>?): String {
            tokensInPath?.let {
                return names.plus(tokensInPath).joinToString(separator)
            }
            return names.joinToString(separator)
        }

        private fun getGeneNamesInPath(parameters: List<Param>, gene: Gene): MutableList<String>? {
            parameters.forEach { p ->
                val names = mutableListOf<String>()
                if (handle(p.gene, gene, names)) {
                    return names
                }
            }

            return null
        }

        fun handle(comGene: Gene, gene: Gene, names: MutableList<String>): Boolean {
            when (comGene) {
                is ObjectGene -> return handle(comGene, gene, names)
                is DisruptiveGene<*> -> return handle(comGene, gene, names)
                is OptionalGene -> return handle(comGene, gene, names)
                is ArrayGene<*> -> return handle(comGene, gene, names)
                is MapGene<*> -> return handle(comGene, gene, names)
                else -> if (comGene == gene) {
                    names.add(comGene.name)
                    return true
                } else return false
            }
        }

        fun handle(comGene: ObjectGene, gene: Gene, names: MutableList<String>): Boolean {
            comGene.fields.forEach {
                if (handle(it, gene, names)) {
                    names.add(it.name)
                    return true
                }
            }
            return false
        }

        fun handle(comGene: DisruptiveGene<*>, gene: Gene, names: MutableList<String>): Boolean {
            if (handle(comGene.gene, gene, names)) {
                names.add(comGene.name)
                return true
            }
            return false
        }

        fun handle(comGene: OptionalGene, gene: Gene, names: MutableList<String>): Boolean {
            if (handle(comGene.gene, gene, names)) {
                names.add(comGene.name)
                return true
            }
            return false
        }

        fun handle(comGene: ArrayGene<*>, gene: Gene, names: MutableList<String>): Boolean {
            comGene.elements.forEach {
                if (handle(it, gene, names)) {
                    return true
                }
            }
            return false
        }

        fun handle(comGene: MapGene<*>, gene: Gene, names: MutableList<String>): Boolean {
            comGene.elements.forEach {
                if (handle(it, gene, names)) {
                    names.add(it.name)
                    return true
                }
            }
            return false
        }

        fun getParamId(param: Param, path: RestPath): String {
            return listOf(param.name).plus(path.getNonParameterTokens().reversed()).joinToString(separator)
        }

        fun generateParamId(list: Array<String>): String = list.joinToString(separator)

        fun getValueGene(gene: Gene): Gene {
            if (gene is OptionalGene) {
                return getValueGene(gene.gene)
            } else if (gene is DisruptiveGene<*>)
                return getValueGene(gene.gene)
            else if (gene is SqlPrimaryKeyGene) {
                if (gene.gene is SqlAutoIncrementGene)
                    return gene
                else return getValueGene(gene.gene)
            } else if (gene is SqlNullable) {
                return getValueGene(gene.gene)
            }
            return gene
        }

        fun getObjectGene(gene: Gene): ObjectGene? {
            if (gene is ObjectGene) {
                return gene
            } else if (gene is OptionalGene) {
                return getObjectGene(gene.gene)
            } else if (gene is DisruptiveGene<*>)
                return getObjectGene(gene.gene)
            else return null
        }
    }
}