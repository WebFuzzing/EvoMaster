package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.evomaster.core.Lazy
import org.evomaster.core.problem.enterprise.EnterpriseIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.RootElement
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.SearchGlobalState


/**
 *
 * A building block representing one part of an Individual.
 * The terms "gene" comes from the evolutionary algorithm literature
 *
 * When creating a new Gene type, should not extend directly from this class, but rather
 * from [SimpleGene], [CompositeGene] or [CompositeFixedGene], or any of their subclasses.
 * There are test cases to impose this property.
 *
 *
 * TO enable adaptive hypermutation
 * 1. override [mutationWeight] if the gene is not simple gene, e.g., it is complex with many genes inside
 *
 * 2. if the gene has inner genes, then we need to collect impact info.
 * Implement an impact (a subclass of [GeneImpact]) for the new gene to collect impact info for gene mutation.
 * Impact here is referred to how the gene is influencing the fitness.
 * For instance, see [org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact]
 *  we collect impacts for each field, then could guide on which field to be selected for mutation.
 * See more details in comments of [org.evomaster.core.search.impact.impactinfocollection.GeneImpact]
 *
 * 3. override [mutablePhenotypeChildren] to decide 1) whether to apply selection for the internal genes
 *  2) what candidates are in [this] gene to be selected for mutation, eg, mutable fields for ObjectGene.
 *      More info could be found with comments of the method.
 *
 * 4. with the collected impact info, override [adaptiveSelectSubsetToMutate] to decide which gene to be selected
 *
 */
