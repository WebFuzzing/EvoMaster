package org.evomaster.core.utils

import java.util.*

object StringUtils {

        fun capitalization(word: String) : String{
            if(word.isEmpty()){
                return word
            }

            return word.substring(0, 1).uppercase(Locale.getDefault()) +
                    word.substring(1).lowercase(Locale.getDefault())
        }
}