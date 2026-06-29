package org.evomaster.core.utils

data class RegexWithExternalFlags(val regex: String, val externalRegexFlags: RegexFlags) {
    constructor (regex: String, externalRegexFlagBitmask: Int) : this(regex, RegexFlags.fromExternalJavaRegexFlagBitmask(externalRegexFlagBitmask))
}