abstract class Gene(
        /**
         * The name for this gene, mainly needed for debugging.
         * One actual use is for binding, e.g., paremeters in HTTP requests
         */
        var name: String,
        /**
         * The direct node inside this Gene (excluding templates).
         * These children might have their own children.
         * Note that children while have links back to their "parents".
         */
        children: MutableList<out Gene>
) : StructuralElement(
    children,
    {k -> Gene::class.java.isAssignableFrom(k)}
    ){

    /*
        TODO Major refactoring still to do:
        - mutation of gene (including hypermutation and innerGene)
        - impact of genes
        - for binding, we ll need tests on Individual (started in core-it)
        - how to print (this is going to be painful...)
        - validity / robustness testing (will need new ChoiceGene)
     */

    companion object{
        private val log: Logger = LoggerFactory.getLogger(Gene::class.java)
    }

    /**
     * Whether this gene has been initialized, and can be used.
     * Note that gene can have validity constraints, and those might not be satisfied
     * when the constructor of a gene is called to create a new instance.
     */
    var initialized : Boolean = false
        private set

    /**
     * In the scope of [Individual] and not genes in isolation.
     * The current gene could be "bound" to other genes.
     * This means that, if this gene is modified, then ALL the other
     * bound gene must be updated as well.
     * The type can be different, eg strings vs numbers, but still consistent.
     *
     * WARNING: genes are mutable, but here we check for references. this implies
     * NO gene can overridde hashcode.
     *
     * If A is bound to B, then as well B is bound to A.
     * If [this] is A, then binding genes will contain only B, and the binding genes of B
     * will contain [this] A.
     *
     * If A bound (->) B, and B->C, then C->A.
     * If [this] is A, then [this] as binding genes will contain B
     * but not "necessary" C... ???
     * TODO will need to be clarified and tested on all conditions
     *
     * A gene X is never bound to itself.
     *
     * In other words, this relationship is symmetric, transitive but not reflexive
     */
    private val bindingGenes: MutableSet<Gene> = mutableSetOf()

    init{
        if(name.isBlank()){
            throw IllegalArgumentException("Empty name for Gene")
        }
    }

    override fun hashCode(): Int {
        //Otherwise bindingGenes will not work
        return super.hashCode()
    }

    /*
        TODO: make sure to call it not only on mutation, but also on printing out
     */
    private fun checkInitialized(){
        if(! initialized)
            throw IllegalStateException("Trying to use a gene that is not initialized")
    }


    /**
     * Return all direct children of this gene.
     * Note that a gene can only have genes inside, and not other types of structural elements.
     * These children might have their own children, and those are NOT directly returned here.
     * If you want to see the whole tree, use flatView().
     *
     * Note that some genes might have special "template" genes, and those are never marked as children.
     *
     * In case one has several children, but only 1 is actually impacting the phenotype, still all the other
     * siblings are returned as well.
     * Think about a disjunction in a regex like A | B | C, only 1 gene would be active.
     * Same thing for Optional and Nullable genes.
     *
     * TODO add tests for this invariant, but might need refactoring of innerGene
     */
    override  val children : MutableList<Gene>
        get() = super.children as MutableList<Gene>

    final override fun getViewOfChildren() : List<Gene> = children

    /**
     * Initialize this gene with random data, as well as initializing all
     * of its children, recursively.
     *
     * A gene cannot be used (eg, mutated or printed in the phenotype) before it is initialized
     *
     * if [rand] is provided, the gene will be randomized. this should always be provided, unless
     * you are building tests directly. Otherwise, the default values might not be within the given
     * constraints (if any). For example, a default 0 int would fail a constraint min=42.
     *
     * If a gene is already initialized, this will fail.
     * The reasoning is to avoid building manual tests (eg via seeding) and randomize them
     * by mistake by doing a second doInitialize call.
     * On the other hand, randomize a gene twice (or more) by mistake at initialization is not
     * a huge deal, apart from wasted CPU cycles.
     */
    fun doInitialize(rand: Randomness? = null){
        if(initialized){
            throw IllegalStateException("Gene already initialized")
        }
        if(rand != null && isMutable()) {
            randomize(rand, false)
        }
        markAllAsInitialized()
        Lazy.assert{isLocallyValid()}
    }


    override fun addChild(child: StructuralElement){
        checkChildGeneToAdd(child)
        super.addChild(child)
    }

    override fun addChild(position: Int, child: StructuralElement){
        checkChildGeneToAdd(child)
        super.addChild(position, child)
    }

    override fun addChildren(position: Int, list : List<StructuralElement>){
        list.forEach { checkChildGeneToAdd(it) }
        super.addChildren(position, list)
    }

    private fun checkChildGeneToAdd(child: StructuralElement) {
        if (this.initialized && !(child as Gene).initialized) {
            throw IllegalArgumentException("Trying to add non-initialized gene inside an initialized gene")
        }
    }


    /**
     *  this is done once a gene is already initialized, and mounted inside an individual.
     *  this is to deal with all intra-gene dependencies (eg. foreign keys) or when needing
     *  references to global state
     */
    fun doGlobalInitialize(){
        if(!initialized){
            throw IllegalStateException("The gene was not locally initialized")
        }
        val ind = getRoot()
        if(ind !is Individual){
            throw IllegalStateException("The gene is not mounted inside an individual")
        }
        if(ind.searchGlobalState == null){
            throw IllegalStateException("Search Global State was not setup for the individual")
        }

        if (ind !is EnterpriseIndividual || ind.sampleType != SampleType.SEEDED)
            applyGlobalUpdates()

        children.forEach { it.doGlobalInitialize() }
    }

    /**
     * Once the gene is mounted inside an individual, make sure to apply all updates that
     * depend on the other genes and are specific for this one (eg, foreign-key).
     * When this is called, all genes are mounted and locally initialized, but the order in which
     * this global initialization is performed is not guaranteed (for now...)
     */
    protected open fun applyGlobalUpdates(){}

    /*
        TODO needed for copies. check if can be refactored, eg if copyContent enforce the copy of initialized

        will need to be removed / made private, and refactor all its callers
     */
    fun markAllAsInitialized(){
        flatView().forEach{it.initialized = true}
        initialized = true
    }

    /**
     * Make a copy of this gene.
     */
    final override fun copy() : Gene{
        val copy = super.copy()
        if (copy !is Gene)
            throw IllegalStateException("mismatched type: the type should be Gene, but it is ${this::class.java.simpleName}")
        copy.initialized = initialized
        copy.flatView().forEach{it.initialized = initialized}
        return copy
    }

    /*
     * override to force return type Gene
     */
    protected abstract override  fun copyContent(): Gene


    override fun postCopy(original: StructuralElement) {
        //rebuild the binding genes
        val root = getRoot()
        if(root is RootElement) {
            /*
                a gene can refer to other genes outside of its tree.
                when we make a copy we need to make sure that we refer to the new gene in the copied
                individual, not the original individual.
                so, this is applied only  when the root is an individual, otherswise skipped, because
                would not be able to find those genes anyway
             */
            val postBinding = (original as Gene).bindingGenes.map { b ->
                val found = root.find(b)
                found as? Gene
                        ?: throw IllegalStateException("mismatched type between template (${b::class.java.simpleName}) and found (${found::class.java.simpleName})")
            }
            bindingGenes.clear()
            bindingGenes.addAll(postBinding)
            Lazy.assert { !bindingGenes.contains(this) }
        } else {
            assert(bindingGenes.isEmpty())
        }


        super.postCopy(original)
    }


    /**
     * Some genes are wrapper to provide extra functionality and control on how genes are sampled, mutated and
     * impacting the phenotype.
     * Here, we search for wrapped child with given type.
     * Note that containers like objects and arrays are not wrappers.
     * See for example Optional and Choice genes.
     *
     * Will return [this] if of the specified type, otherwise [null].
     * Wrapper genes, and only those, will override this method to check their children
     */
    open  fun <T> getWrappedGene(klass: Class<T>) : T?  where T : Gene{

        if(this.javaClass == klass){
            return this as T
        }

        return null
    }

    /**
     * there might be a need to repair gene based on some constraints, e.g., DateGene and TimeGene
     *
     * TODO likely this will be removed once we deal with ChoiceGene for robustness testing
     *
     * TODO it is actually bit more complicated... not just for robustness (time/date that will need refactoring),
     * but also for intra-children constraints: ie modifying one child might brake constraints in parent,
     * see SqlRangeGene as an example
     */
    open fun repair(){
        //do nothing
    }

    /**
     * @return whether the gene is locally valid,
     *  based on any specialized rule for different types of genes, if there exist.
     *  "Locally" here means that the constraints are based only on the current gene and all its children,
     *  but not genes up in the hierarchy and in other actions.
     *
     * Note that the method is only used for debugging and testing purposes.
     *  e.g., for NumberGene, if min and max are specified, the value should be within min..max.
     *        for FloatGene with precision 2, the value 10.222 would not be considered as a valid gene.
     * Sampling a gene at random until is valid could end up in an infinite loop, so should be avoided.
     *
     * Validity is based only internal constraints. if those constraints lead to meaningless data (eg
     * a date object with month index 42), it would still be "valid".
     *
     * Note that, if a gene is valid, then all of its children must be valid as well, regardless of whether
     * they are having any effect on the phenotype.
     *
     * A default implementation could be:
     *        return getViewOfChildren().all { it.isLocallyValid() }
     * but here we want to force new genes to explicitly write this method
     */
    abstract fun isLocallyValid() : Boolean

    /**
     *  Verify all constraints (including locals).
     *  This is necessary when constraints involved more than 1 gene, possibly
     *  in different actions.
     */
    open fun isGloballyValid() : Boolean {
        if(! isLocallyValid()){
            return false
        }
        //TODO check bindings

        return checkForGloballyValid() && getViewOfChildren().all { it.isGloballyValid() }
    }

    override fun callWinstonWolfe() {
        super.callWinstonWolfe()

        removeThisFromItsBindingGenes()
        //TODO in future will deal with FK as well here
    }

    protected open fun checkForGloballyValid() = true

    /**
     * mutated gene should pass the check if needed, eg, DateGene
     *
     * In some cases, we must have genes with 'valid' values.
     * For example, a date with month 42 would be invalid.
     * On the one hand, it can still be useful for robustness testing
     * to provide such invalid values in a HTTP call. On the other hand,
     * it would be pointless to try to add it directly into a database,
     * as that SQL command would simply fail without any SUT code involved.
     *
     * FIXME This will be removed once we refactor how we do Robustness Testing using
     * ChoiceGene
     */
    @Deprecated("will be removed")
    open fun mutationCheck() : Boolean = true


    /*
        TODO shall we remove to default function implementation? to make sure new
        genes are forced to set them up, and not forget about them?
        or can we write invariants which will make tests fail in those cases?
     */

    /**
     * weight for mutation
     * For example, higher the weight, the higher the chances to be selected for mutation
     */
    open fun mutationWeight() : Double = 1.0

    /**
     * Specify if this gene can be mutated during the search.
     * Typically, it will be true, apart from some special cases.
     */
    open fun isMutable() = true

    /**
     * Specify if this gene should be printed in the output test.
     * In other words, if this genotype directly influences the
     * phenotype
     */
    open fun isPrintable() = true


    /**
     *
     *   Randomize the content of this gene.
     *   After a gene is randomized, it MUST be locally valid.
     *
     *   @param randomness the source of non-determinism
     *   @param tryToForceNewValue whether we should force the change of value. When we do mutation,
     *          it could otherwise happen that a value is replaced with itself.
     *          This is not 100% enforced, it is more like a "strong recommendation"
     *
     */
    abstract fun randomize(randomness: Randomness,tryToForceNewValue: Boolean)


    /**
     * Return reference to the singleton contain all shared info for the whole search.
     * This is going to be null if the gene is not mounted inside an individual.
     * Otherwise, it MUST not be null.
     */
    fun getSearchGlobalState() : SearchGlobalState? {
        val root = getRoot()
        if(root is Individual) {
            val sgt =  root.searchGlobalState
            //assert(sgt != null) //these would fail in integration tests where individuals are created without a sampler
            return sgt
        }
        return null
    }


    /**
     * When mutating a gene, we could mutate its fields (ie shallow mutation) or mutate some of its
     * children (if any).
     */
    private fun shouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ) : Boolean {

        if(selectionStrategy == SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT &&
                additionalGeneMutationInfo == null){
            throw IllegalArgumentException("Trying adaptive weight selection, but with no info as input")
        }

        if(children.none { it.isMutable() }){
            //no mutable child, so always apply shallow mutate
            return true
        }

       return customShouldApplyShallowMutation(randomness, selectionStrategy, enableAdaptiveGeneMutation, additionalGeneMutationInfo)
    }

    /**
     * Determine whether should apply a shallow mutation.
     * Note at this point it has already been checked if there is any child.
     * If there is no child, this method is never called.
     *
     * If the gene has no internal state besides children, then this method should always return false.
     * Otherwise, it should non-deterministically decide whether a shallow mutation (ie mutation
     * of internal variables) is applied or a mutation of children.
     *
     * Note: if the implementation of this method can return true, then must override shallowMutation
     *
     * To get better results, would be important to implement the logic for adaptive mutation, for all genes that
     * need it. But not necessary in first implementation of a new gene.
     * TODO go through every single gene...
     *
     * @param selectionStrategy a strategy to select internal genes to mutate
     * @param enableAdaptiveGeneMutation whether apply adaptive gene mutation, e.g., archive-based gene mutation
     * @param additionalGeneMutationInfo contains additional info for gene mutation
     */
    abstract fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean, //FIXME this might not be needed here
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ) : Boolean


    /**
     * A mutation is just a small change.
     * Apply a mutation to the current gene.
     * Regarding the gene:
     * 1) there might exist multiple internal genes, i.e.,[mutablePhenotypeChildren].
     *  In this case, we first apply [selectSubsetToMutate] to select a subset of internal genes.
     *  then apply mutation on each of the selected genes.
     * 2) When there is no need to do further selection, we apply [shallowMutate] on the current gene.
     * 3) if either internal genes (ie children impacting phenotype) or current gene (ie with shallowMutate)
     *   can be mutated, the choice is based with different strategies
     *
     *   @param randomness the source of non-determinism
     *   @param apc adatpive parameter control singleton
     *   @param mwc mutation weight control
     *   @param interalGeneSelectionStrategy a strategy to select internal genes to mutate.
     *          In hypermutation, several genes could be mutated at same time.
     *          The choice of what to mutate depends on the "weight" of the genes, and possible on historical data
     *   @param enableAdaptiveGeneValueMutation whether to apply adaptive gene mutation for values (ie, not for choice
     *          of children to mutate with hypermutation), e.g., archive-based gene mutation.
     *          This is the case when using history data when mutating number and string values,
     *          e.g., [HistoryBasedMutationGene]
     *   @param additionalGeneMutationInfo contains additional info for gene mutation
     *          TODO what are the cases in which it is expected to be null? when should it NEVER be null?
     */
    fun standardMutation(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        childrenToMutateSelectionStrategy: SubsetGeneMutationSelectionStrategy = SubsetGeneMutationSelectionStrategy.DEFAULT,
        enableAdaptiveGeneValueMutation: Boolean = false,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo? = null
    ){
        checkInitialized()
        Lazy.assert { this.isMutable() }

        //if impact is not able to obtain, adaptive-gene-mutation should also be disabled
        val applyShallow = shouldApplyShallowMutation(randomness, childrenToMutateSelectionStrategy, enableAdaptiveGeneValueMutation, additionalGeneMutationInfo)

        if (applyShallow){
            val mutated = shallowMutate(randomness, apc, mwc, childrenToMutateSelectionStrategy, enableAdaptiveGeneValueMutation, additionalGeneMutationInfo)
            if (!mutated)
                throw IllegalStateException("leaf mutation is not implemented for ${this::class.java.simpleName}")
        }else{

            val mutablePhenotypeGenes = mutablePhenotypeChildren()
            Lazy.assert {
                mutablePhenotypeGenes.isNotEmpty() // otherwise shallow mutation should had been applied
                        && mutablePhenotypeGenes.none { it == this } // cannot return this gene as an internal candidate
                        //candidate internal genes must be subset of children
                        && mutablePhenotypeGenes.size <= children.size
                        && mutablePhenotypeGenes.all { children.contains(it) }
                        //everything returned should be mutable
                        && mutablePhenotypeGenes.all {it.isMutable()}
                        && mutablePhenotypeGenes.all { it.parent == this }
            }

            /*
                In hypermutation, we might mutate more than 1 gene at a time
             */
            val selected = selectSubsetToMutate(mutablePhenotypeGenes, randomness, mwc, childrenToMutateSelectionStrategy, additionalGeneMutationInfo)

            Lazy.assert { selected.isNotEmpty() }

            selected.forEach{
                var mutateCounter = 0
                do {
                    it.first.standardMutation(randomness, apc, mwc, childrenToMutateSelectionStrategy, enableAdaptiveGeneValueMutation, it.second)
                    mutateCounter +=1
                }while (!mutationCheck() && mutateCounter <=3)
                if (!mutationCheck()){
                    LoggingUtil.uniqueWarn(log, "the mutated value for Gene ($name with type ${this::class.java.simpleName}) cannot satisfy its `mutationCheck` after 3 attempts")
                    if (log.isTraceEnabled)
                        log.trace("invoke GeneUtils.repairGenes")
                    GeneUtils.repairGenes(listOf(this))
                }
            }
        }

        //sync binding gene after value mutation
        syncBindingGenesBasedOnThis()
    }


    /**
     * @return a list of internal genes (direct children) that can be selected for mutation.
     * These could then be further selected based on weight-based or adaptive weight-based gene selection
     * when applying hyper-mutation.
     * Note that, if return an empty list, [shallowMutate] will be applied to mutate this gene.
     *
     * The default implementation for "simple" genes would be to return "listOf<Gene>()", ie an empty list.
     * In general, all children that are mutable will be returned, as long as they do have IMPACT on the
     * phenotype of the individual.
     * There is no point in mutating a gene that will not change the fitness.
     *
     * Note that the current gene must never be returned in this method
     *
     * What returned here is a subset (possibly not strict) of children.
     *
     * TODO add tests for invariants (eg, an easy way is to see if, after mutation, the string
     * representation of the test is changed).
     *
     */
    protected open fun mutablePhenotypeChildren(): List<Gene> {
        return children.filter { it.isMutable() }
    }


    /**
     * How we are going to use impact info on selecting the given subset of internal genes
     *
     * @param randomness
     * @param internalGenes is a set of candidates to be selected
     * @param mwc is mutation weight controller which can be used to select genes with given weights
     * @param additionalGeneMutationInfo contains impact info of [this] gene
     * @return a subset of [internalGenes] with corresponding impact info
     */
    //TODO abstract
    protected open fun adaptiveSelectSubsetToMutate(randomness: Randomness,
                                                    internalGenes: List<Gene>,
                                                    mwc: MutationWeightControl,
                                                    additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        throw IllegalStateException("adaptive gene selection is unavailable for the gene ${this::class.java.simpleName}")
    }

    /**
     * @return a subset of internal genes to apply mutations
     */
    private fun selectSubsetToMutate(mutablePhenotypeChildren: List<Gene>,
                                     randomness: Randomness,
                                     mwc: MutationWeightControl,
                                     selectionStrategy: SubsetGeneMutationSelectionStrategy,
                                     additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        return  when(selectionStrategy){
            SubsetGeneMutationSelectionStrategy.DEFAULT -> {
                //No hypermutation... return just 1 gene, at random
                val s = randomness.choose(mutablePhenotypeChildren)
                listOf( s to additionalGeneMutationInfo?.copyFoInnerGene( null,s))
            }
            SubsetGeneMutationSelectionStrategy.DETERMINISTIC_WEIGHT -> {
                mwc.selectSubGene(candidateGenesToMutate = mutablePhenotypeChildren, adaptiveWeight = false)
                    .map { it to additionalGeneMutationInfo?.copyFoInnerGene(null, it) }
            }
            SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT -> {
                additionalGeneMutationInfo?: throw IllegalArgumentException("additionalGeneSelectionInfo should not be null")
                if (additionalGeneMutationInfo.impact == null)
                    //if no impact info, still apply hypermutation
                    selectSubsetToMutate(mutablePhenotypeChildren, randomness, mwc, SubsetGeneMutationSelectionStrategy.DETERMINISTIC_WEIGHT, additionalGeneMutationInfo)
                else
                    adaptiveSelectSubsetToMutate(randomness, mutablePhenotypeChildren, mwc, additionalGeneMutationInfo)
            }
        }.also {
            if (it.isEmpty())
                throw IllegalStateException("with $selectionStrategy strategy and ${mutablePhenotypeChildren.size} candidates, none is selected to mutate")
            if (it.any { a -> a.second?.impact?.validate(a.first) == false})
                throw IllegalStateException("mismatched impact for gene ${it.filter { a -> a.second?.impact?.validate(a.first) == false}.map { "${it.first}:${it.second}" }.joinToString(",")}")
        }
    }



    /**
     * mutate the current gene (and NONE of its children directly, if any) if there is no need to apply selection,
     * i.e., when [mutablePhenotypeChildren] is empty.
     * Note though that this method might add/remove children
     *
     * @return whether the mutation was successful
     */
    protected  open fun shallowMutate(randomness: Randomness,
                                      apc: AdaptiveParameterControl,
                                      mwc: MutationWeightControl,
                                      selectionStrategy: SubsetGeneMutationSelectionStrategy,
                                      enableAdaptiveGeneMutation: Boolean,
                                      additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        return false
    }

    /**
     * Return the value as a printable string.
     * Once printed, it would be equivalent to the actual value, eg
     *
     * 1 -> "1" -> printed as 1
     *
     * "foo" -> "\"foo\"" -> printed as "foo"
     *
     * @param previousGenes previous genes which are necessary to look at
     * to determine the actual value of this gene
     * @param mode some genes could be printed in different ways, like an
     * object printed as JSON or XML
     * @param targetFormat different target formats may have different rules
     * regarding what characters need to be escaped (e.g. the $ char in Kotlin)
     * If the [targetFormat] is set to null, no characters are escaped.
     */
    abstract fun getValueAsPrintableString(
        previousGenes: List<Gene> = listOf(),
        mode: GeneUtils.EscapeMode? = null,
        targetFormat: OutputFormat? = null,
        /**
             * Generic boolean, used for extra info, if needed.
             *
             * This was introduced mainly to deal with the printing of objects in GraphQL.
             * Specify if the name of object should be printed or not, or just directly the
             * object {} definition, ie,
             * foo {...}
             * vs
             * {...}
             */
            extraCheck: Boolean = false
    ) : String


    open fun getValueAsRawString() = getValueAsPrintableString(targetFormat = null)
    /*
    Note: above, null target format means that no characters are escaped.
     */

    /**
     * copy value based on [other]
     * in some case, the [other] might not satisfy constraints of [this gene],
     * then copying will not be performed successfully
     *
     * @return whether the value is copied based on [other] successfully
     */
    abstract fun copyValueFrom(other: Gene): Boolean

    /**
     * If this gene represents a variable, then return its name.
     */
    open fun getVariableName() = name

    /**
     * Genes might have other genes inside (eg, think of array).
     * But these are only the ones in "child-parent" hierarchy.
     * There might be cases like "template" genes inside, those will NOT be returned.
     *
     * @param excludePredicate is used to configure which genes you do not want to show genes inside.
     *      For instance, an excludePredicate is {gene : Gene -> (gene is TimeGene)}, then when flatView of a Gene including TimeGene,
     *      the genes inside e.g., hour: IntegerGene will be not viewed, but TimeGene will be viewed.
     * @return a recursive list of all nested genes, "this" included
     */
    fun flatView(excludePredicate: (Gene) -> Boolean = {false}): List<Gene>{
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(children.flatMap { g -> g.flatView(excludePredicate) })
    }

    /**
     * Get references to ALL genes (and not just top ones) in the individual this gene belongs to.
     * If not mounted in an individual, return an empty list
     */
    fun getAllGenesInIndividual() : List<Gene>{
        return getAllTopGenesInIndividual().flatMap { it.flatView() }
    }

    fun getAllTopGenesInIndividual() : List<Gene>{
        val root = getRoot()
        if(root !is Individual){
            return listOf()
        }

        return root.seeGenes()
    }

    /**
     * Genes might contain a value that is also stored
     * in another gene of the same type.
     */
    abstract fun containsSameValueAs(other: Gene): Boolean




    /**
     * evaluate whether [this] and [gene] belong to one evolution during search
     */
    open fun possiblySame(gene : Gene) : Boolean = gene.name == name && gene::class == this::class


    /**
     * Given a string value, apply it to the current state of this gene (and possibly recursively to its children).
     * If it fails for any reason, return false, without modifying its state.
     */
    open fun setFromStringValue(value: String) : Boolean{
        //TODO in future this should be abstract, to force each gene to handle it.
        //few implementations can be based on AbstractParser class for Postman
        throw IllegalStateException("setFromStringValue() is not implemented for gene ${this::class.simpleName}")
    }


    //========================= handing binding genes ===================================

    //TODO make sure all public methods keep the gene in a valid state.
    //TODO go through all these methods

    /**
     * rebuild the binding relationship of [this] gene based on [copiedGene] which exists in [copiedIndividual]
     */
    fun rebuildBindingWithTemplate(newIndividual: Individual, copiedIndividual: Individual, copiedGene: Gene){
        if (bindingGenes.isNotEmpty())
            throw IllegalArgumentException("gene ($name) has been rebuilt")

        val list = copiedGene.bindingGenes.map { g->
            newIndividual.findGene(copiedIndividual, g)
                ?:throw IllegalArgumentException("cannot find the gene (${g.name}) in the copiedIndividual")
        }

        bindingGenes.addAll(list)
        Lazy.assert { !bindingGenes.contains(this) }
    }

    /**
     * sync [bindingGenes] based on [this]
     */
    fun syncBindingGenesBasedOnThis(all : MutableSet<Gene> = mutableSetOf()){
        if (bindingGenes.isEmpty()) return
        all.add(this)
        bindingGenes.filterNot { all.contains(it) }.forEach { b->
            all.add(b)
            if(!b.bindValueBasedOn(this))
                LoggingUtil.uniqueWarn(log, "fail to bind the gene (${b.name} with the type ${b::class.java.simpleName}) based on this gene (${this.name} with ${this::class.java.simpleName})")
            b.syncBindingGenesBasedOnThis(all)
        }

        children.filterNot { all.contains(it) }.forEach { it.syncBindingGenesBasedOnThis(all) }
    }

    /**
     * get all binding genes of [this], transitively,
     * as well as for any internal gene which is bound
     *
     * TODO this is not necessary if transitive relations have been sync,
     * because could use bindingGenes directly
     */
    private fun computeTransitiveBindingGenes(all : MutableSet<Gene>){
        val root = getRoot()
        val allBindingGene = bindingGenes.plus(
            if (root is Individual) root.seeGenes().flatMap { it.flatView() }.filter {
                r-> r != this && r.bindingGenes.contains(this)
            }
            else emptyList()
        )
        if (allBindingGene.isEmpty()) {
            return
        }
        all.add(this) //adding current

        allBindingGene.filterNot { all.contains(it) }
                //all other genes that are bound to this and are NOT in all
                .forEach { b->
            all.add(b)
            b.computeTransitiveBindingGenes(all)
        }
        /*
            TODO if [this] is bound, can any of its children be bound??? likely not
         */
//        children.filterNot { all.contains(it) }.forEach { it.computeTransitiveBindingGenes(all) }
    }

    /**
     * compute transitive BindingGenes of this gene in an individual
     */
    private fun computeTransitiveBindingGenes(){
        val all = mutableSetOf<Gene>()
        computeTransitiveBindingGenes(all)
        all.remove(this)
        resetBinding(all)
    }

    /**
     * compute transitive BindingGenes of this gene and composed children genes in an individual
     */
    fun computeAllTransitiveBindingGenes(){
        computeTransitiveBindingGenes()
        children.forEach(Gene::computeTransitiveBindingGenes)
    }

    /**
     * remove [this] from its binding genes, and also make
     * sure that any children will lose their bindings
     *
     * For example, if [this] is an Object K with no binding, but
     * with two fields X and Y that are bound, all genes we lose their
     * binding to X and Y
     *
     * TODO possibly rename, see next TODO
     */
    fun removeThisFromItsBindingGenes(){
//        val all = mutableSetOf<Gene>()
//
//        //TODO can we remove a gene that is not in sync? if not, we can look at bindingGenes directly
//        computeTransitiveBindingGenes(all)

        bindingGenes.forEach { b->
            //FIXME this is a bug, removing to K, but not X and Y, isn'it?
            b.removeBindingGene(this)
        }
        //TODO should we do this???
        //bindingGenes.clear()
    }



    /**
     * @return whether [this] gene is bound with any other gene
     */
    fun isBoundGene() = bindingGenes.isNotEmpty()

    /**
     * repair the broken binding reference e.g., the binding gene is removed from the current individual
     */
    fun cleanBrokenReference(all : List<Gene>) : Boolean{
        return bindingGenes.removeIf { !all.contains(it) }
    }

    /**
     * remove genes which has been removed from the root
     */
    fun cleanRemovedGenes(removed: List<Gene>): Boolean{
        return bindingGenes.removeIf{removed.contains(it)}
    }

    /**
     * @return whether [this] gene has same binding gene as [genes]
     *
     * it is useful for debugging/unit tests
     */
    fun isSameBinding(genes: Set<Gene>) = (genes.size == bindingGenes.size) && genes.containsAll(bindingGenes)

    /**
     * add [gene] as the binding gene
     */
    fun addBindingGene(gene: Gene) {
        bindingGenes.add(gene)
        Lazy.assert { !bindingGenes.contains(this) }
    }

    /**
     * remove [gene] as the binding gene
     */
    private fun removeBindingGene(gene: Gene): Boolean {
        return bindingGenes.remove(gene)
    }

    /**
     * @return whether the bindingGene is subset of the [set]
     */
    fun bindingGeneIsSubsetOf(set: List<Gene>) = set.containsAll(bindingGenes)

    /**
     * reset binding based on [genes]
     */
    fun resetBinding(genes: Set<Gene>) {
        bindingGenes.clear()
        bindingGenes.addAll(genes)
        Lazy.assert { !bindingGenes.contains(this) }
    }

    /**
     * clean bindingGenes
     */
    fun cleanBinding(){
        bindingGenes.clear()
    }

    /**
     *  A is bound with C
     *  B is bound with C
     *
     *  then A and B should be considered as Bounded
     *
     *  might Deprecated this later, once the [bindingGenes]
     *  contains all this 2 depth binding genes
     */
    fun is2DepthDirectBoundWith(gene: Gene) : Boolean{
        if (isDirectBoundWith(gene)) return true

        // 2 depth binding check
        return bindingGenes.any { b->
            b.isDirectBoundWith(gene)
        }
    }

    /**
     * @return whether [this] is directly bound with [gene]
     */
    fun isDirectBoundWith(gene: Gene) = bindingGenes.contains(gene)

    /**
     * @return whether [this] parent is bound with [gene]'s parent
     */
    fun isAnyParentBoundWith(gene: Gene) : Boolean{
        if ((this.parent as? Gene) != null && (gene.parent as? Gene) != null){
            val direct = (this.parent as Gene).isDirectBoundWith(gene.parent as Gene)
            if (direct) return true
            return (this.parent as Gene).isDirectBoundWith((gene.parent as Gene))
        }
        return false
    }


    /**
     * bind value of [this] gene based on [gene].
     * The type of genes can be different.
     * @return whether the binding performs successfully
     *
     * TODO what if this lead to isLocallyValid to be false? can we prevent it?
     * or just return false here?
     *
     * FIXME: change name, because it is not modifying binding, and just copy over
     * the values
     *
     */
    abstract fun bindValueBasedOn(gene: Gene) : Boolean


    /**
     * here `valid` means that 1) [updateValue] performs correctly, ie, returns true AND 2) isLocallyValid is true
     *
     * @param updateValue lambda performs update of value of the gene
     * @param undoIfUpdateFails represents whether it needs to undo the value update if [undoIfUpdateFails] returns false
     *
     * @return if the value is updated with [updateValue]
     */
    fun updateValueOnlyIfValid(updateValue: () -> Boolean, undoIfUpdateFails: Boolean) : Boolean{
        val current = copy()
        val ok = updateValue()
        if (!ok && !undoIfUpdateFails) return false

        if (!ok || !isLocallyValid()){
            val success = copyValueFrom(current)
            assert(success)
            return false
        }
        return true

    }
}

