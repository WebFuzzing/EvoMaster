package org.evomaster.core.output.naming

import org.evomaster.core.search.action.Action

interface AmbiguitySolver {

    /**
     * @param action providing information to disambiguate the test case name
     * @param remainingNameChars to decide if a token is added to the name or not
     *
     * @return list of strings to be added to the test case name
     */
    fun apply(action: Action, remainingNameChars: Int): List<String>

}
