package org.evomaster.core.remote


class SutProblemException(
        message : String = "Problem with the system under test."
) : RuntimeException(message)
