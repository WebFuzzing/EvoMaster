package org.evomaster.core.search.gene

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.client.java.instrumentation.shared.StringSpecialization.*
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.StaticCounter
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.parser.RegexUtils
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.GeneUtils.EscapeMode
import org.evomaster.core.search.gene.GeneUtils.getDelta
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.StringGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy


class StringGene(
        name: String,
        var value: String = "foo",
        /** Inclusive */
        val minLength: Int = 0,
        /** Inclusive */
        val maxLength: Int = 16,
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
        val specializationGenes: MutableList<Gene> = mutableListOf()

) : Gene(name, specializationGenes) {

    companion object {

        private val log: Logger = LoggerFactory.getLogger(StringGene::class.java)

        private const val PROB_CHANGE_SPEC = 0.1

        /**
         * These are regex with no value, as they match everything.
         * Note: we could have something more sophisticated, to check for any possible meaningless one.
         * But this simple list should do for most cases.
         *
         * TODO: this is not really true, as by default . does not match line breakers like \n
         * So, although they are not important, they are technically not "meaningless"
         */
        private val meaninglesRegex = setOf(".*","(.*)","^(.*)","(.*)$","^(.*)$","^((.*))","((.*))$","^((.*))$")
    }

    /*
        Even if through mutation we can get large string, we should
        avoid sampling very large strings by default
     */
    private val maxForRandomization = 16

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

    override fun getChildren(): List<Gene> = specializationGenes

    override fun copyContent(): Gene {
        val copy = StringGene(name, value, minLength, maxLength, invalidChars, this.specializationGenes.map { g -> g.copyContent() }.toMutableList())
                .also {
                    it.specializations.addAll(this.specializations)
                    it.validChar = this.validChar
                    it.selectedSpecialization = this.selectedSpecialization
                    it.selectionUpdatedSinceLastMutation = this.selectionUpdatedSinceLastMutation
                    it.tainted = this.tainted
                    it.bindingIds = this.bindingIds.map { id -> id }.toMutableSet()
                }
//        copy.specializationGenes.forEach { it.parent = copy }
        copy.addChildren(copy.specializationGenes)
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

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        /*
            TODO weirdly we did not do taint on sampling!!! we must do it, and evaluate it

        if(!tainted){
            redoTaint()
        }
         */

        value = randomness.nextWordString(minLength, Math.min(maxLength, maxForRandomization))
        repair()
        selectedSpecialization = -1
        handleBinding(allGenes)
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>,
                        selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{
        if (enableAdaptiveGeneMutation){
            additionalGeneMutationInfo?:throw IllegalArgumentException("archive-based gene mutation cannot be applied without AdditionalGeneMutationInfo")
            additionalGeneMutationInfo.archiveGeneMutator.mutateStringGene(
                    this, allGenes = allGenes, selectionStrategy = selectionStrategy, targets = additionalGeneMutationInfo.targets, additionalGeneMutationInfo = additionalGeneMutationInfo
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
            selectionStrategy: SubsetGeneSelectionStrategy,
            allGenes: List<Gene>,
            enableAdaptiveGeneMutation: Boolean,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ) : Boolean {
        val specializationGene = getSpecializationGene()

        if (specializationGene == null && specializationGenes.isNotEmpty() && randomness.nextBoolean(0.5)) {
            log.trace("random a specializationGene of String at StandardMutation with string: {}; size: {}; content: {}", name, specializationGenes.size,
                specializationGenes.joinToString(",") { s -> s.getValueAsRawString() })
            selectedSpecialization = randomness.nextInt(0, specializationGenes.size - 1)
            selectionUpdatedSinceLastMutation = false
            handleBinding(allGenes)
            return true

        } else if (specializationGene != null) {
            if (selectionUpdatedSinceLastMutation && randomness.nextBoolean(0.5)) {
                /*
                    selection of most recent added gene, but only with a given
                    probability, albeit high.
                    point is, switching is not always going to be beneficial
                 */
                selectedSpecialization = specializationGenes.lastIndex
            } else if (specializationGenes.size > 1 && randomness.nextBoolean(PROB_CHANGE_SPEC)) {
                //choose another specialization, but with low probability
                selectedSpecialization = randomness.nextInt(0, specializationGenes.size - 1, selectedSpecialization)
            } else if(randomness.nextBoolean(PROB_CHANGE_SPEC)){
                //not all specializations are useful
                selectedSpecialization = -1
            } else {
                //extract impact of specialization of String
                val impact = if (enableAdaptiveGeneMutation || selectionStrategy != SubsetGeneSelectionStrategy.DEFAULT)
                    (additionalGeneMutationInfo?.impact as? StringGeneImpact)?.hierarchySpecializationImpactInfo?.flattenImpacts()?.get(selectedSpecialization) as? GeneImpact
                else null
                //just mutate current selection
                specializationGene.standardMutation(randomness, apc, mwc, allGenes, selectionStrategy, enableAdaptiveGeneMutation, additionalGeneMutationInfo?.copyFoInnerGene(impact = impact, gene = specializationGene))
            }
            selectionUpdatedSinceLastMutation = false
            handleBinding(allGenes)
            return true
        }

        if (redoTaint(apc, randomness, allGenes)) return true

        return false
    }

    fun standardValueMutation(
            randomness: Randomness,
            allGenes: List<Gene>,
            apc: AdaptiveParameterControl
    ){
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
            p < 0.02 && !others.isEmpty() -> {
                randomness.choose(others)
            }
            //change
            p < 0.8 && s.isNotEmpty() -> {
                val delta = getDelta(randomness, apc, start = 6, end = 3)
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
            s.length < maxLength -> {
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

    fun redoTaint(apc: AdaptiveParameterControl, randomness: Randomness, allGenes: List<Gene>) : Boolean{
        val minPforTaint = 0.1
        val tp = apc.getBaseTaintAnalysisProbability(minPforTaint)

        if (
                !apc.doesFocusSearch() &&
                (
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
            value = TaintInputName.getTaintName(StaticCounter.getAndIncrease())
            tainted = true
            return true
        }

        if (tainted && randomness.nextBoolean(0.5) && TaintInputName.isTaintInput(value)) {
            randomize(randomness, true, allGenes)
            return true
        }

        return false
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
            k.selectedSpecialization = -1
            k.value = update
        }
    }

    fun addSpecializations(key: String, specs: Collection<StringSpecializationInfo>, randomness: Randomness) {

        val toAddSpecs = specs
                //don't add the same specialization twice
                .filter { !specializations.contains(it) }
                /*
                        a StringGene might have some characters that are not allowed,
                        like '/' and '.' in a PathParam.
                        If we have a constant that uses any of such chars, then we must
                        skip it.
                        We allow constant larger than Max (as that should not be a problem),
                        but not smaller than Min (eg to avoid empty strings in PathParam)
                 */
                .filter { s ->
                    s.stringSpecialization != CONSTANT ||
                            (invalidChars.none { c -> s.value.contains(c) } && s.value.length >= minLength)
                }

        val toAddGenes = mutableListOf<Gene>()

        //all constant values are merged in the same enum gene
        if (toAddSpecs.any { it.stringSpecialization == CONSTANT }) {
            /*
                TODO partial matches
             */
            toAddGenes.add(
                    EnumGene<String>(
                            name,
                            toAddSpecs.filter { it.stringSpecialization == CONSTANT }.map { it.value }))
            log.trace("CONSTANT, added specification size: {}", toAddGenes.size)
        }

        if (toAddSpecs.any { it.stringSpecialization == CONSTANT_IGNORE_CASE }) {
            toAddGenes.add(RegexHandler.createGeneForJVM(
                    toAddSpecs.filter { it.stringSpecialization == CONSTANT_IGNORE_CASE }
                            .map { "^(${RegexUtils.ignoreCaseRegex(it.value)})$" }
                            .joinToString("|")
            ))
            log.trace("CONSTANT_IGNORE_CASE, added specification size: {}", toAddGenes.size)
        }


        if (toAddSpecs.any { it.stringSpecialization == DATE_YYYY_MM_DD }) {
            toAddGenes.add(DateGene(name))
            log.trace("DATE_YYYY_MM_DD, added specification size: {}", toAddGenes.size)
        }

        if (toAddSpecs.any { it.stringSpecialization == BOOLEAN }) {
            toAddGenes.add(BooleanGene(name))
            log.trace("BOOLEAN, added specification size: {}", toAddGenes.size)
        }

        if (toAddSpecs.any { it.stringSpecialization == INTEGER }) {
            toAddGenes.add(IntegerGene(name))
            log.trace("INTEGER, added specification size: {}", toAddGenes.size)

        }

        if (toAddSpecs.any { it.stringSpecialization == LONG }) {
            toAddGenes.add(LongGene(name))
            log.trace("LONG, added specification size: {}", toAddGenes.size)
        }

        if (toAddSpecs.any { it.stringSpecialization == FLOAT }) {
            toAddGenes.add(FloatGene(name))
            log.trace("FLOAT, added specification size: {}", toAddGenes.size)
        }

        if (toAddSpecs.any { it.stringSpecialization == DOUBLE }) {
            toAddGenes.add(DoubleGene(name))
            log.trace("DOUBLE, added specification size: {}", toAddGenes.size)
        }

        //all regex are combined with disjunction in a single gene
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
                it.randomize(randomness, false, listOf())
//                it.parent = this
            }
            log.trace("in total added specification size: {}", toAddGenes.size)
            specializationGenes.addAll(toAddGenes)
            addChildren(toAddGenes)

            specializations.addAll(toAddSpecs)
        }

        if (toAddSpecs.any { it.stringSpecialization == EQUAL }) {
            /*
                this treated specially. we do not create a new string specialization, but
                rather update bindingIds
             */
            val ids = toAddSpecs.filter { it.stringSpecialization == EQUAL }.map { it.value }
            bindingIds.addAll(ids)
        }
    }

    private fun handleRegex(key: String, toAddSpecs: List<StringSpecializationInfo>, toAddGenes: MutableList<Gene>) {

        val fullPredicate = { s: StringSpecializationInfo -> s.stringSpecialization == REGEX && s.type.isFullMatch }
        val partialPredicate = { s: StringSpecializationInfo -> s.stringSpecialization == REGEX && s.type.isPartialMatch }

        if (toAddSpecs.any(fullPredicate)) {
            val regex = toAddSpecs
                    .filter(fullPredicate)
                    .filter{isMeaningfulRegex(it.value)}
                    .map { it.value }
                    .joinToString("|")

            try {
                toAddGenes.add(RegexHandler.createGeneForJVM(regex))
                log.trace("Regex, added specification size: {}", toAddGenes.size)

            } catch (e: Exception) {
                LoggingUtil.uniqueWarn(log, "Failed to handle regex: $regex")
            }
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

    private fun isMeaningfulRegex(regex: String):  Boolean {

        return ! meaninglesRegex.contains(regex)
    }


    /**
     * Make sure no invalid chars is used
     */
    override fun repair() {
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

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        val specializationGene = getSpecializationGene()

        if (specializationGene != null) {
            return "\"" + specializationGene.getValueAsRawString() + "\""
        }

        val rawValue = getValueAsRawString()
        if (mode != null && mode == EscapeMode.XML) {
            return StringEscapeUtils.escapeXml(rawValue)
        } else {
            when {
                // TODO this code should be refactored with other getValueAsPrintableString() methods
                (targetFormat == null) -> return "\"${rawValue}\""
                //"\"${rawValue.replace("\"", "\\\"")}\""
                (mode != null) -> return "\"${GeneUtils.applyEscapes(rawValue, mode, targetFormat)}\""
                else -> return "\"${GeneUtils.applyEscapes(rawValue, EscapeMode.TEXT, targetFormat)}\""
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

    override fun copyValueFrom(other: Gene) {
        if (other !is StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
        this.selectedSpecialization = other.selectedSpecialization

        this.specializations.clear()
        this.specializations.addAll(other.specializations)

        this.specializationGenes.clear()
        this.specializationGenes.addAll(other.specializationGenes.map { it.copy() })
        addChildren(this.specializationGenes)

        this.tainted = other.tainted

        this.bindingIds.clear()
        this.bindingIds.addAll(other.bindingIds)
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

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(specializationGenes.flatMap { it.flatView(excludePredicate) })
    }

    override fun mutationWeight(): Double {
        return if(specializationGenes.isEmpty()) 1.0 else (specializationGenes.map { it.mutationWeight() }.sum() * PROB_CHANGE_SPEC + 1.0)
    }

    /**
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
    override fun innerGene(): List<Gene> = listOf()


    override fun bindValueBasedOn(gene: Gene): Boolean {
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
            else -> {
                //return false
                //Man: with taint analysis, g might be any other type.
                if (gene is SqlForeignKeyGene){
                    LoggingUtil.uniqueWarn(log, "attempt to bind $name with a SqlForeignKeyGene ${gene.name} whose target table is ${gene.targetTable}")
                    value = "${gene.uniqueIdOfPrimaryKey}"
                } else{
                    value = gene.getValueAsRawString()
                }
            }
        }
        return true
    }

}