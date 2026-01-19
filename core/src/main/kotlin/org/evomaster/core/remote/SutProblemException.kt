package org.evomaster.core.remote


open class SutProblemException(
        message : String = "Problem with the system under test.",
        val tag: String? = null
) : RuntimeException(message)
