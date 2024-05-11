package org.evomaster.core.search.gene.string

import org.apache.commons.text.StringEscapeUtils
import org.evomaster.client.java.instrumentation.shared.RegexSharedUtils
import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.StaticCounter
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.parser.RegexUtils
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.*
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.interfaces.ComparableGene
import org.evomaster.core.search.gene.interfaces.TaintableGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.uri.UriGene
import org.evomaster.core.search.gene.uri.UrlHttpGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.StringGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import kotlin.math.max
import kotlin.math.min

class StringGene(
        name: String,
        var value: String = "foo",
        /** Inclusive */
        val minLength: Int = 0,
        /**
         * Inclusive.
         * Constraint on maximum lenght of the string. This could had been specified as
         * a constraint in the schema, or specific for the represented data type.
         * Note: further limits could be imposed to avoid too large strings that would
         * hamper the search process, which can be set via [EMConfig] options
         */
        val maxLength: Int = EMConfig.stringLengthHardLimit,
        /**
         * Depending on what a string is representing, there might be some chars
         * we do not want to use.
         * For example, in a URL Path variable, we do not want have "/", as otherwise
         * it would create 2 distinct paths
         */
        val invalidChars: List<Char> = listOf(),

        /**
         * specialization based on taint analysis
         */
        specializationGenes: List<Gene> = listOf()

) : TaintableGene, ComparableGene, CompositeGene(name, specializationGenes.toMutableList()) {

    init {
        if (minLength>maxLength) {
            throw IllegalArgumentException("Cannot create string gene ${this.name} with minimum length ${this.minLength} and maximum length ${this.maxLength}")
        }
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(StringGene::class.java)

        private const val PROB_CHANGE_SPEC = 0.1
    }

    private var validChar: String? = null

    /**
     * Based on taint analysis, in some cases we can determine how some Strings are
     * used in the SUT.
     * For example, if a String is used as a Date, then it make sense to use a specialization
     * in which we mutate to have only Strings that are valid dates
     */
    private val specializations: MutableSet<StringSpecializationInfo> = mutableSetOf()

    var selectedSpecialization = -1

    var selectionUpdatedSinceLastMutation = false
        private set

    /**
     * Check if we already tried to use this string for taint analysis
     */
    var tainted = false


    /**
     * During the search, we might discover (with TaintAnalysis) that 2 different
     * string variables are compared for equality.
     * In those cases, if we want to keep them in sync, each time we mutate one, we
     * need to update the other.
     * This is not trivial, as the strings might be subject to different constraints,
     * and we would need to find their intersection.
     */
    var bindingIds = mutableSetOf<String>()

    //right now, all children are specializations
    val specializationGenes: List<Gene>
        get() {return children}


    private fun actualMaxLength() : Int {

        val state = getSearchGlobalState()
            ?: return maxLength

        return max(minLength, min(maxLength, state.config.maxLengthForStrings))
    }


    override fun checkForGloballyValid() : Boolean{
        return value.length <= actualMaxLength()
    }

    override fun isLocallyValid() : Boolean{
        //note, here we do not check actualMaxLength(), as it imply a global initialization
        return value.length in minLength..maxLength
                && invalidChars.none { value.contains(it) }
                && getViewOfChildren().all { it.isLocallyValid() }
    }

    override fun copyContent(): Gene {
        val copy = StringGene(
            name,
            value,
            minLength,
            maxLength,
            invalidChars,
            this.specializationGenes.map { g -> g.copy() }.toMutableList()
        )
                .also {
                    it.specializations.addAll(this.specializations)
                    it.validChar = this.validChar
                    it.selectedSpecialization = this.selectedSpecialization
                    it.selectionUpdatedSinceLastMutation = this.selectionUpdatedSinceLastMutation
                    it.tainted = this.tainted
                    it.bindingIds = this.bindingIds.map { id -> id }.toMutableSet()
                }
//        copy.specializationGenes.forEach { it.parent = copy }
        return copy
    }

    fun getSpecializationGene(): Gene? {
        if (selectedSpecialization >= 0 && selectedSpecialization < specializationGenes.size) {
            return specializationGenes[selectedSpecialization]
        }
        return null
    }

    override fun isMutable(): Boolean {
        if (getSpecializationGene() != null) {
            return specializationGenes.size > 1 || getSpecializationGene()!!.isMutable()
        }
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        /*
            Even if through mutation we can get large string, we should
            avoid sampling very large strings by default
        */
        val maxForRandomization = getSearchGlobalState()?.config?.maxLengthForStringsAtSamplingTime ?: 16

        val adjustedMin = minLength
        var adjustedMax = min(maxLength, maxForRandomization)

        if(adjustedMax < adjustedMin){
            /*
            this can happen if there are constraints on min length that are longer than our typical strings.
            even if we do not want to use too long strings for performance reasons, we still must satisfy
            any min constrains
            */
            assert(minLength <= maxLength)
            adjustedMax = adjustedMin
        }

        if(adjustedMax == 0 && adjustedMin == adjustedMax){
            //only empty string is allowed
            value = ""
        } else {
            val x = randomness.nextWordString(adjustedMin, adjustedMax)
            if(tryToForceNewValue && value == x){
                //this should be rare, but can happen when max is low
                randomize(randomness, true)
                return
            }
            value = x
        }
        repair()
        selectedSpecialization = -1
        handleBinding(getAllGenesInIndividual())
    }

    override fun applyGlobalUpdates() {
        /*
            TODO this assertion had to be removed, as Resource Sampler uses action templates that have been
            already initialized... but unsure how that would negatively effect the Taint on Sampling done
            here
         */
        //assert(!tainted)

        /*
            the gene might be initialized without global constraint
         */
        if (!checkForGloballyValid())
            repair()

        /*
            it binds with any value, skip to apply the global taint
         */
        if (isBoundGene())
            return

        //check if starting directly with a tainted value
        val state = getSearchGlobalState()!! //cannot be null when this method is called
        if(state.config.taintOnSampling){

            if(state.spa.hasInfoFor(name) && state.randomness.nextDouble() < state.config.useGlobalTaintInfoProbability){
                val spec = state.spa.chooseSpecialization(name, state.randomness)!!
                assert(specializations.size == 0)
                addSpecializations("", listOf(spec),state.randomness, false, enableConstraintHandling = state.config.enableSchemaConstraintHandling)
                assert(specializationGenes.size == 1)
                selectedSpecialization = specializationGenes.lastIndex
            } else {
                redoTaint(state.apc, state.randomness)
            }
        }

        //config might have stricter limits for length
        repair()
    }

    override fun mutablePhenotypeChildren(): List<Gene> {
       /*
            Specializations are children... but here the shallow mutation deal with them.
        */
        return listOf()
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return true
    }


    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl,
                               selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        val allGenes = getAllGenesInIndividual()

        if (enableAdaptiveGeneMutation){
            additionalGeneMutationInfo?:throw IllegalArgumentException("archive-based gene mutation cannot be applied without AdditionalGeneMutationInfo")
            additionalGeneMutationInfo.archiveGeneMutator.mutateStringGene(
                    this, allGenes = allGenes, selectionStrategy = selectionStrategy, targets = additionalGeneMutationInfo.targets, additionalGeneMutationInfo = additionalGeneMutationInfo, changeSpecSetting = PROB_CHANGE_SPEC
            )
            return true
        }

        val didSpecializationMutation = standardSpecializationMutation(
                randomness, apc, mwc, selectionStrategy, allGenes, enableAdaptiveGeneMutation, additionalGeneMutationInfo
        )
        if (!didSpecializationMutation){
            standardValueMutation(
                    randomness, allGenes, apc
            )
        }
        return true
    }

    /**
     * @return whether the specialization is mutated
     */
    fun standardSpecializationMutation(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        allGenes: List<Gene>,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ) : Boolean {
        val specializationGene = getSpecializationGene()

        val taintApplySpecializationProbability = getSearchGlobalState()?.config?.taintApplySpecializationProbability ?: 0.5
        val taintChangeSpecializationProbability = getSearchGlobalState()?.config?.taintChangeSpecializationProbability ?: 0.1

        if (specializationGene == null && specializationGenes.isNotEmpty() && randomness.nextBoolean(taintApplySpecializationProbability)) {
            /*
                Shouldn't be done with 100% probability, because some specializations might be useless
             */
            log.trace("random a specializationGene of String at StandardMutation with string: {}; size: {}; content: {}", name, specializationGenes.size,
                specializationGenes.joinToString(",") { s -> s.getValueAsRawString() })
            selectedSpecialization = randomness.nextInt(0, specializationGenes.size - 1)
            selectionUpdatedSinceLastMutation = false
            handleBinding(allGenes)
            return true

        } else if (specializationGene != null) {
            if (selectionUpdatedSinceLastMutation && randomness.nextBoolean(0.67)) {
                /*
                    selection of most recent added gene, but only with a given
                    probability, albeit high.
                    point is, switching is not always going to be beneficial

                    is this branch possible? to get here, would need a specialization gene that still counts
                    as a tainted value...
                    YES! that applies for Regex, as those are handled specially (with values sent at each Action evaluation)
                 */
                selectedSpecialization = specializationGenes.lastIndex
            } else if (specializationGenes.size > 1 && (!specializationGene.isMutable() ||randomness.nextBoolean(taintChangeSpecializationProbability))) {
                //choose another specialization, but with low probability
                selectedSpecialization = randomness.nextInt(0, specializationGenes.size - 1, selectedSpecialization)
            } else if(!specializationGene.isMutable() || randomness.nextBoolean(taintChangeSpecializationProbability)){
                //not all specializations are useful
                selectedSpecialization = -1
            } else {
                //extract impact of specialization of String
                val impact = if (enableAdaptiveGeneMutation || selectionStrategy != SubsetGeneMutationSelectionStrategy.DEFAULT)
                    (additionalGeneMutationInfo?.impact as? StringGeneImpact)?.hierarchySpecializationImpactInfo?.flattenImpacts()?.get(selectedSpecialization) as? GeneImpact
                else null
                //just mutate current selection
                specializationGene.standardMutation(randomness, apc, mwc, selectionStrategy, enableAdaptiveGeneMutation, additionalGeneMutationInfo?.copyFoInnerGene(impact = impact, gene = specializationGene))
            }
            selectionUpdatedSinceLastMutation = false
            handleBinding(allGenes)
            return true
        }

        if (redoTaint(apc, randomness)) return true

        return false
    }

    fun standardValueMutation(
        randomness: Randomness,
        allGenes: List<Gene>,
        apc: AdaptiveParameterControl
    ){
        if(TaintInputName.isTaintInput(value)){
            //standard mutation on a tainted value makes little sense, so randomize instead
            randomize(randomness, true)
        }

        val p = randomness.nextDouble()
        val s = value

        /*
            What type of mutations we do on Strings is strongly
            correlated on how we define the fitness functions.
            When dealing with equality, as we do left alignment,
            then it makes sense to prefer insertion/deletion at the
            end of the strings, and reward more "change" over delete/add
         */

        val others = allGenes.flatMap { it.flatView() }
                .filterIsInstance<StringGene>()
                .map { it.getValueAsRawString() }
                .filter { it != value }
                .filter { !TaintInputName.isTaintInput(it) }

        value = when {
            //seeding: replace
            p < 0.02 && others.isNotEmpty() -> {
                randomness.choose(others)
            }
            //change
            p < 0.8 && s.isNotEmpty() -> {
                val delta = GeneUtils.getDelta(randomness, apc, start = 6, end = 3)
                val sign = randomness.choose(listOf(-1, +1))
                log.trace("Changing char in: {}", s)
                val i = randomness.nextInt(s.length)
                val array = s.toCharArray()
                array[i] = s[i] + (sign * delta)
                String(array)
            }
            //delete last
            p < 0.9 && s.isNotEmpty() && s.length > minLength -> {
                s.dropLast(1)
            }
            //append new
            s.length < actualMaxLength() -> {
                if (s.isEmpty() || randomness.nextBoolean(0.8)) {
                    s + randomness.nextWordChar()
                } else {
                    log.trace("Appending char")
                    val i = randomness.nextInt(s.length)
                    if (i == 0) {
                        randomness.nextWordChar() + s
                    } else {
                        s.substring(0, i) + randomness.nextWordChar() + s.substring(i, s.length)
                    }
                }
            }
            else -> {
                //do nothing
                s
            }
        }

        repair()
        handleBinding(allGenes)
    }

    fun redoTaint(apc: AdaptiveParameterControl, randomness: Randomness) : Boolean{

        if(!TaintInputName.doesTaintNameSatisfiesLengthConstraints("${StaticCounter.get()}", actualMaxLength())){
            return false
        }

        val minPforTaint = 0.1
        val tp = apc.getBaseTaintAnalysisProbability(minPforTaint)

        if (!apc.doesFocusSearch()
            && (
                    (!tainted && randomness.nextBoolean(tp))
                    ||
                                /*
                                    if this has already be tainted, but that lead to no specialization,
                                    we do not want to reset with a new taint value, and so skipping all
                                    standard mutation on strings.
                                    but we might want to use a taint value at a later stage, in case its
                                    specialization depends on code paths executed depending on other inputs
                                    in the test case
                                 */
                    (tainted && randomness.nextBoolean(Math.max(tp/2, minPforTaint)))
                )
        ) {
            forceTaintedValue()
            return true
        }

        val taintRemoveProbability = getSearchGlobalState()?.config?.taintRemoveProbability ?: 0.5

        if (tainted && randomness.nextBoolean(taintRemoveProbability) && TaintInputName.isTaintInput(value)) {
            randomize(randomness, true)
            return true
        }

        return false
    }

    /**
     * Force a tainted value. Must guarantee min-max length constraints are satisfied
     */
    fun forceTaintedValue() {
        val taint = TaintInputName.getTaintName(StaticCounter.getAndIncrease(), minLength)

        if(taint.length !in minLength..maxLength){
            throw IllegalStateException("Tainted value out of min-max range [$minLength,$maxLength]")
        }
        value = taint
        tainted = true
    }

    /**
     * This should be called after each mutation, to check if any other genes must be updated after
     * this one has been mutated
     */
    fun handleBinding(allGenes: List<Gene>){

        if(bindingIds.isEmpty()){
            return
        }

        val others = allGenes.filterIsInstance<StringGene>()
                .filter { it != this }
                .filter{ k ->  this.bindingIds.any { k.bindingIds.contains(it) }}

        if(others.isEmpty()){
            /*
                this could happen if the structure mutator did remove the actions
                containing these other genes
             */
            return
        }

        /*
            TODO doing this "properly" will be a lot of work... for now, we keep it simple,
            and remove the specialization in the others
         */
        val update = getValueAsRawString()
        for (k in others){
            setValueWithBindingId(update)
        }
    }

    private fun setValueWithBindingId(update: String){
        val curSelected = selectedSpecialization
        val curValue =value

        selectedSpecialization = -1
        value = update

        if (!isLocallyValid()){
            selectedSpecialization = curSelected
            value = curValue
        }

    }

    fun addSpecializations(
        /**
         * TODO what whas this? does not seem to be used
         */
        key: String,
        specs: Collection<StringSpecializationInfo>,
        randomness: Randomness,
        updateGlobalInfo: Boolean = true,
        enableConstraintHandling: Boolean
    ) {

        val toAddSpecs = specs
                //don't add the same specialization twice
                .filter { !specializations.contains(it) }
                /*
                        a StringGene might have some characters that are not allowed,
                        like '/' and '.' in a PathParam.
                        If we have a constant that uses any of such chars, then we must
                        skip it.
                        Also must guarantee min/max constraints.

                        TODO will need to guarantee min/max constraints on Regex as well
                 */
                .filter { s ->
                    s.stringSpecialization != StringSpecialization.CONSTANT ||
                            (invalidChars.none { c -> s.value.contains(c) }
                                    && s.value.length >= minLength && s.value.length <= maxLength)
                }

        val toAddGenes = mutableListOf<Gene>()

        //all constant values are merged in the same enum gene
        if (toAddSpecs.any { it.stringSpecialization == StringSpecialization.CONSTANT }) {
            /*
                TODO partial matches
             */
            toAddGenes.add(
                EnumGene<String>(
                    name,
                    toAddSpecs.filter { it.stringSpecialization == StringSpecialization.CONSTANT }.map { it.value })
            )
            log.trace("CONSTANT, added specification size: {}", toAddGenes.size)
        }

        if (toAddSpecs.any { it.stringSpecialization == StringSpecialization.CONSTANT_IGNORE_CASE }) {
            toAddGenes.add(RegexHandler.createGeneForJVM(
                toAddSpecs.filter { it.stringSpecialization == StringSpecialization.CONSTANT_IGNORE_CASE }
                    .map { "^(${RegexUtils.ignoreCaseRegex(it.value)})$" }
                    .joinToString("|")
            ))
            log.trace("CONSTANT_IGNORE_CASE, added specification size: {}", toAddGenes.size)
        }


        if (toAddSpecs.any { it.stringSpecialization == StringSpecialization.DATE_YYYY_MM_DD }) {
            toAddGenes.add(DateGene(name))
            log.trace("DATE_YYYY_MM_DD, added specification size: {}", toAddGenes.size)
        }

        if (toAddSpecs.any { it.stringSpecialization == StringSpecialization.BOOLEAN }) {
            toAddGenes.add(BooleanGene(name))
            log.trace("BOOLEAN, added specification size: {}", toAddGenes.size)
        }

        if (toAddSpecs.any { it.stringSpecialization == StringSpecialization.INTEGER }) {
            toAddGenes.add(IntegerGene(name))
            log.trace("INTEGER, added specification size: {}", toAddGenes.size)

        }

        if (toAddSpecs.any { it.stringSpecialization == StringSpecialization.LONG }) {
            toAddGenes.add(LongGene(name))
            log.trace("LONG, added specification size: {}", toAddGenes.size)
        }

        if (toAddSpecs.any { it.stringSpecialization == StringSpecialization.FLOAT }) {
            toAddGenes.add(FloatGene(name))
            log.trace("FLOAT, added specification size: {}", toAddGenes.size)
        }

        if (toAddSpecs.any { it.stringSpecialization == StringSpecialization.DOUBLE }) {
            toAddGenes.add(DoubleGene(name))
            log.trace("DOUBLE, added specification size: {}", toAddGenes.size)
        }

        if(toAddSpecs.any { it.stringSpecialization == StringSpecialization.UUID }){
            toAddGenes.add(UUIDGene(name))
            log.trace("UUID, added specification size: {}", toAddGenes.size)
        }

        if(toAddSpecs.any { it.stringSpecialization == StringSpecialization.URL }){
            toAddGenes.add(UrlHttpGene(name))
            log.trace("URL, added specification size: {}", toAddGenes.size)
        }

        if(toAddSpecs.any { it.stringSpecialization == StringSpecialization.URI }){
            toAddGenes.add(UriGene(name))
            log.trace("URI, added specification size: {}", toAddGenes.size)
        }

        if(toAddSpecs.any { it.stringSpecialization == StringSpecialization.JSON_OBJECT }){
            toAddSpecs.filter { it.stringSpecialization == StringSpecialization.JSON_OBJECT }
                    .forEach {
                        val schema = it.value
                        val t = schema.subSequence(0, schema.indexOf(":")).trim().toString()
                        val ref = t.subSequence(1,t.length-1).toString()
                        val obj = RestActionBuilderV3.createGeneForDTO(ref, schema, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling))
                        toAddGenes.add(obj)
                    }
            log.trace("JSON_OBJECT, added specification size: {}", toAddGenes.size)
        }

        if(toAddSpecs.any { it.stringSpecialization == StringSpecialization.JSON_ARRAY }){
            toAddGenes.add(TaintedArrayGene(name,TaintInputName.getTaintName(StaticCounter.getAndIncrease())))
            log.trace("JSON_ARRAY, added specification size: {}", toAddGenes.size)
        }

        if(toAddSpecs.any { it.stringSpecialization == StringSpecialization.JSON_MAP }){
            val mapGene = FixedMapGene("template", StringGene("keyTemplate"), StringGene("valueTemplate"))
            /*
                for Map, we currently only handle them as string key with string value
                TODO handle generic type if they have

                set tainted input for key of template if the key is string type
             */
            mapGene.template.first.forceTaintedValue()

            toAddGenes.add(mapGene)

            log.trace("JSON_MAP, added specification size: {}", toAddGenes.size)
        }

        handleRegex(key, toAddSpecs, toAddGenes)

        /*
            TODO
            here we could check if merging with existing genes.
            - CONSTANT would be relative easy, as just creating a new enum with the union of all constants
            - REGEX would be tricky, because rather than a disjunction, it would likely need to be an AND,
              which could be achieved with "(?=)". But that is something we do not support yet in the
              grammar, and likely it is VERY complicated to do... eg, see:
              https://stackoverflow.com/questions/24102484/can-regex-match-intersection-between-two-regular-expressions
         */

        if (toAddGenes.size > 0) {
            selectionUpdatedSinceLastMutation = true
            toAddGenes.forEach {
                //it.randomize(randomness, false, listOf())
                it.doInitialize(randomness)
            }
            log.trace("in total added specification size: {}", toAddGenes.size)
            addChildren(toAddGenes)

            specializations.addAll(toAddSpecs)
        }

        if (toAddSpecs.any { it.stringSpecialization == StringSpecialization.EQUAL }) {
            /*
                this treated specially. we do not create a new string specialization, but
                rather update bindingIds
             */
            val ids = toAddSpecs.filter { it.stringSpecialization == StringSpecialization.EQUAL }.map { it.value }
            bindingIds.addAll(ids)
        }

        if(updateGlobalInfo) {
            val state = getSearchGlobalState()!! //cannot be null when this method is called
            state.spa.updateStats(name, toAddSpecs)
        }
    }

    private fun handleRegex(key: String, toAddSpecs: List<StringSpecializationInfo>, toAddGenes: MutableList<Gene>) {

        val fullPredicate = { s: StringSpecializationInfo -> s.stringSpecialization.isRegex && s.type.isFullMatch }
        val partialPredicate = { s: StringSpecializationInfo -> s.stringSpecialization.isRegex && s.type.isPartialMatch }

        if (toAddSpecs.any(fullPredicate)) {

            /*
                originally, all regex with combined with disjunction in a single gene...
                but likely was not a good idea...
             */

            //val regex =
                  toAddSpecs
                    .filter(fullPredicate)
                    .filter{RegexUtils.isMeaningfulRegex(it.value)}
                    .filter{RegexUtils.isNotUselessRegex(it.value) }
                    .map {
                        if(it.stringSpecialization == StringSpecialization.REGEX_WHOLE) {
                            RegexSharedUtils.forceFullMatch(it.value)
                        } else {
                            RegexSharedUtils.handlePartialMatch(it.value)
                        }
                    }
                    //.joinToString("|")
                    .forEach {regex ->
                      try {
                              toAddGenes.add(RegexHandler.createGeneForJVM(regex))
                              log.trace("Regex, added specification for: {}", regex)

                          } catch (e: Exception) {
                              LoggingUtil.uniqueWarn(log, "Failed to handle regex: $regex")
                          }
                      }

//            try {
//                toAddGenes.add(RegexHandler.createGeneForJVM(regex))
//                log.trace("Regex, added specification size: {}", toAddGenes.size)
//
//            } catch (e: Exception) {
//                LoggingUtil.uniqueWarn(log, "Failed to handle regex: $regex")
//            }
        }

/*
    Handling a partial match on a single gene is quite complicated to implement, plus
    it might not be so useful.
    TODO something to investigate in the future if we end up with some of these cases
 */
//        if(toAddSpecs.any(partialPredicate)){
//            val regex = toAddSpecs
//                    .filter(partialPredicate)
//                    .map { RegexUtils.extractPartialRegex(key, this.getValueAsRawString(), it.value) }
//                    .joinToString("|")
//            toAddGenes.add(RegexHandler.createGeneForJVM(regex))
//        }
    }


    /**
     * Make sure no invalid chars is used
     */
    override fun repair() {
        repairInvalidChars()

        if(value.length > actualMaxLength()){
            value = value.substring(0, actualMaxLength())
        } else if(value.length < minLength){
            value += "_".repeat(minLength - value.length)
        }

        Lazy.assert { isLocallyValid() }
    }

    private fun repairInvalidChars() {
        if (invalidChars.isEmpty()) {
            //nothing to do
            return
        }

        if (validChar == null) {
            //compute a valid char
            for (c in 'a'..'z') {
                if (!invalidChars.contains(c)) {
                    validChar = c.toString()
                    break
                }
            }
        }
        if (validChar == null) {
            //no basic char is valid??? TODO should handle this situation, although likely never happens
            return
        }

        for (invalid in invalidChars) {
            value = value.replace("$invalid", validChar!!)
        }
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        val specializationGene = getSpecializationGene()

        val rawValue = getValueAsRawString()

        if (specializationGene != null) {
            //FIXME: really escaping is a total mess... need major refactoring
            // TODO: Don't we need to escape the raw string?
            return "\"" + rawValue + "\""
//            return specializationGene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
//                    .replace("\"", "\\\"")
        }

        return if (mode != null && mode == GeneUtils.EscapeMode.XML) {
            StringEscapeUtils.escapeXml10(rawValue)
        } else {
            when {
                (mode == GeneUtils.EscapeMode.GQL_INPUT_MODE)-> "\"${rawValue.replace("\\", "\\\\\\\\")}\""
                // TODO this code should be refactored with other getValueAsPrintableString() methods
                // JP: It makes no sense to me if we enclose with quotes, we need to escape them
                (mode == GeneUtils.EscapeMode.EJSON) -> "\"${rawValue.replace("\\","\\\\").replace("\"", "\\\"")}\""
                (targetFormat == null) -> "\"${rawValue.replace("\"", "\\\"")}\""
                //"\"${rawValue.replace("\"", "\\\"")}\""
                (mode != null) -> "\"${GeneUtils.applyEscapes(rawValue, mode, targetFormat)}\""
                else -> "\"${GeneUtils.applyEscapes(rawValue, GeneUtils.EscapeMode.TEXT, targetFormat)}\""
            }

        }
    }

    override fun getValueAsRawString(): String {
        val specializationGene = getSpecializationGene()

        if (specializationGene != null) {
            return specializationGene.getValueAsRawString()
        }
        return value
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        val current = this.value
        this.value = other.value

        if (!isLocallyValid()){
            this.value = current
            return false
        }
        this.selectedSpecialization = other.selectedSpecialization

        this.specializations.clear()
        this.specializations.addAll(other.specializations)

        killAllChildren()
        addChildren(other.specializationGenes.map { it.copy() })

        this.tainted = other.tainted

        this.bindingIds.clear()
        this.bindingIds.addAll(other.bindingIds)

        return true
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        val tg = this.getSpecializationGene()
        val og = other.getSpecializationGene()

        if ((tg == null && og != null) ||
                (tg != null && og == null)) {
            return false
        }


        if (tg != null) {
            // tg and og might be different types of gene
            if (tg::class.java != og!!::class.java)
                return false
            return tg.containsSameValueAs(og)
        }

        return this.value == other.value
    }


    override fun mutationWeight(): Double {
        return if(specializationGenes.isEmpty()) 1.0 else (specializationGenes.map { it.mutationWeight() }.sum() * PROB_CHANGE_SPEC + 1.0)
    }

    /*
     * TODO
     * string mutation is complex including
     *  -- (taint analysis) on [specializationGenes] whether to employ the specification if exists
     *  -- (taint analysis) on [specializationGenes] whether to change the specification
     *  -- (taint analysis) on [bindingIds] whether to bind values
     *  -- modification on length of [value]
     *  -- modification on char of [value]
     *
     *  when applying archive-based gene mutation, we employ history-based strategy to derive the further mutation that might improve the fitness.
     *  currently the mutation is on [value].
     *  regarding the mutation on [specializationGenes] and [bindingIds], they are handled by archive-based gene selection based on impacts, ie,
     *      -- whether to apply the specialization
     *      -- whether to change the specialization, and the change is based on impacts
     *      -- whether to apply the binding
     *  since the [innerGene] is used to identify a history of gene for archive-based gene mutation,
     *  [specializationGenes] is not considered as part of its inner genes.
     */



    override fun bindValueBasedOn(gene: Gene): Boolean {

        Lazy.assert { isLocallyValid() }
        val current = value

        when(gene){
            //shall I add the specification into the string if it applies?
            is StringGene -> value = gene.value
            is Base64StringGene -> value = gene.data.value
            is FloatGene -> value = gene.value.toString()
            is IntegerGene -> value = gene.value.toString()
            is LongGene -> value = gene.value.toString()
            is DoubleGene -> {
                value = gene.value.toString()
            }
            is ImmutableDataHolderGene -> value = gene.value
            is SqlPrimaryKeyGene ->{
                value = gene.uniqueId.toString()
            }
            // might check toEngineeringString() and toPlainString()
            is BigDecimalGene -> value = gene.value.toString()
            is BigIntegerGene -> value = gene.value.toString()
            is SeededGene<*> ->{
                return this.bindValueBasedOn(gene.getPhenotype() as Gene)
            }
            is NumericStringGene ->{
                return this.bindValueBasedOn(gene.number)
            }
            else -> {
                //return false
                //Man: with taint analysis, g might be any other type.
                if (gene is SqlForeignKeyGene){
                    LoggingUtil.uniqueWarn(
                        log,
                        "attempt to bind $name with a SqlForeignKeyGene ${gene.name} whose target table is ${gene.targetTable}"
                    )
                    value = "${gene.uniqueIdOfPrimaryKey}"
                } else{
                    value = gene.getValueAsRawString()
                }
            }
        }

        if(!isLocallyValid()){
            //this actually can happen when binding to Long, and goes above lenght limit of String
            value = current
            //TODO should we rather enforce this to never happen?
            return false
        }

        return true
    }

    // need to check with Andrea if there is any further impact
    override fun compareTo(other: ComparableGene): Int {
        if (other !is StringGene) {
            throw ClassCastException("Expected StringGene instance but ${other::javaClass} was found")
        }
        return getValueAsRawString().compareTo(other.getValueAsRawString())
    }

    override fun getPossiblyTaintedValue(): String {
        return getValueAsRawString()
    }

    /**
     * if its parent is ArrayGene, it cannot have the same taint input value with any other elements in this ArrayGene
     */
    override fun mutationCheck(): Boolean {
        val arrayGeneParent = getFirstParent { it is ArrayGene<*> } ?: return true
        if (arrayGeneParent.getViewOfChildren().size == 1) return true

        val otherelements  = arrayGeneParent.getViewOfChildren().filter { it is Gene && !it.flatView().contains(this) }
        return otherelements.none { it is Gene && it.flatView().any { g-> g is StringGene && g.getPossiblyTaintedValue().equals(getPossiblyTaintedValue(), ignoreCase = true) } }
    }


    override fun setFromStringValue(value: String): Boolean {

        val previousSpecialization = selectedSpecialization
        val previousValue = value

        this.value = value
        selectedSpecialization = -1
        if(!isLocallyValid() || !checkForGloballyValid()){
            this.value = previousValue
            this.selectedSpecialization = previousSpecialization
            return false
        }

        return true
    }
}
