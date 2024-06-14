package org.evomaster.core.remote


open class SutProblemException(
        message : String = "Problem with the system under test."
) : RuntimeException(message)
