package org.evomaster.core.utils

import java.util.*

object StringUtils {

    fun capitalization(word: String) : String{
        if(word.isEmpty()){
            return word
        }

        return word.substring(0, 1).uppercase(Locale.ENGLISH) +
                word.substring(1).lowercase(Locale.ENGLISH)
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


    /**
     * Given a list of tokens, and a separator, concatenate them.
     * however, if such concatenation is longer than [maxLength], split in different lines.
     */
    fun linesWithMaxLength(tokens: List<String>, separator: String, maxLength: Int) : List<String>{

        val lines = mutableListOf<String>()
        val buffer = StringBuffer()
        for(t in tokens){
            if(buffer.isEmpty()){
                buffer.append(t)
                continue
            }
            val len = buffer.length + separator.length + t.length
            if(len <= maxLength){
                buffer.append(separator)
                buffer.append(t)
            } else {
                lines.add(buffer.toString())
                buffer.delete(0, buffer.length)
                buffer.append(separator)
                buffer.append(t)
            }
        }
        if(buffer.isNotEmpty()){
            lines.add(buffer.toString())
        }
        return lines
    }
}
