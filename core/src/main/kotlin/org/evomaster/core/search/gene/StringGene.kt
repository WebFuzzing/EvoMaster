package org.evomaster.core.search.gene

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.client.java.instrumentation.shared.StringSpecialization.*
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.search.gene.GeneUtils.getDelta
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness


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
        val invalidChars: List<Char> = listOf()

) : Gene(name) {

    companion object {
        /*
            WARNING
            mutable static state.
            only used to create unique names
         */
        private var counter: Int = 0
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


    override fun copy(): Gene {
        return StringGene(name, value, minLength, maxLength, invalidChars)
                .also {
                    it.specializationGenes = this.specializationGenes.map { g -> g.copy() }.toMutableList()
                    it.specializations.addAll(this.specializations)
                    it.validChar = this.validChar
                    it.selectedSpecialization = this.selectedSpecialization
                    it.selectionUpdatedSinceLastMutation = this.selectionUpdatedSinceLastMutation
                }
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

            value = TaintInputName.getTaintName(counter++.toString())
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


    fun addSpecializations(specs: Collection<StringSpecializationInfo>, randomness: Randomness){

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
            toAddGenes.add(
                    EnumGene<String>(
                            name,
                            toAddSpecs.filter { it.stringSpecialization == CONSTANT }.map { it.value }))
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
        if(toAddSpecs.any {it.stringSpecialization == REGEX}){
            val regex = toAddSpecs
                    .filter { it.stringSpecialization == REGEX }
                    .map { it.value }
                    .joinToString("|")
            toAddGenes.add(RegexHandler.createGeneForJVM(regex))
        }

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
            toAddGenes.forEach { it.randomize(randomness, false, listOf()) }
            specializationGenes.addAll(toAddGenes)
            specializations.addAll(toAddSpecs)
        }
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

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {

        val specializationGene = getSpecializationGene()

        if (specializationGene != null) {
            return "\"" + specializationGene.getValueAsPrintableString(previousGenes, mode, targetFormat) + "\""
        }

        val rawValue = getValueAsRawString()
        if (mode != null && mode.equals("xml")) {
            return StringEscapeUtils.escapeXml(rawValue)
        } else {
            when {
                // TODO this code should be refactored with other getValueAsPrintableString() methods
                (targetFormat == null) -> return "\"${GeneUtils.applyEscapes(rawValue)}\""
                //"\"${rawValue.replace("\"", "\\\"")}\""
                (mode != null) -> return "\"${GeneUtils.applyEscapes(rawValue, mode, targetFormat)}\""
                else -> return "\"${GeneUtils.applyEscapes(rawValue, "text" ,targetFormat)}\""
            }

        }
    }

    /*
    fun getValueAsPrintableString(mode: String?, targetFormat: OutputFormat?): String {
        val rawValue = getValueAsRawString()
        if (mode != null && mode.equals("xml")) {
            return StringEscapeUtils.escapeXml(rawValue)
        } else {
            when {
                (targetFormat == null) -> return "\"$rawValue\""
                else -> return GeneUtils.applyEscapes(rawValue, "json", targetFormat)
                /*
                targetFormat.isKotlin() -> return "\"$rawValue\""
                        .replace("\\", "\\\\")
                        .replace("$", "\\$")
                else -> return "\"$rawValue\""
                        .replace("\\", "\\\\")
                 */
            }
        }
    }


     */

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
}