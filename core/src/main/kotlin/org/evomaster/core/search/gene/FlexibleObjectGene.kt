package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.optional.FlexibleGene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.utils.GeneUtils.isInactiveOptionalGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * this is a gene which could have fixed fields [fields] and more additional fields [additionalFields].
 * The additional fields need to follow the [template] and could have flexible type (eg, T is FlexibleGene)
 * eg, a schema example
 *  Person:
 *      type: object
 *      additionalProperties:
 *          oneOf:
 *              - type: string
 *              - type: integer
 */
class FlexibleObjectGene<T>(
        name: String,
        val fields: List<out Gene>,
        val template : PairGene<StringGene, T>,
        additionalFields:  List<PairGene<StringGene, T>> = listOf()
): CompositeGene(name, mutableListOf<Gene>().apply { addAll(fields); addAll(additionalFields) }) where T: Gene{

    companion object{
        private val log: Logger = LoggerFactory.getLogger(FlexibleObjectGene::class.java)
        // probability of mutating size of additional fields
        private const val PROB_MODIFY_SIZE_ADDITIONAL_FIELDS = 0.1
        // the default maximum size for additional fields
        private const val MAX_SIZE_ADDITIONAL_FIELDS = 5
    }

    /**
     * @return a view of additional fields
     */
    val additionalFields: List<PairGene<StringGene, T>>
        get() {return children.filterNot { fields.contains(it) }.filterIsInstance<PairGene<StringGene, T>>()}

    override fun copyContent(): Gene {
        return FlexibleObjectGene(name, fields.map(Gene::copy), template, additionalFields.map {it.copy() as PairGene<StringGene, T> })
    }

    override fun isLocallyValid(): Boolean {
        return getViewOfChildren().all { it.isLocallyValid() }
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        fields.filter { it.isMutable() }
                .forEach { it.randomize(randomness, tryToForceNewValue) }

        //TODO for additional fields
    }

    override fun customShouldApplyShallowMutation(randomness: Randomness, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        return randomness.nextBoolean(PROB_MODIFY_SIZE_ADDITIONAL_FIELDS)
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        if (additionalFields.isEmpty() || (additionalFields.size < MAX_SIZE_ADDITIONAL_FIELDS && randomness.nextBoolean(0.5))){
            val added = addElement(randomness)
            if (added != null)
                return true

            if (additionalFields.isEmpty()){
                log.warn("fail to apply shallowMutate for FlexibleObject, i.e., adding or removing additional field")
                return false
            }
        }

        val remove = randomness.choose(additionalFields)
        remove.removeThisFromItsBindingGenes()
        killChild(remove)
        return true
    }

    private fun addElement(randomness: Randomness) : PairGene<StringGene, FlexibleGene>?{
        val copy = template.copy() as PairGene<StringGene, FlexibleGene>
        copy.randomize(randomness, false)

        if (existingKey(copy)){
            copy.randomize(randomness, false)
        }

        if (existingKey(copy))
            return null
        return copy
    }

    private fun existingKey(fieldToAdd: PairGene<StringGene, FlexibleGene>): Boolean{
        return additionalFields.any { it.first.value == fieldToAdd.first.value}
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        if (mode != null && mode != GeneUtils.EscapeMode.JSON) {
            throw IllegalArgumentException("NOT SUPPORT $mode mode yet")
        }

        val buffer = StringBuffer()

        buffer.append("{")

        val fixedFields = fields.filter {
            it.isPrintable() && !isInactiveOptionalGene(it)
        }

        fixedFields.joinTo(buffer, ", ") {
            "\"${it.name}\":${it.getValueAsPrintableString(previousGenes, mode, targetFormat)}"
        }

        additionalFields.filter {
            it.isPrintable() && !isInactiveOptionalGene(it)
        }.also {
            if (it.isNotEmpty() && fixedFields.isNotEmpty())
                buffer.append(", ")
        }.joinTo(buffer, ", ") {
            "\"${it.first.value}\":${it.second.getValueAsPrintableString(previousGenes, mode, targetFormat)}"
        }

        buffer.append("}")
        return  buffer.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is FlexibleObjectGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (!template.possiblySame(other.template))
            throw IllegalArgumentException("different template ${other.template.javaClass}")

        for (i in fields.indices) {
            this.fields[i].copyValueFrom(other.fields[i])
        }

        //TODO for additional fields
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is FlexibleObjectGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (!template.possiblySame(other.template))
            throw IllegalArgumentException("different template ${other.template.javaClass}")

        return this.fields.size == other.fields.size
                && additionalFields.size == other.additionalFields.size
                && this.fields.zip(other.fields) { thisField, otherField -> thisField.containsSameValueAs(otherField) }.all { it }
                && this.additionalFields.zip(other.additionalFields) { thisField, otherField -> thisField.containsSameValueAs(otherField) }.all { it }
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is FlexibleObjectGene<*> && (fields.indices).all { fields[it].possiblySame(gene.fields[it]) } && template.possiblySame(gene.template)) {
            var result = true
            (fields.indices).forEach {
                val r = fields[it].bindValueBasedOn(gene.fields[it])
                if (!r)
                    LoggingUtil.uniqueWarn(log, "cannot bind the field ${fields[it].name}")
                result = result && r
            }
            if (!result)
                LoggingUtil.uniqueWarn(log, "fail to fully bind field values with the FlexibleObjectGene")

            //TODO bind additional fields

            return result
        }

        LoggingUtil.uniqueWarn(log, "cannot bind the ${this::class.java.simpleName} with ${gene::class.java.simpleName}")
        return false
    }

    override fun isMutable(): Boolean {
        return getViewOfChildren().any { it.isMutable() }
    }
}