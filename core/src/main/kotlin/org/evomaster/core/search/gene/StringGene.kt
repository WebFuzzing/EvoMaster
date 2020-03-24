package org.evomaster.core.search.gene

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.client.java.instrumentation.shared.StringSpecialization.*
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.StaticCounter
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.parser.RegexUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.GeneUtils.getDelta
import org.evomaster.core.search.impact.*
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate
import org.evomaster.core.search.gene.GeneUtils.EscapeMode


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
         * collect info of mutation on its chars of [value]
         */
        val charsMutation : MutableList<IntMutationUpdate> = mutableListOf(),
        /**
         * collect info of mutation on its length of [value]
         */
        val lengthMutation : IntMutationUpdate = IntMutationUpdate(minLength, maxLength),
        /**
         * collect info regarding whether [this] gene is related to others
         */
        val dependencyInfo :GeneIndependenceInfo = GeneIndependenceInfo(degreeOfIndependence = ArchiveMutator.WITHIN_NORMAL)

) : Gene(name) {

    companion object {

        private val log: Logger = LoggerFactory.getLogger(StringGene::class.java)

        private const val NEVER_ARCHIVE_MUTATION = -2
        private const val CHAR_MUTATION_INITIALIZED = -1
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

    var specializationGenes: MutableList<Gene> = mutableListOf()

    var selectedSpecialization = -1

    var selectionUpdatedSinceLastMutation = false


    /**
     * when [mutatedIndex] = -2, it means that chars of [this] have not be mutated yet
     * when [mutatedIndex] = -1, it means that charsMutation of [this] is initialized
     */
    var mutatedIndex : Int = NEVER_ARCHIVE_MUTATION

    /**
     * degree of dependency of this [gene]
     */
//    var degreeOfIndependence = ArchiveMutator.WITHIN_NORMAL
//    private set
//
//    var mutatedtimes = 0
//    private set
//
//    var resetTimes = 0
//    private set

    fun charMutationInitialized(){
        mutatedIndex = CHAR_MUTATION_INITIALIZED
    }

    override fun copy(): Gene {
        val copy = StringGene(name, value, minLength, maxLength, invalidChars, charsMutation.map { it.copy() }.toMutableList(), lengthMutation.copy(), dependencyInfo.copy())
                .also {
                    it.specializationGenes = this.specializationGenes.map { g -> g.copy() }.toMutableList()
                    it.specializations.addAll(this.specializations)
                    it.validChar = this.validChar
                    it.selectedSpecialization = this.selectedSpecialization
                    it.selectionUpdatedSinceLastMutation = this.selectionUpdatedSinceLastMutation
                    it.mutatedIndex = this.mutatedIndex
                }
        copy.specializationGenes.forEach { it.parent = copy }
        return copy
    }

    fun getSpecializationGene() : Gene?{
        if(selectedSpecialization >= 0 && selectedSpecialization < specializationGenes.size){
            return specializationGenes[selectedSpecialization]
        }
        return null
    }

    override fun isMutable() : Boolean{
        if(getSpecializationGene() != null){
            return specializationGenes.size > 1 || getSpecializationGene()!!.isMutable()
        }
        return true
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        value = randomness.nextWordString(minLength, Math.min(maxLength, maxForRandomization))
        repair()
        selectedSpecialization = -1
    }


    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        val specializationGene = getSpecializationGene()

        if (specializationGene == null && specializationGenes.isNotEmpty()) {
            selectedSpecialization = randomness.nextInt(0, specializationGenes.size-1)
            selectionUpdatedSinceLastMutation = false
            return

        } else if (specializationGene != null) {
            if(selectionUpdatedSinceLastMutation && randomness.nextBoolean(0.5)){
                /*
                    selection of most recent added gene, but only with a given
                    probability, albeit high.
                    point is, switching is not always going to be beneficial
                 */
                selectedSpecialization = specializationGenes.lastIndex
            } else if(specializationGenes.size > 1 && randomness.nextBoolean(0.1)){
                //choose another specialization, but with low probability
                selectedSpecialization = randomness.nextInt(0, specializationGenes.size-1, selectedSpecialization)
            } else{
                //just mutate current selection
                specializationGene.standardMutation(randomness, apc, allGenes)
            }
            selectionUpdatedSinceLastMutation = false
            return
        }

        if (!TaintInputName.isTaintInput(value)
                && randomness.nextBoolean(apc.getBaseTaintAnalysisProbability())) {

            value = TaintInputName.getTaintName(StaticCounter.getAndIncrease())
            return
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
                .map { it.value }
                .filter { it != value }

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
    }


    fun addSpecializations(key: String, specs: Collection<StringSpecializationInfo>, randomness: Randomness){

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
                .filter { s -> s.stringSpecialization != StringSpecialization.CONSTANT ||
                            (invalidChars.none { c -> s.value.contains(c) } && s.value.length >= minLength)}

        val toAddGenes = mutableListOf<Gene>()

        //all constant values are merged in the same enum gene
        if(toAddSpecs.any { it.stringSpecialization == CONSTANT}){
            /*
                TODO partial matches
             */
            toAddGenes.add(
                    EnumGene<String>(
                            name,
                            toAddSpecs.filter { it.stringSpecialization == CONSTANT }.map { it.value }))
        }

        if(toAddSpecs.any { it.stringSpecialization == CONSTANT_IGNORE_CASE}){
            toAddGenes.add(RegexHandler.createGeneForJVM(
                    toAddSpecs.filter { it.stringSpecialization == CONSTANT_IGNORE_CASE }
                            .map { "^(${RegexUtils.ignoreCaseRegex(it.value)})$" }
                            .joinToString("|")
            ))
        }


        if(toAddSpecs.any {it.stringSpecialization == DATE_YYYY_MM_DD}){
            toAddGenes.add(DateGene(name))
        }

        if(toAddSpecs.any {it.stringSpecialization == BOOLEAN}){
            toAddGenes.add(BooleanGene(name))
        }

        if(toAddSpecs.any {it.stringSpecialization == INTEGER}){
            toAddGenes.add(IntegerGene(name))
        }

        if(toAddSpecs.any {it.stringSpecialization == LONG}){
            toAddGenes.add(LongGene(name))
        }

        if(toAddSpecs.any {it.stringSpecialization == FLOAT}){
            toAddGenes.add(FloatGene(name))
        }

        if(toAddSpecs.any {it.stringSpecialization == DOUBLE}){
            toAddGenes.add(DoubleGene(name))
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

        if(toAddGenes.size > 0){
            selectionUpdatedSinceLastMutation = true
            toAddGenes.forEach {
                it.randomize(randomness, false, listOf())
                it.parent = this
            }
            specializationGenes.addAll(toAddGenes)
            specializations.addAll(toAddSpecs)
        }
    }


    private fun handleRegex(key: String, toAddSpecs: List<StringSpecializationInfo>, toAddGenes: MutableList<Gene>) {

        val fullPredicate = {s : StringSpecializationInfo -> s.stringSpecialization == REGEX && s.type.isFullMatch}
        val partialPredicate = {s : StringSpecializationInfo -> s.stringSpecialization == REGEX && s.type.isPartialMatch}

        if (toAddSpecs.any(fullPredicate)) {
            val regex = toAddSpecs
                    .filter(fullPredicate)
                    .map { it.value }
                    .joinToString("|")

            try {
                toAddGenes.add(RegexHandler.createGeneForJVM(regex))
            } catch (e: Exception){
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


    /**
     * Make sure no invalid chars is used
     */
    fun repair() {
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

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: EscapeMode?, targetFormat: OutputFormat?): String {

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
                else -> return "\"${GeneUtils.applyEscapes(rawValue, EscapeMode.TEXT ,targetFormat)}\""
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
            return tg.containsSameValueAs(og!!)
        }

        return this.value == other.value
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(specializationGenes.flatMap { it.flatView(excludePredicate) })
    }
    override fun archiveMutation(
            randomness: Randomness,
            allGenes: List<Gene>,
            apc: AdaptiveParameterControl,
            selection: GeneMutationSelectionMethod,
            geneImpact: GeneImpact?,
            geneReference: String,
            archiveMutator: ArchiveMutator,
            evi: EvaluatedIndividual<*>,
            targets: Set<Int>
    ) {
        val specializationGene = getSpecializationGene()

        if (specializationGene == null && specializationGenes.isNotEmpty()) {
            selectedSpecialization = randomness.nextInt(0, specializationGenes.size-1)
            selectionUpdatedSinceLastMutation = false
            return

        } else if (specializationGene != null) {
            if(selectionUpdatedSinceLastMutation && randomness.nextBoolean(0.5)){
                /*
                    selection of most recent added gene, but only with a given
                    probability, albeit high.
                    point is, switching is not always going to be beneficial
                 */
                selectedSpecialization = specializationGenes.lastIndex
            } else if(specializationGenes.size > 1 && randomness.nextBoolean(0.1)){
                //choose another specialization, but with low probability
                selectedSpecialization = randomness.nextInt(0, specializationGenes.size-1, selectedSpecialization)
            } else{
                //just mutate current selection
                specializationGene.standardMutation(randomness, apc, allGenes)
            }
            selectionUpdatedSinceLastMutation = false
            return
        }

        if (!TaintInputName.isTaintInput(value)
                && randomness.nextBoolean(apc.getBaseTaintAnalysisProbability())) {

            value = TaintInputName.getTaintName(StaticCounter.getAndIncrease())
            return
        }

        if (archiveMutator.enableArchiveGeneMutation()){
            dependencyInfo.mutatedtimes +=1
            archiveMutator.mutate(this)
            if (mutatedIndex < CHAR_MUTATION_INITIALIZED){
                log.warn("archiveMutation: mutatedIndex {} of this gene should be more than {}", mutatedIndex, NEVER_ARCHIVE_MUTATION)
            }
            if (charsMutation.size != value.length){
                log.warn("regarding string gene, a length {} of a value {} of the gene should be always same with a size {} of its charMutation", value.length, value, charsMutation.size)
            }
        }else{
            standardMutation(randomness, apc, allGenes)
        }
    }

    override fun reachOptimal() : Boolean{
       return lengthMutation.reached && (charsMutation.all { it.reached  }  || charsMutation.isEmpty())
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        if (!archiveMutator.enableArchiveGeneMutation()) return

        original as? StringGene ?: throw IllegalStateException("$original should be StringGene")
        mutated as? StringGene ?: throw IllegalStateException("$mutated should be StringGene")

        if (this != mutated){
            dependencyInfo.mutatedtimes +=1
            if (this.mutatedIndex == NEVER_ARCHIVE_MUTATION){
                initCharMutation()
            }
            this.mutatedIndex = mutated.mutatedIndex
        }
        if (mutatedIndex < CHAR_MUTATION_INITIALIZED){
            log.warn("archiveMutationUpdate: mutatedIndex {} of this gene should be more than {}", mutatedIndex, NEVER_ARCHIVE_MUTATION)
        }

        val previous = original.value
        val current = mutated.value

        if (previous.length != current.length){
            if (this != mutated){
                this.lengthMutation.reached = mutated.lengthMutation.reached
            }
            lengthUpdate(previous, current, mutated, doesCurrentBetter, archiveMutator)
        }else{
            charUpdate(previous, current, mutated, doesCurrentBetter, archiveMutator)
        }
    }
    private fun charUpdate(previous:String, current: String, mutated: StringGene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        val charUpdate = charsMutation[mutatedIndex]
        if (this != mutated){
            charUpdate.reached =
                mutated.charsMutation[mutatedIndex] .reached
        }

        val pchar = previous[mutatedIndex].toInt()
        val cchar = current[mutatedIndex].toInt()

        /*
            1) current char is not in min..max, but current is better -> reset
            2) cmutation is optimal, but current is better -> reset
         */
        val reset = doesCurrentBetter && (
                cchar !in charUpdate.preferMin..charUpdate.preferMax ||
                        charUpdate.reached
                )

        if (reset){
            charUpdate.preferMax = Char.MAX_VALUE.toInt()
            charUpdate.preferMin = Char.MIN_VALUE.toInt()
            charUpdate.reached = false
            dependencyInfo.resetTimes +=1
            if(dependencyInfo.resetTimes >=2) dependencyInfo.degreeOfIndependence = 0.8
            return
        }
        charUpdate.updateBoundary(pchar, cchar,doesCurrentBetter)

        val exclude = value[mutatedIndex].toInt()
        val excludes = invalidChars.map { it.toInt() }.plus(cchar).plus(exclude).toSet()

        if (0 == archiveMutator.validateCandidates(charUpdate.preferMin, charUpdate.preferMax, exclude = excludes.toList() )){
            charUpdate.reached = true
        }
    }

    private fun lengthUpdate(previous:String, current: String, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        //update charsMutation regarding value
        val added = value.length - charsMutation.size
        if (added != 0){
            if (added > 0){
                (0 until added).forEach { _->
                    charsMutation.add(archiveMutator.createCharMutationUpdate())
                }
            }else{
                (0 until -added).forEach { _ ->
                    charsMutation.removeAt(value.length)
                }
            }
        }

        if (value.length != charsMutation.size){
            throw IllegalArgumentException("invalid!")
        }
        /*
            1) current.length is not in min..max, but current is better -> reset
            2) lengthMutation is optimal, but current is better -> reset
         */
        val reset = doesCurrentBetter && (
                current.length !in lengthMutation.preferMin..lengthMutation.preferMax ||
                        lengthMutation.reached
                )

        if (reset){
            lengthMutation.preferMin = minLength
            lengthMutation.preferMax = maxLength
            lengthMutation.reached = false
            dependencyInfo.resetTimes +=1
            if(dependencyInfo.resetTimes >=2) dependencyInfo.degreeOfIndependence = 0.8
            return
        }
        lengthMutation.updateBoundary(previous.length, current.length, doesCurrentBetter)

        if(0 == archiveMutator.validateCandidates(lengthMutation.preferMin, lengthMutation.preferMax, exclude = setOf(previous.length, current.length, value.length).toList())){
            lengthMutation.reached = true
        }
    }

    private fun initCharMutation(){
        charsMutation.clear()
        charsMutation.addAll((0 until value.length).map { IntMutationUpdate(Char.MIN_VALUE.toInt(), Char.MAX_VALUE.toInt()) })
    }
}