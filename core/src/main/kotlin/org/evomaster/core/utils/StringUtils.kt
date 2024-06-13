package org.evomaster.core.utils

import java.util.*

class StringUtils {
    companion object {
        fun capitalization(word: String) =
            word.substring(0, 1).uppercase(Locale.getDefault()) +
                    word.substring(1).lowercase(Locale.getDefault())
    }
}