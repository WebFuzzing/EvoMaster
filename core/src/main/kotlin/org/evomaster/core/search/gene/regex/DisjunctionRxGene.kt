package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.AssertionRepairResult
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

/**
 * One nested group's own unresolved requirement, as settled by [DisjunctionRxGene.settleNestedGroups]
 * and resolved by [DisjunctionRxGene.resolveNestedGroupRequirements]. [termIndex] is that group's
 * own index in [DisjunctionRxGene.terms].
 */
private data class NestedGroupRequirement(val termIndex: Int, val result: AssertionRepairResult)

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
        AssertionRepairWalk.absorbableCount(terms, value).consumed

    /**
     * Delegates to a backward walk over [terms]. Mirrors [absorbableCount], walking
     * right-to-left since lookbehind's target sits before the assertion.
     * @see [RxAbsorbable.absorbableSuffixCount]
     */
    override fun absorbableSuffixCount(value: String): Int =
        AssertionRepairWalk.absorbableSuffixCount(terms, value).consumed

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
        return AssertionRepairWalk.tryForce(terms, value).consumed
    }

    /**
     * Delegates to a backward walk over [terms], mirroring [tryForce] in the opposite
     * direction.
     * @see [RxAbsorbable.tryForceSuffix]
     */
    override fun tryForceSuffix(value: String): Int {
        require(value.isNotEmpty())
        return AssertionRepairWalk.tryForceSuffix(terms, value).consumed
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
     * value onto the genes on the appropriate side of it within [terms]:
     * - Forward, onto [terms] after it, for [AssertionType.LOOKAHEAD]
     * - Backward, onto [terms] before it, for [AssertionType.LOOKBEHIND]
     *
     * Also, recurses into any direct term that is itself a nested group repairing
     * it first, and resolving whatever it couldn't satisfy locally against this scope's own
     * neighboring terms. Runs as three passes, in order: [settleNestedGroups],
     * [resolveNestedGroupRequirements], [repairDirectAssertions].
     *
     *  Note: forcing here is sequential and uncoordinated, so a later force can overwrite an
     *  earlier one. Still sound as the top-level pattern check catches any resulting mismatch.
     *
     * @return whether repair succeeded, with possible outside requirements.
     */
    fun attemptAssertionRepair(randomness: Randomness): AssertionRepairResult {
        if (terms.none { it is AssertionRxGene || it is DisjunctionListRxGene }) {
            return AssertionRepairResult.SUCCESS
        }

        val nestedGroupRequirements = settleNestedGroups(randomness)
            ?: return AssertionRepairResult.FAILURE

        val nestedResult = resolveNestedGroupRequirements(nestedGroupRequirements)
        if (!nestedResult.success) {
            return AssertionRepairResult.FAILURE
        }

        val directResult = repairDirectAssertions(randomness)
        if (!directResult.success) {
            return AssertionRepairResult.FAILURE
        }

        return AssertionRepairResult(
            success = true,
            neededPrefix = directResult.neededPrefix ?: nestedResult.neededPrefix,
            neededPostfix = directResult.neededPostfix ?: nestedResult.neededPostfix
        )
    }

    /**
     * Pass 1 of [attemptAssertionRepair]: settles every nested group's own internal repair,
     * left to right, before anything in this scope uses it as a forcing target.
     *
     * @return the outward requirements for each nested group's own term index, in ascending index order
     * (left to right, matching [terms] itself). `null` if any nested group's own repair failed outright.
     */
    private fun settleNestedGroups(randomness: Randomness): List<NestedGroupRequirement>? {
        val nestedGroupRequirements = mutableListOf<NestedGroupRequirement>()
        for (idx in terms.indices) {
            val term = terms[idx] as? DisjunctionListRxGene ?: continue
            val result = term.attemptAssertionRepair(randomness)
            if (!result.success) {
                return null
            }
            if (result.neededPrefix != null || result.neededPostfix != null) {
                nestedGroupRequirements.add(NestedGroupRequirement(idx, result))
            }
        }
        return nestedGroupRequirements
    }

    /**
     * Pass 2 of [attemptAssertionRepair]: resolves each nested group's own outward requirement
     * (as settled by [settleNestedGroups]) against this scope's own neighboring terms.
     *
     * @return [AssertionRepairResult.FAILURE] if resolving any of them failed outright; otherwise
     * a successful result carrying whatever this scope itself must still propagate outward.
     */
    private fun resolveNestedGroupRequirements(nestedGroupRequirements: List<NestedGroupRequirement>): AssertionRepairResult {
        var pending = AssertionRepairResult.SUCCESS
        for ((idx, requirement) in nestedGroupRequirements) {
            requirement.neededPrefix?.let { requirement ->
                pending = pending.mergedWith(resolveOutwardRequirement(requirement, genesBefore(idx), backward = true))
            }
            if (!pending.success) {
                return AssertionRepairResult.FAILURE
            }
            requirement.neededPostfix?.let { requirement ->
                pending = pending.mergedWith(resolveOutwardRequirement(requirement, genesAfter(idx), backward = false))
            }
            if (!pending.success) {
                return AssertionRepairResult.FAILURE
            }
        }
        return pending
    }

    /**
     * Pass 3 of [attemptAssertionRepair]: repairs every direct-term assertion in [terms].
     *
     * @return [AssertionRepairResult.FAILURE] if repairing any of them failed outright; otherwise
     * a successful result carrying whatever this scope's own assertions still need propagated
     * outward.
     */
    private fun repairDirectAssertions(randomness: Randomness): AssertionRepairResult {
        var pending = AssertionRepairResult.SUCCESS
        for (idx in terms.indices) {
            val assertion = terms[idx] as? AssertionRxGene ?: continue
            val assertionType = assertion.assertionType
            val backward = assertionType.direction == Direction.BACKWARD
            val target = if (backward) genesBefore(idx) else genesAfter(idx)

            val resolution = when {
                !assertionType.hasContent -> repairBoundaryAssertion(target, backward)
                target.isEmpty() -> repairAssertionWithNoTarget(assertion, backward, randomness)
                else -> repairAssertionAgainstTarget(assertion, target, backward, randomness)
            }
            pending = pending.mergedWith(resolution)
            if (!pending.success) {
                return AssertionRepairResult.FAILURE
            }
        }
        return pending
    }

    /**
     * Handles an assertion with nothing local to force onto: escapes zero-width if the assertion
     * itself allows it, otherwise samples once and escapes the whole candidate outward.
     */
    private fun repairAssertionWithNoTarget(assertion: AssertionRxGene, backward: Boolean, randomness: Randomness): AssertionRepairResult {
        val innerGene = assertion.innerGene!!
        if (innerGene.canBeZeroWidth) {
            innerGene.forceZeroWidth()
            return AssertionRepairResult.SUCCESS
        }
        assertion.randomize(randomness, false)
        val candidate = assertion.sampledInnerValue()!!
        return AssertionRepairResult.stillNeeded(candidate, backward)
    }

    /**
     * Resamples [assertion] up to [MAX_LOCAL_ASSERTION_ATTEMPTS] times looking for a candidate
     * [target] fully absorbs; if none does, escapes the last candidate tried outwards. This mirrors
     * the same outcome [resolveOutwardRequirement] produces.
     */
    private fun repairAssertionAgainstTarget(assertion: AssertionRxGene, target: List<Gene>, backward: Boolean, randomness: Randomness): AssertionRepairResult {
        val countFunction = countWalkFunction(backward)
        val forceFunction = forceWalkFunction(backward)

        var lastCandidate: String? = null
        for (attempt in 0 until MAX_LOCAL_ASSERTION_ATTEMPTS) {
            assertion.randomize(randomness, false)
            val candidate = assertion.sampledInnerValue()!!
            if (candidate.isEmpty()) {
                return AssertionRepairResult.SUCCESS
            }
            if (countFunction(target, candidate).consumed == candidate.length) {
                forceFunction(target, candidate)
                return AssertionRepairResult.SUCCESS
            }
            // Read-only for now, try to force full match before escaping partial match.
            lastCandidate = candidate
        }

        val candidate = lastCandidate ?: return AssertionRepairResult.FAILURE
        return resolveOutwardRequirement(candidate, target, backward)
    }

    /**
     * The genes in [terms] lying before index [idx], excluding other assertions. This is the forcing
     * target for a [AssertionType.LOOKBEHIND] assertion (or an outward requirement) sitting at [idx].
     */
    private fun genesBefore(idx: Int): List<Gene> =
        terms.subList(0, idx).filter { it !is AssertionRxGene }

    /**
     * The genes in [terms] lying after index [idx], excluding other assertions. This is the forcing
     * target for a [AssertionType.LOOKAHEAD] assertion (or an outward requirement) sitting at [idx].
     */
    private fun genesAfter(idx: Int): List<Gene> =
        terms.subList(idx + 1, terms.size).filter { it !is AssertionRxGene }

    /**
     * The read-only walk function for [backward].
     */
    private fun countWalkFunction(backward: Boolean) = if (backward) {
        AssertionRepairWalk::absorbableSuffixCount
    } else {
        AssertionRepairWalk::absorbableCount
    }

    /**
     * The mutating counterpart of [countWalkFunction], for the same [backward] direction.
     */
    private fun forceWalkFunction(backward: Boolean) = if (backward) {
        AssertionRepairWalk::tryForceSuffix
    } else {
        AssertionRepairWalk::tryForce
    }

    /**
     * Resolves a "" (empty) requirement: every gene in [target] must collapse to zero width.
     */
    private fun resolveEmptyRequirement(target: List<Gene>, backward: Boolean): AssertionRepairResult {
        if (target.any { !(it as RxAbsorbable).canBeZeroWidth }) {
            return AssertionRepairResult.FAILURE
        }
        target.forEach { (it as RxAbsorbable).forceZeroWidth() }
        return AssertionRepairResult.stillNeeded("", backward)
    }

    /**
     * Resolves an outward [requirement] against [target], a list of this scope's own genes lying to one
     * side of wherever the requirement originated. [backward] selects which direction [target] is walked.
     */
    private fun resolveOutwardRequirement(requirement: String, target: List<Gene>, backward: Boolean): AssertionRepairResult {
        if (requirement.isEmpty()) {
            return resolveEmptyRequirement(target, backward)
        }
        if (target.isEmpty()) {
            return AssertionRepairResult.stillNeeded(requirement, backward)
        }

        val countFunction = countWalkFunction(backward)
        val outcome = countFunction(target, requirement)
        if (outcome.hardMismatch) {
            return AssertionRepairResult.FAILURE
        }

        val forceFunction = forceWalkFunction(backward)
        forceFunction(target, requirement)

        val consumed = outcome.consumed
        return if (consumed == requirement.length) {
            AssertionRepairResult.SUCCESS
        } else {
            val remainder = if (backward) requirement.dropLast(consumed) else requirement.drop(consumed)
            AssertionRepairResult.stillNeeded(
                remainder,
                backward
            )
        }
    }

    /**
     * Repair input boundary assertions (`^` and `$` for example) by forcing taget (and whatever follows) to zero width.
     */
    private fun repairBoundaryAssertion(target: List<Gene>, backward: Boolean): AssertionRepairResult =
        resolveOutwardRequirement("", target, backward)
}