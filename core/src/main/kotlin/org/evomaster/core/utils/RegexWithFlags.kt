package org.evomaster.core.utils

data class RegexWithFlags(val regex: String, val regexFlags: RegexFlags) {
    constructor (regex: String, regexFlagInt: Int) : this(regex, RegexFlags.fromExternalJavaRegexFlagBitmask(regexFlagInt))
}