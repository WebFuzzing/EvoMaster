package org.evomaster.core.problem.enterprise.service

import com.webfuzzing.commons.faults.FaultCategory

/**
 * In EvoMaster we have many different types of oracles used to detect faults.
 * Not all oracles apply to all SUTs.
 * Eg, an oracle that needs a PATCH will not work for sure on an API that has no PATCH.
 *
 * Even if an oracle could be in theory applied on an SUT, it does not mean it can always do.
 * For example, if an oracle needs a PATCH with a 2xx response, it cannot be used if fuzzing session
 * failed to create any valid success call.
 *
 * There is a difference between the "static" properties of an SUT (eg PATCH exists) and the
 * "dynamic" properties related to the effectiveness of the fuzzing process (eg a 2xx was generated).
 *
 * This class is used to keep track of such info
 */
class OracleApplicability {

    private data class OAEntry(
        val code: Int,
        val potentialTotal: Int,
        val verifiableTotal: Int,
    ){
        init{
            if(potentialTotal < 0){
                throw IllegalArgumentException("Negative potentialTotal: $potentialTotal")
            }
            if(verifiableTotal < 0){
                throw IllegalArgumentException("Negative verifiableTotal: $verifiableTotal")
            }
            if(verifiableTotal > potentialTotal){
                throw IllegalArgumentException("verifiableTotal $verifiableTotal cannot be greater than potentialTotal $potentialTotal")
            }
        }
    }

    /**
     * Oracle Applicability entries (value) for each fault code (key)
     */
    private val data = mutableMapOf<Int, OAEntry>()


    /**
     * Report information about the applicability of [faultCategory] oracle.
     * @param potentialTotal the number of operations/endpoints it could be potentially applied on
     * @param verifiableTotal the number of operations/endpoints that can be actually applied on based on the
     *                        generated test cases so far
     */
    fun reportStats(faultCategory: FaultCategory, potentialTotal: Int, verifiableTotal: Int) {

        val code = faultCategory.code
        if(data.containsKey(code)) {
            throw IllegalArgumentException("Oracle Applicability info for code $code has already been provided")
        }

        if(potentialTotal < 0){
            throw IllegalArgumentException("Negative potentialTotal: $potentialTotal")
        }
        if(verifiableTotal < 0){
            throw IllegalArgumentException("Negative verifiableTotal: $verifiableTotal")
        }
        if(verifiableTotal > potentialTotal){
            throw IllegalArgumentException("verifiableTotal $verifiableTotal cannot be greater than potentialTotal $potentialTotal")
        }

        data[code] = OAEntry(code, potentialTotal, verifiableTotal)
    }

    fun exportStatsAsSingleString(): String {
        return data.values
            .sortedBy { it.code }
            .joinToString(separator = "|") {
                "${it.code}:${it.potentialTotal}:${it.verifiableTotal}"
            }
    }
}