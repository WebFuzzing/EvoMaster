package org.evomaster.core.search.gene

import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.TupleGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *  A tuple is a fixed-size, ordered list of elements, of possible different types.
 *  This is needed for example when representing the inputs of function calls in
 *  GraphQL.
 */
class TupleGene(
    /**
     * The name of this gene
     */
    name: String,
    /**
     * The actual elements in the array, based on the template. Ie, usually those elements will be clones
     * of the templated, and then mutated/randomized
     * note that if the list of gene could be updated, its impact needs to be updated
     */
    val elements: List<Gene>,
    /**
     * In some cases, we want to treat an element differently from the other (the last in particular).
     * This is for example the case of function calls in GQL when the return type is an object, on
     * which we need to select what to retrieve.
     * In these cases, such return object will be part of the tuple, as the last element.
     */
    val lastElementTreatedSpecially: Boolean = false

) : Gene(name, elements) {

    init {
        if (elements.isEmpty()) {
            throw IllegalArgumentException("Empty tuple")
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(TupleGene::class.java)
    }

    /*
    For GQL, the last element of the Tuple represents a return type gene.
    It is a special gene that will be treated with the Boolean selection.
     */
    fun getSpecialGene(): Gene? {

        if (lastElementTreatedSpecially) {
            return elements.last()
        }
        return null

    }


    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {

        val buffer = StringBuffer()

        if (mode== GeneUtils.EscapeMode.GQL_NONE_MODE) {
            //need the name for input and return
            buffer.append("$name")
            //printout the inputs. See later if a refactoring is needed
            if (elements.dropLast(1).isNotEmpty()) {
                buffer.append("(")
                val s = elements.dropLast(1).map {

                    if (it is EnumGene<*> ||
                        (it is OptionalGene && it.gene is EnumGene<*>) ||
                        (it is OptionalGene && it.gene is ArrayGene<*> && it.gene.template is EnumGene<*>) ||
                        (it is OptionalGene && it.gene is ArrayGene<*> && it.gene.template is OptionalGene && it.gene.template.gene is EnumGene<*>) ||
                        (it is ArrayGene<*> && it.template is EnumGene<*>) ||
                        (it is ArrayGene<*> && it.template is OptionalGene && it.template.gene is EnumGene<*>)
                    ) {
                        val i = it.getValueAsRawString()
                        "${it.name} : $i"
                    } else {
                        if (it is ObjectGene || (it is OptionalGene && it.gene is ObjectGene)) {
                            val i = it.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_MODE)
                            " $i"
                        } else {
                            if (it is ArrayGene<*> || (it is OptionalGene && it.gene is ArrayGene<*>)) {
                                val i = it.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_ARRAY_MODE)
                                "${it.name} : $i"
                            } else {
                                val mode =
                                    if (ParamUtil.getValueGene(it) is StringGene) GeneUtils.EscapeMode.GQL_STR_VALUE else GeneUtils.EscapeMode.GQL_INPUT_MODE
                                val i = it.getValueAsPrintableString(mode = mode, targetFormat = targetFormat)
                                "${it.name} : $i"
                            }
                        }
                    }

                }.joinToString(",").replace("\"", "\\\"")
                //see another way: eg, joinTo(buffer, ", ").toString().replace("\"", "\\\"") to buffer
                buffer.append(s)
                buffer.append(")")
            }
            //printout the return
            val returnGene = elements.last()
            buffer.append(
                if (returnGene is OptionalGene) {
                    assert(returnGene.gene is ObjectGene)
                    returnGene.gene.getValueAsPrintableString(
                        previousGenes,
                        GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE,
                        targetFormat,
                        extraCheck = true
                    )
                } else
                    if (returnGene is ObjectGene) {
                        returnGene.getValueAsPrintableString(
                            previousGenes,
                            GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE,
                            targetFormat,
                            extraCheck = true
                        )
                    } else ""
            )
        } else {
            "[" + elements.map { it.getValueAsPrintableString(previousGenes, mode, targetFormat) }.joinTo(buffer, ", ") + "]"
        }
        return buffer.toString()

    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        if (elements.isNotEmpty())
        elements.forEach {
            it.randomize(randomness, false)
        }
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is TupleGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        assert (elements.size == other.elements.size)
        (elements.indices).forEach {
            elements[it].copyValueFrom(other.elements[it])
        }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is TupleGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.elements.zip(other.elements) { thisElem, otherElem ->
            thisElem.containsSameValueAs(otherElem)
        }.all { it }
    }

    override fun innerGene(): List<Gene> = elements


    override fun bindValueBasedOn(gene: Gene): Boolean {

        if (gene is TupleGene
            && elements.size == gene.elements.size
            // binding is applicable only if names of element genes are consistent
            && (elements.indices).all { elements[it].possiblySame(gene.elements[it]) }
        ) {
            var result = true
            (elements.indices).forEach {
                val r = elements[it].bindValueBasedOn(gene.elements[it])
                if (!r)
                    LoggingUtil.uniqueWarn(log, "cannot bind the element at $it with the name ${elements[it].name}")
                result = result && r
            }
            if (!result)
                LoggingUtil.uniqueWarn(log, "cannot bind the ${this::class.java.simpleName} with the specified TupleGene gene")
            return result
        }
        LoggingUtil.uniqueWarn(log, "cannot bind TupleGene with ${gene::class.java.simpleName}")
        return false

    }

    override fun getChildren(): List<Gene> = elements

    override fun isMutable(): Boolean {
        return elements.any { it.isMutable() }
    }

    override fun mutationWeight(): Double {
        return elements.sumOf { it.mutationWeight() }
    }

    override fun candidatesInternalGenes(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        allGenes: List<Gene>,
        selectionStrategy: SubsetGeneSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return elements.filter { it.isMutable() }
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(elements.flatMap { g -> g.flatView(excludePredicate) })
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {

        if (additionalGeneMutationInfo.impact != null
            && additionalGeneMutationInfo.impact is TupleGeneImpact
        ) {
            val impacts = internalGenes.map { additionalGeneMutationInfo.impact.elements.getValue(it.name) }
            val selected = mwc.selectSubGene(
                internalGenes, true, additionalGeneMutationInfo.targets, individual = null, impacts = impacts, evi = additionalGeneMutationInfo.evi
            )
            val map = selected.map { internalGenes.indexOf(it) }
            return map.map { internalGenes[it] to additionalGeneMutationInfo.copyFoInnerGene(impact = impacts[it] as? GeneImpact, gene = internalGenes[it]) }
        }
        throw IllegalArgumentException("impact is null or not TupleGeneImpact, ${additionalGeneMutationInfo.impact}")
    }

    override fun copyContent(): Gene {
        return TupleGene(name, elements.map(Gene::copyContent), lastElementTreatedSpecially)
    }
}