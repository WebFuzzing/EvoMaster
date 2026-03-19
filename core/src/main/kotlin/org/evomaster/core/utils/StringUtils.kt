package org.evomaster.core.utils

object StringUtils {

    /**
     * Capitalizes a word, lowercasing the rest of the word. For example, stringProperty would be modified into
     * Stringproperty.
     *
     * @param word to be capitalized
     * @return the capitalized word
     */
    fun capitalization(word: String) : String{
        if(word.isEmpty()){
            return word
        }

        return word.substring(0, 1).uppercase() +
                word.substring(1).lowercase()
    }

    /**
     * Capitalizes the first char of a word, without modifying the rest. For example, stringProperty would be
     * modified into StringProperty. This is useful for writing setter methods in output, where the name matches
     * the property being serialized.
     *
     * @param word to be capitalized
     * @return the capitalized word
     */
    fun capitalizeFirstChar(word: String) : String {
        if(word.isEmpty()){
            return word
        }

        return word.substring(0, 1).uppercase() +
                word.substring(1)
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

    /**
     * Replaces non-ASCII characters in a name to make it a valid SMT-LIB identifier.
     * SMT-LIB unquoted symbols are restricted to ASCII, so characters like Æ, Ø, Å must be transliterated.
     *
     * This is needed because our test suite includes Norwegian APIs whose database schemas
     * contain column and table names with Norwegian characters (Æ, Ø, Å).
     *
     * Characters that do not decompose under NFD (Ø, Æ) are replaced explicitly.
     * Characters that decompose under NFD (Å→A, and other accented letters like é, ü, ñ)
     * are handled by normalizing to NFD form and stripping the remaining non-ASCII combining marks.
     */
    fun convertToAscii(name: String): String {
        val replaced = name
            .replace('Ø', 'O').replace('ø', 'o')
            .replace("Æ", "AE").replace("æ", "ae")
        return java.text.Normalizer.normalize(replaced, java.text.Normalizer.Form.NFD)
            .replace(Regex("[^\\x00-\\x7F]"), "")
    }
}
