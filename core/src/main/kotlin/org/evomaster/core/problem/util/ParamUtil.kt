package org.evomaster.core.problem.util

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils

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


        /**
         * @return the actions which have the longest path in [actions]
         */
        fun selectLongestPathAction(actions: List<RestCallAction>): List<RestCallAction> {
            val max =
                actions.asSequence().map { a -> a.path.levels() }
                    .maxOrNull()!!
            return actions.filter { a ->  a.path.levels() == max }
        }

        /**
         * append param i.e., [paramToAppend] with additional info [paramsText]
         */
        fun appendParam(paramsText: String, paramToAppend: String): String =
            if (paramsText.isBlank()) paramToAppend else "$paramsText$separator$paramToAppend"

        /**
         * @return extracted params
         */
        fun parseParams(params: String): List<String> = params.split(separator)

        /**
         * @return a field name with info of its object
         */
        fun modifyFieldName(obj: ObjectGene, field: Gene): String {
            return if (isGeneralName(field.name)) (obj.refType ?: "") + field.name else field.name
        }

        /**
         * @return whether [name] is possibly matched with a field [fieldName] of [refType]
         */
        fun compareField(fieldName: String, refType: String?, name: String): Boolean {
            if (!isGeneralName(fieldName) || refType == null) return fieldName.equals(name, ignoreCase = true)
            val prefix = "$refType$fieldName".equals(name, ignoreCase = true)
            if (prefix) return true
            return "$fieldName$refType".equals(name, ignoreCase = true)
        }

        /**
         * @return are all [params] BodyParam?
         */
        fun isAllBodyParam(params: List<Param>): Boolean {
            return numOfBodyParam(params) == params.size
        }

        /**
         * @return a number of BodyParam in [params]
         */
        fun numOfBodyParam(params: List<Param>): Int {
            return params.count { it is BodyParam }
        }

        /**
         * @return do [params] contain any BodyParam?
         */
        fun existBodyParam(params: List<Param>): Boolean {
            return numOfBodyParam(params) > 0
        }

        /**
         * @return whether [geneA] and [geneB] have same value.
         */
        fun compareGenesWithValue(geneA: Gene, geneB: Gene): Boolean {
            val geneAWithGeneBType = geneB.copy()
            geneAWithGeneBType.setFromDifferentGene(geneA)
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

        /**
         * @return the score of match between [target] and [source] which represents two genes respectively. Note that 0 means matched.
         * @param doContain indicates whether 'contains' can be considered as match. i.e., target contains every token of sources.
         */
        fun scoreOfMatch(target: String, source: String, doContain: Boolean): Int {
            val targets = target.split(separator).filter { it != DISRUPTIVE_NAME }.toMutableList()
            val sources = source.split(separator).filter { it != DISRUPTIVE_NAME }.toMutableList()
            if (doContain) {
                if (sources.toHashSet().map { s -> if (target.lowercase().contains(s.lowercase())) 1 else 0 }
                        .sum() == sources.toHashSet().size)
                    return 0
            }
            if (targets.toHashSet().size == sources.toHashSet().size) {
                if (targets.containsAll(sources)) return 0
            }
            if (sources.contains(BODYGENE_NAME)) {
                val sources_rbody = sources.filter { it != BODYGENE_NAME }.toMutableList()
                if (sources_rbody.toHashSet().map { s -> if (target.lowercase().contains(s.lowercase())) 1 else 0 }
                        .sum() == sources_rbody.toHashSet().size)
                    return 0
            }
            if (targets.first() != sources.first())
                return -1
            else
                return targets.plus(sources).filter { targets.contains(it).xor(sources.contains(it)) }.size

        }

        /**
         *  @return a map of a path of gene to gene
         *  @param parameters specifies the params which contains genes to be extracted
         *  @param tokensInPath specified the tokens of the path which refers to [parameters]
         */
        fun geneNameMaps(parameters: List<Param>, tokensInPath: List<String>?): MutableMap<String, Gene> {
            val maps = mutableMapOf<String, Gene>()
            val pred = { gene: Gene -> (gene is DateTimeGene) }
            parameters.forEach { p ->
                p.gene.flatView(pred).filter {
                    !(it is ObjectGene ||
                            it is CustomMutationRateGene<*> ||
                            it is OptionalGene ||
                            it is ArrayGene<*> ||
                            it is FixedMapGene<*, *>)
                }
                    .forEach { g ->
                        val names = getGeneNamesInPath(parameters, g)
                        if (names != null)
                            maps.put(genGeneNameInPath(names, tokensInPath), g)
                    }
            }

            return maps
        }


        /**
         * @return whether [text] is a general name, e.g., 'id' or 'name'
         */
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
                if (extractPathFromRoot(p.gene, gene, names)) {
                    return names
                }
            }

            return null
        }

        /**
         * extract a path from [comGene] to [gene]
         * @param names contains the name of genes in the path
         *
         * @return can find [gene] in [comGene]?
         */
        private fun extractPathFromRoot(comGene: Gene, gene: Gene, names: MutableList<String>): Boolean {
            when (comGene) {
                is ObjectGene -> return extractPathFromRoot(comGene, gene, names)
                is PairGene<*, *> -> return extractPathFromRoot(comGene, gene, names)
                is CustomMutationRateGene<*> -> return extractPathFromRoot(comGene, gene, names)
                is OptionalGene -> return extractPathFromRoot(comGene, gene, names)
                is ArrayGene<*> -> return extractPathFromRoot(comGene, gene, names)
                is FixedMapGene<*, *> -> return extractPathFromRoot(comGene, gene, names)
                else -> if (comGene == gene) {
                    names.add(comGene.name)
                    return true
                } else return false
            }
        }

        private fun extractPathFromRoot(comGene: ObjectGene, gene: Gene, names: MutableList<String>): Boolean {
            comGene.fields.forEach {
                if (extractPathFromRoot(it, gene, names)) {
                    names.add(it.name)
                    return true
                }
            }
            return false
        }

        private fun extractPathFromRoot(comGene: CustomMutationRateGene<*>, gene: Gene, names: MutableList<String>): Boolean {
            if (extractPathFromRoot(comGene.gene, gene, names)) {
                names.add(comGene.name)
                return true
            }
            return false
        }

        private fun extractPathFromRoot(comGene: OptionalGene, gene: Gene, names: MutableList<String>): Boolean {
            if (extractPathFromRoot(comGene.gene, gene, names)) {
                names.add(comGene.name)
                return true
            }
            return false
        }

        private fun extractPathFromRoot(comGene: ArrayGene<*>, gene: Gene, names: MutableList<String>): Boolean {
            comGene.getViewOfElements().forEach {
                if (extractPathFromRoot(it, gene, names)) {
                    return true
                }
            }
            return false
        }

        private fun extractPathFromRoot(comGene: PairGene<*, *>, gene: Gene, names: MutableList<String>): Boolean {
            listOf(comGene.first, comGene.second).forEach {
                if (extractPathFromRoot(it, gene, names)) {
                    names.add(it.name)
                    return true
                }
            }
            return false
        }

        private fun extractPathFromRoot(comGene: FixedMapGene<*, *>, gene: Gene, names: MutableList<String>): Boolean {
            comGene.getAllElements().forEach {
                if (extractPathFromRoot(it, gene, names)) {
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

        @Deprecated(message = "Rather use GeneUtils.getWrappedValueGene(gene)",
            replaceWith = ReplaceWith("GeneUtils.getWrappedValueGene(gene)"))
        fun getValueGene(gene: Gene): Gene {
           return GeneUtils.getWrappedValueGene(gene,false)!!
        }

        fun getObjectGene(gene: Gene): ObjectGene? {
            if (gene is ObjectGene) {
                return gene
            } else if (gene is OptionalGene) {
                return getObjectGene(gene.gene)
            } else if (gene is CustomMutationRateGene<*>)
                return getObjectGene(gene.gene)
            else return null
        }
    }
}