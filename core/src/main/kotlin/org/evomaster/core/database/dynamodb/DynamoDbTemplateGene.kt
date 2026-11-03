package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionKeyBuilder
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import java.math.BigDecimal

/**
 * Represents an evolvable DynamoDB document attribute (MAP or LIST).
 * Retains the structural template (keys, hierarchy, types) while promoting
 * nested scalar leaf values into mutable, bindable child genes.
 *
 * @param name attribute name
 * @param value template DTO holding the structure
 * @param childGenes optional explicit child genes for promoted leaves
 */
class DynamoDbTemplateGene @JvmOverloads constructor(
    name: String,
    value: DynamoDbAttributeValueDto,
    childGenes: List<Gene>? = null
) : Gene(name, mutableListOf()) {

    /**
     * Represents a promoted scalar leaf within the template hierarchy.
     *
     * @property path navigation path segments (attribute names or list indices) to this leaf
     * @property gene evolvable leaf gene
     * @property type native DynamoDB scalar value type
     */
    data class PromotedLeaf(
        val path: List<Any>,
        val gene: Gene,
        val type: DynamoDbValueTypeDto
    )

    private val snapshot: DynamoDbAttributeValueDto = value.copy()
    private var promotedLeaves: List<PromotedLeaf>

    init {
        val extracted = extractLeaves(snapshot)
        if (childGenes != null) {
            require(extracted.size == childGenes.size) {
                "Mismatch between extracted leaves (${extracted.size}) and child genes (${childGenes.size})"
            }
            promotedLeaves = extracted.indices.map { i ->
                extracted[i].copy(gene = childGenes[i])
            }
        } else {
            promotedLeaves = extracted
        }
        addChildren(promotedLeaves.map { it.gene })
    }

    /**
     * Replaces a child gene when constraints are intersected or bound.
     *
     * @param oldGene child gene to replace
     * @param newGene replacement child gene
     */
    fun replaceChildGene(oldGene: Gene, newGene: Gene) {
        val index = children.indexOf(oldGene)
        if (index >= 0) {
            killChildByIndex(index)
            addChildren(index, listOf(newGene))
            promotedLeaves = promotedLeaves.map {
                if (it.gene == oldGene) it.copy(gene = newGene) else it
            }
        }
    }

    /**
     * Returns an independent typed tree with updated promoted leaf values.
     *
     * @return deep copy of template DTO updated with current values from promoted leaf genes
     */
    fun toDto(): DynamoDbAttributeValueDto {
        val result = snapshot.copy()
        promotedLeaves.forEach { leaf ->
            updateLeafValue(result, leaf.path, leaf.gene.getValueAsRawString())
        }
        return result
    }

    override fun copyContent(): Gene {
        val copiedChildren = children.map { (it as Gene).copy() }
        return DynamoDbTemplateGene(name, snapshot, copiedChildren)
    }

    override fun isMutable(): Boolean = children.any { (it as Gene).isMutable() }

    override fun requiresRandomInitialization(): Boolean = false

    override fun isPrintable() = false

    override fun checkForLocallyValidIgnoringChildren() = true

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        children.forEach { (it as Gene).randomize(randomness, tryToForceNewValue) }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean = false

    override fun shallowMutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean = false

    override fun adaptiveSelectSubsetToMutate(
        randomness: Randomness,
        internalGenes: List<Gene>,
        mwc: MutationWeightControl,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        val s = randomness.choose(internalGenes)
        return listOf(s to additionalGeneMutationInfo.copyFoInnerGene(null, s))
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String = DynamoDbInsertionKeyBuilder.fromAttributes("", listOf(toDto()))

    override fun containsSameValueAs(other: Gene): Boolean =
        other is DynamoDbTemplateGene && toDto() == other.toDto()

    override fun mutationWeight(): Double = if (isMutable()) 1.0 else 0.0

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is DynamoDbTemplateGene) return false
        if (children.size != other.children.size) return false
        for (i in children.indices) {
            val thisChild = children[i] as Gene
            val otherChild = other.children[i] as Gene
            if (!thisChild.unsafeCopyValueFrom(otherChild)) return false
        }
        return true
    }

    companion object {
        private const val MAX_PROMOTED_LEAVES = 50

        /**
         * Extracts promoted leaves from a template DTO.
         *
         * @param dto template DTO to extract leaves from
         * @param currentPath navigation path segments accumulated so far
         * @param fallbackName fallback name for leaf genes when attributeName is missing
         * @return list of promoted leaves up to MAX_PROMOTED_LEAVES
         */
        private fun extractLeaves(
            dto: DynamoDbAttributeValueDto,
            currentPath: List<Any> = emptyList(),
            fallbackName: String = dto.attributeName ?: "element"
        ): List<PromotedLeaf> {
            val result = mutableListOf<PromotedLeaf>()
            collectLeaves(dto, currentPath, fallbackName, result)
            return result.take(MAX_PROMOTED_LEAVES)
        }

        /**
         * Recursively collects promoted leaves from a DTO node.
         *
         * @param dto current node in the DTO tree
         * @param currentPath navigation path segments to current node
         * @param name name for the leaf gene
         * @param out destination list accumulating leaves
         */
        private fun collectLeaves(
            dto: DynamoDbAttributeValueDto,
            currentPath: List<Any>,
            name: String,
            out: MutableList<PromotedLeaf>
        ) {
            if (out.size >= MAX_PROMOTED_LEAVES) return

            when (dto.type) {
                DynamoDbValueTypeDto.STRING -> {
                    val gene = StringGene(name, dto.value ?: "")
                    out.add(PromotedLeaf(currentPath, gene, DynamoDbValueTypeDto.STRING))
                }
                DynamoDbValueTypeDto.NUMBER -> {
                    val num = try {
                        BigDecimal(dto.value?.ifBlank { "0" } ?: "0")
                    } catch (e: Exception) {
                        BigDecimal.ZERO
                    }
                    val gene = BigDecimalGene(name, num)
                    out.add(PromotedLeaf(currentPath, gene, DynamoDbValueTypeDto.NUMBER))
                }
                DynamoDbValueTypeDto.BOOLEAN -> {
                    val gene = BooleanGene(name, dto.value?.toBoolean() ?: false)
                    out.add(PromotedLeaf(currentPath, gene, DynamoDbValueTypeDto.BOOLEAN))
                }
                DynamoDbValueTypeDto.MAP -> {
                    dto.mapValue?.entries?.sortedBy { it.key }?.forEach { (key, childDto) ->
                        collectLeaves(childDto, currentPath + key, key, out)
                    }
                }
                DynamoDbValueTypeDto.LIST -> {
                    dto.listValue?.forEachIndexed { index, childDto ->
                        val elemName = childDto.attributeName ?: name
                        collectLeaves(childDto, currentPath + index, elemName, out)
                    }
                }
                else -> { /* NULL or unsupported types: remain unchanged in template */ }
            }
        }

        /**
         * Updates a leaf value within a cloned DTO tree at the specified path.
         *
         * @param root root DTO to update in place
         * @param path navigation path segments to the target leaf
         * @param newValue new raw string value to assign to the leaf
         */
        private fun updateLeafValue(root: DynamoDbAttributeValueDto, path: List<Any>, newValue: String) {
            if (path.isEmpty()) {
                root.value = newValue
                return
            }
            var current: DynamoDbAttributeValueDto = root
            for (i in 0 until path.size - 1) {
                when (val segment = path[i]) {
                    is String -> current = current.mapValue?.get(segment) ?: return
                    is Int -> current = current.listValue?.getOrNull(segment) ?: return
                    else -> return
                }
            }
            when (val last = path.last()) {
                is String -> current.mapValue?.get(last)?.value = newValue
                is Int -> current.listValue?.getOrNull(last)?.value = newValue
            }
        }
    }
}
