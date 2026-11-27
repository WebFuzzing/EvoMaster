package org.evomaster.core.search.gene.collection

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.NumberGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.placeholder.LimitObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * An [ArrayGene] with `uniqueElements = true` that protects the first [protectedCount] elements
 * from removal during randomization, shallow size mutation, repair, and value copying.
 *
 * Protected elements remain mutable in-place (subject to uniqueness), while trailing elements
 * beyond [protectedCount] can be freely added and removed up to [maxSize]. This guarantees that
 * inferred non-empty sets (such as DynamoDB `SS` and `NS`) never shrink to empty while still
 * allowing search to explore larger sets and mutated element values.
 *
 * Initial duplicate elements are removed while preserving first-occurrence order, and
 * [protectedCount] (defaulting to all initial [elements]) is clamped to the deduplicated size.
 */
class ProtectedUniqueArrayGene<T>(
    name: String,
    template: T,
    protectedCount: Int? = null,
    maxSize: Int? = null,
    minSize: Int? = null,
    elements: MutableList<T> = mutableListOf(),
    openingTag: String = "[",
    closingTag: String = "]",
    separatorTag: String = ", "
) : ArrayGene<T>(
    name = name,
    template = template,
    uniqueElements = true,
    maxSize = maxSize,
    minSize = minSize,
    elements = prepareInitialElements(template, protectedCount, elements),
    openingTag = openingTag,
    closingTag = closingTag,
    separatorTag = separatorTag
) where T : Gene {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ProtectedUniqueArrayGene::class.java)
        private const val MAX_PROTECTED_RANDOMIZE_ATTEMPTS = 5

        /**
         * Validates [requestedProtectedCount] against [elements] and deduplicates initial elements
         * while preserving first-occurrence order.
         */
        private fun <T : Gene> prepareInitialElements(
            template: T,
            requestedProtectedCount: Int?,
            elements: MutableList<T>
        ): MutableList<T> {
            val rawProtected = requestedProtectedCount ?: elements.size
            if (rawProtected < 0) {
                throw IllegalArgumentException("protectedCount ($rawProtected) cannot be negative")
            }
            if (rawProtected > elements.size) {
                throw IllegalArgumentException(
                    "protectedCount ($rawProtected) cannot exceed initial elements size (${elements.size})"
                )
            }
            if (template is CycleObjectGene || template is LimitObjectGene) {
                return mutableListOf()
            }
            return deduplicateInitialElements(elements)
        }

        private fun <T : Gene> deduplicateInitialElements(elements: MutableList<T>): MutableList<T> {
            if (elements.size <= 1) {
                return elements.toMutableList()
            }
            val kept = mutableListOf<T>()
            for (element in elements) {
                val leaf = element.getLeafGene()
                val applicable = leaf is NumberGene<*> ||
                    leaf is StringGene ||
                    leaf is EnumGene<*> ||
                    leaf is BooleanGene
                val isDuplicate = applicable && kept.any { existing ->
                    val existingLeaf = existing.getLeafGene()
                    existingLeaf.javaClass == leaf.javaClass && existingLeaf.containsSameValueAs(leaf)
                }
                if (!isDuplicate) {
                    kept.add(element)
                }
            }
            return kept
        }
    }

    /**
     * Number of leading elements (`0 until protectedCount`) protected from removal.
     * Defaults to the initial `elements` size, clamped to the deduplicated element count (`super.elements.size`),
     * or `0` when [template] is a [CycleObjectGene] or [LimitObjectGene] placeholder (which forces an empty array).
     */
    val protectedCount: Int = if (template is CycleObjectGene || template is LimitObjectGene) {
        0
    } else {
        minOf(protectedCount ?: elements.size, super.elements.size)
    }

    /**
     * Snapshot of the last valid values of the protected prefix `0 until protectedCount`,
     * used by [repair] to restore colliding protected elements without deleting them when
     * in-place child mutation retries are exhausted (e.g., on saturated enum or numeric domains).
     */
    private val protectedBackup: MutableList<T> = mutableListOf()

    init {
        if (template !is CycleObjectGene && template !is LimitObjectGene) {
            this.minSize = maxOf(this.minSize ?: 0, this.protectedCount)
        }
        refreshProtectedBackup()
    }

    private fun refreshProtectedBackup() {
        protectedBackup.clear()
        for (i in 0 until protectedCount) {
            protectedBackup.add(elements[i].copy() as T)
        }
    }

    private fun restoreProtectedFromBackup() {
        if (protectedBackup.size != protectedCount) return
        for (i in 0 until protectedCount) {
            if (elements[i].unsafeCopyValueFrom(protectedBackup[i])) {
                elements[i].syncBindingGenesBasedOnThis()
            }
        }
    }

    private fun hasProtectedDuplicates(): Boolean {
        if (protectedCount <= 1) return false
        for (i in 0 until protectedCount - 1) {
            if (!isElementApplicableToUniqueCheck(elements[i].getLeafGene())) continue
            for (j in i + 1 until protectedCount) {
                if (hasSameLeafValue(elements[i], elements[j])) {
                    return true
                }
            }
        }
        return false
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return elements.size >= maxOf(protectedCount, getMinSizeOrDefault()) &&
            super.checkForLocallyValidIgnoringChildren()
    }

    override fun mutationCheck(): Boolean {
        val valid = super.mutationCheck()
        if (valid) {
            refreshProtectedBackup()
        }
        return valid
    }

    /**
     * Repairs duplicate elements without deleting any element in the protected prefix `0 until protectedCount`.
     * If a protected element mutated into a duplicate of another protected element, the protected prefix
     * is restored from [protectedBackup] before removing any duplicate trailing elements.
     */
    override fun repair() {
        if (elements.size <= 1) return

        if (hasProtectedDuplicates()) {
            restoreProtectedFromBackup()
        }

        val kept = mutableListOf<T>()
        val duplicates = mutableListOf<T>()
        for (i in elements.indices) {
            val element = elements[i]
            val isDuplicate = isElementApplicableToUniqueCheck(element.getLeafGene()) &&
                kept.any { hasSameLeafValue(it, element) }
            if (isDuplicate && i >= protectedCount) {
                duplicates.add(element)
            } else {
                kept.add(element)
            }
        }
        duplicates.forEach { killChild(it) }
        if (!hasProtectedDuplicates()) {
            refreshProtectedBackup()
        }
    }

    override fun removeExistingElement(element: T) {
        val idx = elements.indexOfFirst { it === element }
        if (idx in 0 until protectedCount) {
            log.warn("Cannot remove protected element at index $idx from ProtectedUniqueArrayGene $name")
            return
        }
        super.removeExistingElement(element)
    }

    /**
     * Randomizes protected elements in place (preserving uniqueness and never removing them),
     * drops unprotected trailing elements, and appends fresh unique elements up to a sampled size.
     */
    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        if (maxSize == 0) {
            return
        }

        while (elements.size > protectedCount) {
            killChildByIndex(elements.size - 1)
        }

        for (i in 0 until protectedCount) {
            val gene = elements[i]
            if (!gene.isMutable()) continue

            val backup = gene.copy()
            gene.randomize(randomness, tryToForceNewValue)

            var attempt = 0
            while (conflictsWithOtherProtectedElement(i, gene) && attempt < MAX_PROTECTED_RANDOMIZE_ATTEMPTS) {
                gene.randomize(randomness, true)
                attempt++
            }
            if (conflictsWithOtherProtectedElement(i, gene)) {
                gene.unsafeCopyValueFrom(backup)
            }
        }

        val minTarget = maxOf(protectedCount, getMinSizeOrDefault())
        val maxTarget = maxOf(minTarget, getMaxSizeUsedInRandomize())
        val targetSize = randomness.nextInt(minTarget, maxTarget)
        while (elements.size < targetSize) {
            val gene = createRandomElement(randomness) ?: break
            addElement(gene)
        }
        refreshProtectedBackup()
    }

    private fun conflictsWithOtherProtectedElement(index: Int, candidate: T): Boolean {
        if (!isElementApplicableToUniqueCheck(candidate.getLeafGene())) return false
        for (j in 0 until protectedCount) {
            if (j != index && hasSameLeafValue(elements[j], candidate)) {
                return true
            }
        }
        return false
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        val effectiveMin = maxOf(protectedCount, getMinSizeOrDefault())
        if (elements.size <= effectiveMin && elements.size >= getMaxSizeOrDefault()) {
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
        val effectiveMin = maxOf(protectedCount, getMinSizeOrDefault())
        val canAdd = elements.size < getMaxSizeOrDefault()
        val canRemove = elements.size > effectiveMin
        if (!canAdd && !canRemove) {
            return false
        }
        if (canAdd && (!canRemove || randomness.nextBoolean())) {
            val gene = createRandomElement(randomness) ?: return false
            addElement(gene)
        } else {
            val idx = randomness.nextInt(protectedCount, elements.size - 1)
            killChildByIndex(idx)
        }
        return true
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        val gene = other.getPhenotype()
        if (gene !is ProtectedUniqueArrayGene<*> ||
            gene.template::class.java.simpleName != template::class.java.simpleName ||
            gene.elements.size < maxOf(protectedCount, getMinSizeOrDefault()) ||
            (maxSize != null && gene.elements.size > maxSize!!)
        ) {
            LoggingUtil.uniqueWarn(
                log,
                "cannot bind ProtectedUniqueArrayGene with template (${template::class.java.simpleName}) with ${gene::class.java.simpleName}"
            )
            return false
        }

        for (i in 0 until protectedCount) {
            if (!elements[i].unsafeCopyValueFrom(gene.elements[i])) {
                restoreProtectedFromBackup()
                return false
            }
        }

        if (hasProtectedDuplicates()) {
            restoreProtectedFromBackup()
            return false
        }

        while (elements.size > protectedCount) {
            killChildByIndex(elements.size - 1)
        }

        for (otherElem in gene.elements.drop(protectedCount)) {
            val cloned = otherElem.copy() as? T ?: return false
            cloned.resetLocalIdRecursively()
            if (this.initialized && !cloned.initialized) {
                cloned.markAllAsInitialized()
            }
            if (!doesExist(cloned)) {
                addChild(cloned)
            }
        }

        val valid = checkForLocallyValidIgnoringChildren()
        if (valid) {
            refreshProtectedBackup()
        }
        return valid
    }

    override fun copyContent(): Gene {
        val copy = ProtectedUniqueArrayGene(
            name = name,
            template = template.copy() as T,
            protectedCount = protectedCount,
            maxSize = maxSize,
            minSize = minSize,
            elements = elements.map { it.copy() as T }.toMutableList(),
            openingTag = openingTag,
            closingTag = closingTag,
            separatorTag = separatorTag
        )
        if (copy.children.size != this.children.size) {
            throw IllegalStateException(
                "copy and its template have different size of children, e.g., copy (${copy.children.size}) vs. template (${this.children.size})"
            )
        }
        return copy
    }
}
