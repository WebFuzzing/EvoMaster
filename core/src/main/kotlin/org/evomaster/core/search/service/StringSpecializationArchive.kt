package org.evomaster.core.search.service

import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import java.util.Collections

/**
 * Keep track of all the string specializations added during the search
 */
class StringSpecializationArchive {

    /**
     * How often a string specialization has been seen / added.
     * Recall that [StringSpecializationInfo] is immutable.
     *
     * Missing keys are assumed to have value 0.
     */
    private val occurrences : MutableMap<StringSpecializationInfo, Int> = mutableMapOf()


    /**
     * Map from variable name (ie name of StringGene) and all its specialization info seen so far.
     * Note that variable names are not unique, eg the same HTTP Query Parameter could be used in different
     * endpoints.
     */
    private val specializationsForVariable : MutableMap<String, MutableSet<StringSpecializationInfo>> = mutableMapOf()


    fun updateStats(name: String, info: StringSpecializationInfo){
        updateStats(name, listOf(info))
    }

    fun updateStats(name: String, specs : Collection<StringSpecializationInfo>){
        for(info in specs){
            occurrences.compute(info){ _,v -> v?.plus(1) ?: 1}
            specializationsForVariable.compute(name){_,v -> v?.apply {add(info)} ?: mutableSetOf(info) }
        }
    }

    fun hasInfoFor(name: String) = specializationsForVariable.containsKey(name)

    fun chooseSpecialization(name: String, rand: Randomness) : StringSpecializationInfo? {

        val specs = specializationsForVariable[name]
        if(specs == null || specs.isEmpty()){
            return null //can this even happen??? likely not
        }

        //the more occurrences, the least chances to be chosen
        val probabilities = specs.associateWith { 1.0 / (1.0 + occurrences[it]!!) }

        return rand.chooseByProbability(probabilities)
    }
}