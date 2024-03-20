package org.evomaster.core.problem.enterprise.auth

/**
 * representing AuthenticationInfo
 */
abstract class AuthenticationInfo(
    /**
     * name of the Authentication
     */
    val name: String){


    init {
        if (name.isBlank()) {
            throw IllegalArgumentException("Blank name")
        }
    }
}