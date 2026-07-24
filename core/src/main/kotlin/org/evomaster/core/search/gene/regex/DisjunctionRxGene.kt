package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.AssertionRepairWalk
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.regex.DisjunctionRxGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * How many times a single [DisjunctionRxGene] tries to fix one of its own direct-term
 * assertions, see [DisjunctionRxGene.attemptAssertionRepair].
 */
const val MAX_LOCAL_ASSERTION_ATTEMPTS = 20

class DisjunctionRxGene(
        name: String,
        val terms: List<Gene>,
        /**  does this disjunction match the beginning of the string, or could it be at any position? */
        var matchStart: Boolean,
        /** does this disjunction match the end of the string, or could it be at any position? */
        var matchEnd: Boolean
) : RxAtom, CompositeFixedGene(name, terms) {

    init{
        if(terms.any { it !is RxTerm }){
            throw IllegalArgumentException("All terms must be RxTerm")
        }
    }

    /**
     * whether we should append a prefix.
     * this can only happen if [matchStart] is false
     */
    var extraPrefix = false

    /**
     * whether we should append a postfix.
     * this can only happen if [matchEnd] is false
     */
    var extraPostfix = false

    companion object{
        private const val APPEND = 0.05
        private val log : Logger = LoggerFactory.getLogger(DisjunctionRxGene::class.java)
    }

    override fun isUnsatisfiable(): Boolean =
        terms.isNotEmpty() && terms.any { (it as? RxTerm)?.isUnsatisfiable() == true }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    /**
     *  to handle "term*", as * can be empty, representing an empty string ""
     */
    override fun canBeChildless() = true

    override fun copyContent(): Gene {
        val copy = DisjunctionRxGene(name, terms.map { it.copy() }, matchStart, matchEnd)
        copy.extraPrefix = this.extraPrefix
        copy.extraPostfix = this.extraPostfix
        return copy
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        terms.filter { it.isMutable() }
                .forEach { it.randomize(randomness, tryToForceNewValue) }

        if (!matchStart) {
            extraPrefix = randomness.nextBoolean()
        }

        if (!matchEnd) {
            extraPostfix = randomness.nextBoolean()
        }
    }

    override fun isMutable(): Boolean {
        return !matchStart || !matchEnd || terms.any { it.isMutable() }
    }

    override fun customShouldApplyShallowMutation(randomness: Randomness,
                                                  selectionStrategy: SubsetGeneMutationSelectionStrategy,
                                                  enableAdaptiveGeneMutation: Boolean,
                                                  additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ) : Boolean {
        if(!matchStart && randomness.nextBoolean(APPEND)){
            return true
        }
        if(!matchEnd && randomness.nextBoolean(APPEND)){
            return true
        }
        return false
    }

    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact == null || additionalGeneMutationInfo.impact !is DisjunctionRxGeneImpact)
            throw IllegalArgumentException("mismatched gene impact")

        if (!terms.containsAll(internalGenes))
            throw IllegalArgumentException("mismatched internal genes")

        val impacts = internalGenes.map {
            additionalGeneMutationInfo.impact.termsImpact[terms.indexOf(it)]
        }

        val selected = mwc.selectSubGene(
                candidateGenesToMutate = internalGenes,
                impacts = impacts,
                targets = additionalGeneMutationInfo.targets,
                forceNotEmpty = true,
                adaptiveWeight = true
        )
        return selected.map { it to additionalGeneMutationInfo.copyFoInnerGene(impacts[internalGenes.indexOf(it)], it) }.toList()
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        if(!matchStart){
            extraPrefix = ! extraPrefix
        } else {
            extraPostfix = ! extraPostfix
        }
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        val prefix = if (extraPrefix) "prefix_" else ""
        val postfix = if (extraPostfix) "_postfix" else ""

        return prefix +
                terms.map { it.getValueAsPrintableString(previousGenes, mode, targetFormat) }
                        .joinToString("") +
                postfix
    }



    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DisjunctionRxGene) {
            return false
        }

        //TODO Man: Andrea, please check this code
        if (terms.size != other.terms.size) return false

        //Man: if terms is empty, there throws IndexOutOfBoundsException (found by rest-scs case study)
        if (terms.isNotEmpty()){
            for (i in 0 until terms.size) {
                if ( this.terms[i]::class.java.simpleName != other.terms[i]::class.java.simpleName ||!this.terms[i].containsSameValueAs(other.terms[i])) {
                    return false
                }
            }
        }

        return this.extraPrefix == other.extraPrefix &&
                this.extraPostfix == other.extraPostfix
    }



    override fun mutationWeight(): Double {
        return terms.filter { isMutable() }.map { it.mutationWeight() }.sum()
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is DisjunctionRxGene
            || other.terms.size != this.terms.size) {
            return false
        }

        var ok = true
        for (i in 0 until terms.size) {
            ok = ok && this.terms[i].unsafeCopyValueFrom(other.terms[i])
        }
        if (ok){
            this.extraPrefix = other.extraPrefix
            this.extraPostfix = other.extraPostfix
        }
        return ok
    }

    /**
     * Delegates to a forward walk over [terms].
     * @see [RxAbsorbable.absorbableCount]
     * @see [AssertionRepairWalk.absorbableCount]
     */
    override fun absorbableCount(value: String): Int =
        AssertionRepairWalk.absorbableCount(terms, value)

    /**
     * True only if every term can independently render "", as this disjunction's own value is
     * the concatenation of all of them.
     * @see [RxAbsorbable.canBeZeroWidth]
     */
    override val canBeZeroWidth: Boolean =
        terms.all { (it as RxAbsorbable).canBeZeroWidth }

    /**
     * Delegates to a forward walk over [terms], mirroring [absorbableCount].
     * @see [RxAbsorbable.tryForce]
     * @see [AssertionRepairWalk.tryForce]
     */
    override fun tryForce(value: String): Int {
        require(value.isNotEmpty())
        return AssertionRepairWalk.tryForce(terms, value)
    }

    /**
     * Forces every term to zero width individually.
     * @see [RxAbsorbable.forceZeroWidth]
     */
    override fun forceZeroWidth() {
        require(canBeZeroWidth)
        terms.forEach { (it as RxAbsorbable).forceZeroWidth() }
    }

    /**
     * Attempts to repair this disjunction's own value so that each of its direct-term
     * [AssertionRxGene]s is actually satisfied, by forcing the assertion's sampled inner
     * value onto the genes that follow it within [terms].
     */
    fun attemptAssertionRepair(randomness: Randomness) {
        if (terms.none { it is AssertionRxGene }) {
            return
        }

        for (idx in terms.indices) {
            val assertion = terms[idx] as? AssertionRxGene ?: continue
            if (assertion.innerGene == null) {
                continue
            }

            val genesAfter = terms.subList(idx + 1, terms.size).filter { it !is AssertionRxGene }
            if (genesAfter.isEmpty()) {
                return
            }

            var satisfied = false
            for (attempt in 0 until MAX_LOCAL_ASSERTION_ATTEMPTS) {
                assertion.randomize(randomness, false)
                val candidate = assertion.sampledInnerValue() ?: break
                if (candidate.isEmpty()
                    || AssertionRepairWalk.absorbableCount(genesAfter, candidate) == candidate.length) {
                    if (candidate.isNotEmpty()) {
                        AssertionRepairWalk.tryForce(genesAfter, candidate)
                    }
                    satisfied = true
                    break
                }
            }
            if (!satisfied) {
                return
            }
        }
    }
}
