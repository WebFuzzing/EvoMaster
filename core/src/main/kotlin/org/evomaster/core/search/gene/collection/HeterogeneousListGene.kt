package org.evomaster.core.search.gene.collection

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.interfaces.CollectionGene
import org.evomaster.core.search.gene.interfaces.TaintableGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.placeholder.LimitObjectGene
import org.evomaster.core.search.gene.root.CompositeConditionalFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * An ordered list of elements that may have different gene types (e.g., JSON/DynamoDB lists).
 *
 * Mirrors [org.evomaster.core.search.gene.ObjectGene]'s [CompositeConditionalFixedGene] design:
 * - When [isFixed] is `true`, the list structure is fixed (no additions or removals; may be empty `[]`).
 * - When [isFixed] is `false`, [template] is required and shallow mutations may append new elements
 *   cloned from [template] up to [maxSize] or remove trailing elements down to [minSize] (protecting
 *   the initial `0 until minSize` prefix).
 */
open class HeterogeneousListGene(
    name: String,
    elements: List<Gene>,
    isFixed: Boolean = true,
    val template: Gene? = null,
    var maxSize: Int? = null,
    var minSize: Int? = null
) : CollectionGene, CompositeConditionalFixedGene(name, isFixed, elements.toMutableList()) {

    /**
     * Convenience constructor for a fixed-structure heterogeneous list (including empty `[]`).
     */
    constructor(name: String, elements: List<Gene>) : this(name, elements, isFixed = true, template = null)

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HeterogeneousListGene::class.java)
        const val MAX_SIZE = ArrayGene.MAX_SIZE
    }

    /**
     * Live view of the list elements backed directly by [children].
     */
    val elements: List<Gene>
        get() = getViewOfChildren()

    init {
        if (isFixed && template != null) {
            throw IllegalArgumentException("Cannot specify template when HeterogeneousListGene is fixed")
        }
        if (!isFixed && template == null) {
            throw IllegalArgumentException("Must specify template when HeterogeneousListGene is not fixed")
        }
        if (!isFixed && (template is CycleObjectGene || template is LimitObjectGene)) {
            minSize = 0
            maxSize = 0
            killAllChildren()
        }
        validateCollectionSizeBounds(
            "HeterogeneousListGene",
            name,
            minSize,
            maxSize,
            if (isFixed) null else elements.size
        )
    }

    override fun canBeChildless(): Boolean = true

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        if (isFixed) return true
        return elements.size >= getMinSizeOrDefault() &&
            elements.size <= (maxSize ?: Int.MAX_VALUE)
    }

    override fun isMutable(): Boolean {
        if (isFixed) {
            return elements.any { it.isMutable() }
        }
        return getMaxSizeOrDefault() > 0 &&
            !(getMinSizeOrDefault() == getMaxSizeOrDefault() &&
                elements.size == getMinSizeOrDefault() &&
                elements.none { it.isMutable() })
    }

    override fun mutationWeight(): Double {
        return if (isFixed) {
            elements.sumOf { it.mutationWeight() }
        } else {
            1.0 + elements.sumOf { it.mutationWeight() }
        }
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        if (isFixed) {
            elements.filter { it.isMutable() }.forEach {
                it.randomize(randomness, false)
            }
            return
        }
        if (maxSize == 0) {
            return
        }
        killAllChildren()
        val minTarget = getMinSizeOrDefault()
        val maxTarget = maxOf(minTarget, getMaxSizeUsedInRandomize())
        val n = randomness.nextInt(minTarget, maxTarget)
        repeat(n) {
            addChild(createRandomElement(randomness))
        }
    }

    /**
     * Instantiates and randomizes a fresh element cloned from [template] without attaching [template] to [children].
     */
    private fun createRandomElement(randomness: Randomness): Gene {
        val copy = template!!.copy()
        copy.name = "${name}_${elements.size}"
        if (this.initialized) {
            copy.doInitialize(randomness)
        } else if (copy.isMutable()) {
            copy.randomize(randomness, false)
        }
        copy.getWrappedGene(TaintableGene::class.java)?.forceNewTaintId()
        return copy
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        if (isFixed) return false
        if (getMinSizeOrDefault() == getMaxSizeOrDefault() && elements.size == getMinSizeOrDefault()) {
            return false
        }
        val p = probabilityToModifySize(selectionStrategy, additionalGeneMutationInfo?.impact)
        return randomness.nextBoolean(p)
    }

    override fun shallowMutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        if (isFixed) {
            throw IllegalStateException("shallowMutate is not applied to fixed HeterogeneousListGene")
        }
        val effectiveMin = getMinSizeOrDefault()
        val effectiveMax = getMaxSizeOrDefault()
        val canAdd = elements.size < effectiveMax
        val canRemove = elements.size > effectiveMin
        if (!canAdd && !canRemove) {
            return false
        }
        if (canAdd && (!canRemove || randomness.nextBoolean())) {
            addChild(createRandomElement(randomness))
        } else {
            val idx = randomness.nextInt(effectiveMin, elements.size - 1)
            killChildByIndex(idx)
        }
        return true
    }

    override fun adaptiveSelectSubsetToMutate(
        randomness: Randomness,
        internalGenes: List<Gene>,
        mwc: MutationWeightControl,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        val selected = randomness.choose(internalGenes)
        return listOf(
            selected to additionalGeneMutationInfo.copyFoInnerGene(
                ImpactUtils.createGeneImpact(selected, selected.name),
                selected
            )
        )
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return "[" + elements.filter { it.isPrintable() }.joinToString(", ") {
            it.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
        } + "]"
    }


    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is HeterogeneousListGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (this::class != other::class || this.isFixed != other.isFixed) {
            return false
        }
        if (this.elements.size != other.elements.size) {
            return false
        }
        return this.elements.zip(other.elements) { a, b ->
            a::class == b::class && try {
                a.containsSameValueAs(b)
            } catch (_: IllegalArgumentException) {
                false
            }
        }.all { it }
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        val gene = other.getPhenotype()
        if (gene !is HeterogeneousListGene || this::class != gene::class || gene.isFixed != this.isFixed) {
            LoggingUtil.uniqueWarn(log, "cannot bind HeterogeneousListGene with ${gene::class.java.simpleName}")
            return false
        }
        if (isFixed) {
            if (elements.size != gene.elements.size ||
                !elements.indices.all { elements[it].possiblySame(gene.elements[it]) }
            ) {
                LoggingUtil.uniqueWarn(log, "cannot bind fixed HeterogeneousListGene with incompatible elements")
                return false
            }
            var result = true
            for (i in elements.indices) {
                val r = elements[i].unsafeCopyValueFrom(gene.elements[i])
                if (!r) {
                    LoggingUtil.uniqueWarn(log, "cannot bind the element at $i with the name ${elements[i].name}")
                }
                result = result && r
            }
            return result
        }

        if (!template!!.possiblySame(gene.template!!) ||
            gene.elements.size < getMinSizeOrDefault() ||
            (maxSize != null && gene.elements.size > maxSize!!)
        ) {
            LoggingUtil.uniqueWarn(log, "cannot bind mutable HeterogeneousListGene with incompatible template or size")
            return false
        }

        killAllChildren()
        val cloned = gene.elements.map { elem ->
            elem.copy().also { c ->
                c.resetLocalIdRecursively()
                if (this.initialized && !c.initialized) {
                    c.markAllAsInitialized()
                }
            }
        }
        addChildren(cloned)
        return true
    }

    override fun copyContent(): Gene {
        return HeterogeneousListGene(
            name = name,
            elements = elements.map(Gene::copy),
            isFixed = isFixed,
            template = template?.copy(),
            maxSize = maxSize,
            minSize = minSize
        )
    }

    override fun isEmpty(): Boolean = elements.isEmpty()

    override fun getSpecifiedMaxSize(): Int? = if (isFixed) elements.size else maxSize

    override fun getSpecifiedMinSize(): Int? = if (isFixed) elements.size else minSize

    override fun getGeneName(): String = name

    override fun getSizeOfElements(filterMutable: Boolean): Int {
        if (!filterMutable) return elements.size
        return elements.count { it.isMutable() }
    }

    override fun getMaxSizeOrDefault(): Int = if (isFixed) elements.size else (maxSize ?: getDefaultMaxSize())

    override fun getMinSizeOrDefault(): Int = if (isFixed) elements.size else (minSize ?: 0)

    override fun getDefaultMaxSize(): Int {
        val min = getMinSizeOrDefault()
        return if (min >= MAX_SIZE) min + MAX_SIZE else MAX_SIZE
    }
}
