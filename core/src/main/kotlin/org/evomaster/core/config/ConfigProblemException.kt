package org.evomaster.core.config

class ConfigProblemException(
    message : String = "Problem with the configuration of EvoMaster."
) : RuntimeException(message)