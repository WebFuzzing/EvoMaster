package org.evomaster.core.search.gene

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.client.java.instrumentation.shared.StringSpecialization.*
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.GeneUtils.getDelta
import org.evomaster.core.search.impact.*
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory


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
         * Based on taint analysis, in some cases we can determine how some Strings are
         * used in the SUT.
         * For example, if a String is used as a Date, then it make sense to use a specialization
         * in which we mutate to have only Strings that are valid dates
         */
        var specializations: List<StringSpecializationInfo> = listOf()
) : Gene(name) {

    companion object {
        /*
            WARNING
            mutable static state.
            only used to create unique names
         */
        private var counter: Int = 0

        private val log: Logger = LoggerFactory.getLogger(StringGene::class.java)
    }

    /*
        Even if through mutation we can get large string, we should
        avoid sampling very large strings by default
     */
    private val maxForRandomization = 16

    private var validChar: String? = null

    var specializationGene: Gene? = null

    /**
     * chars at 0..[mutatedIndex] (exclusion) are set, only used by archive-based mutation
     * when [mutatedIndex] = -1, it means that chars of [this] have not be mutated yet
     */
    var mutatedIndex : Int = -1

    var preferCharMin = Char.MIN_VALUE.toInt()
    var preferCharMax = Char.MAX_VALUE.toInt()

    var noImproveCharCounter = 0

    var targetCharFoundAtMutatedIndex = false

    var preferLengthMin = minLength
    var preferLengthMax = maxLength

    var targetLengthFoundAtMutatedIndex = false

    var noImproveLengthCounter = 0


    override fun copy(): Gene {
        return StringGene(name, value, minLength, maxLength, invalidChars, specializations)
                .also {
                    it.specializationGene = this.specializationGene?.copy()
                    it.validChar = this.validChar

                    it.mutatedIndex = mutatedIndex
                    it.preferCharMin = preferCharMin
                    it.preferCharMax = preferCharMax
                    it.targetCharFoundAtMutatedIndex = targetCharFoundAtMutatedIndex
                    it.preferLengthMin = preferLengthMin
                    it.preferLengthMax = preferLengthMax
                    it.targetLengthFoundAtMutatedIndex = targetLengthFoundAtMutatedIndex
                    it.noImproveCharCounter = noImproveCharCounter
                    it.noImproveLengthCounter = noImproveLengthCounter
                }
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        value = randomness.nextWordString(minLength, Math.min(maxLength, maxForRandomization))
        repair()
        specializationGene = null
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        if (specializationGene == null && specializations.isNotEmpty()) {
            chooseSpecialization()
            assert(specializationGene != null)
        }

        if (specializationGene != null) {
            specializationGene!!.standardMutation(randomness, apc, allGenes)
            return
        }

        if (specializationGene == null
                && !TaintInputName.isTaintInput(value)
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

        val others = allGenes.flatMap { g -> g.flatView() }
                .filterIsInstance<StringGene>()
                .map { g -> g.value }
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

    private fun chooseSpecialization() {
        assert(specializations.isNotEmpty())

        specializationGene = when {
            specializations.any { it.stringSpecialization == DATE_YYYY_MM_DD } -> DateGene(name)

            specializations.any { it.stringSpecialization == INTEGER } -> IntegerGene(name)

            specializations.any { it.stringSpecialization == CONSTANT } -> EnumGene<String>(name,
                        specializations.filter { it.stringSpecialization == CONSTANT }.map { it.value }
                )

            else -> {
                //should never happen
                throw IllegalStateException("Cannot handle specialization")
            }
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

        if (specializationGene != null) {
            return "\"" + specializationGene!!.getValueAsPrintableString(previousGenes, mode, targetFormat) + "\""
        }

        val rawValue = getValueAsRawString()
        if (mode != null && mode.equals("xml")) {
            return StringEscapeUtils.escapeXml(rawValue)
        } else {
            when {
                // TODO this code should be refactored with other getValueAsPrintableString() methods
                (targetFormat == null) -> return "\"$rawValue\""
                targetFormat.isKotlin() -> return "\"$rawValue\""
                        .replace("\\", "\\\\")
                        .replace("$", "\\$")
                else -> return "\"$rawValue\""
                        .replace("\\", "\\\\")
            }
        }
    }

    override fun getValueAsRawString(): String {
        if (specializationGene != null) {
            return specializationGene!!.getValueAsRawString()
        }
        return value
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
        if (other.specializationGene == null) {
            this.specializationGene = null
        } else {
            this.specializationGene?.copyValueFrom(other.specializationGene!!)
        }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if ((this.specializationGene == null && other.specializationGene != null) ||
                (this.specializationGene != null && other.specializationGene == null)) {
            return false
        }

        if (this.specializationGene != null) {
            return this.specializationGene!!.containsSameValueAs(other.specializationGene!!)
        }

        return this.value == other.value
    }

    override fun archiveMutation(
            randomness: Randomness,
            allGenes: List<Gene>,
            apc: AdaptiveParameterControl,
            selection: ImpactMutationSelection,
            geneImpact: GeneImpact?,
            geneReference : String,
            evi: EvaluatedIndividual<*>
    ) {
        if (specializationGene == null && specializations.isNotEmpty()) {
            chooseSpecialization()
            assert(specializationGene != null)
        }

        if (specializationGene != null) {
            val impact = geneImpact?: evi.getImpactOfGenes()[ImpactUtils.generateGeneId(evi.individual, this)] ?: throw IllegalStateException("cannot find this gene in the individual")
            specializationGene!!.archiveMutation(randomness, allGenes, apc, selection, impact, geneReference, evi)
            return
        }

        ArchiveStringMutationUtils.mutate(this, randomness)
    }

    fun copyMutationInfo(gene : StringGene){
        mutatedIndex = gene.mutatedIndex
        targetCharFoundAtMutatedIndex = gene.targetCharFoundAtMutatedIndex
        targetLengthFoundAtMutatedIndex = gene.targetLengthFoundAtMutatedIndex
        if (mutatedIndex == -1){
            preferCharMin = gene.preferCharMin
            preferCharMax = gene.preferCharMax
        }
    }

    override fun reachOptimal() : Boolean{
       return targetLengthFoundAtMutatedIndex && targetCharFoundAtMutatedIndex && mutatedIndex == value.length
    }

}