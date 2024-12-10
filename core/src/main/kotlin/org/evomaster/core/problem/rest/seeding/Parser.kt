package org.evomaster.core.problem.rest.seeding

import org.evomaster.core.problem.rest.RestCallAction


@Deprecated("Code here will be replaced with intermediate representation. See new 'seeding' package")
interface Parser {

    /**
     * Transform a set of test cases in a specific format (e.g., Postman, JUnit, etc.)
     * into a set of test cases that EvoMaster can handle. Each test case in the set
     * is a sequence of calls. The RestIndividuals must be created by the RestSampler
     *
     * @param path Path of the file containing the test cases
     */
    fun parseTestCases(path: String): MutableList<MutableList<RestCallAction>>
}