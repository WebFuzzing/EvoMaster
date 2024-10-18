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

    /**
     * A function for extracting the Simple Class Name from a class string. Given an inner class in the
     * OuterClass$InnerClass format, it will return the InnerClass value
     *
     * @param fullyQualifiedName of the class to extract
     * @return the simple class name string
     */
    fun extractSimpleClass(fullyQualifiedName: String) : String {
        if (fullyQualifiedName.isNullOrEmpty()) return ""

        // Replace $ with . and then split by . to handle inner classes
        val parts = fullyQualifiedName.replace('$', '.').split(".")
        return parts.last()
    }
}
