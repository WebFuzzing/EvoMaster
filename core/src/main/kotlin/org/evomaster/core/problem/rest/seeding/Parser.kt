package org.evomaster.core.problem.rest.seeding

import org.evomaster.core.problem.rest.RestIndividual

interface Parser {

    /**
     * Transform a set of test cases in a specific format (e.g., Postman, JUnit, etc.)
     * into a set of test cases that EvoMaster can handle
     *
     * @param path Path of the file containing the test cases
     */
    fun parseTestCases(path: String): MutableList<RestIndividual>
}