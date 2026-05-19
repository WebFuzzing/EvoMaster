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
     * Converts a string to a valid ASCII identifier for use in SMT-LIB.
     * SMT-LIB unquoted symbols are restricted to ASCII.
     *
     * The conversion uses two complementary steps:
     * 1. An explicit folding map for characters that have no canonical decomposition under NFD
     *    (e.g., Ø→O, Æ→AE, ß→ss, ð→d, þ→th, Ł→L, Œ→OE, ŋ→n, ħ→h, ı→i, …),
     *    covering non-decomposable characters from the Unicode Latin Extended blocks.
     * 2. NFD normalization followed by stripping of non-ASCII combining marks, which handles
     *    all accented characters that do decompose (e.g., é→e, ü→u, ñ→n, Ä→A, ö→o, å→a).
     *
     * Any remaining non-ASCII characters (e.g., from non-Latin scripts) are dropped.
     */
    fun convertToAscii(name: String): String {
        val sb = StringBuilder(name.length * 2)
        for (ch in name) {
            sb.append(ASCII_FOLD_MAP[ch] ?: ch.toString())
        }
        return java.text.Normalizer.normalize(sb.toString(), java.text.Normalizer.Form.NFD)
            .replace(Regex("[^\\x00-\\x7F]"), "")
    }

    /**
     * Explicit ASCII replacements for Unicode characters that do not decompose under NFD normalization.
     * Covers non-decomposable characters from the Unicode Latin-1 Supplement and Latin Extended-A/B blocks.
     * Characters that DO decompose under NFD (e.g., Ä, ö, å, é, ü, ñ) are handled by the NFD step in
     * [convertToAscii] and need no entry here.
     */
    private val ASCII_FOLD_MAP: Map<Char, String> = mapOf(
        // Latin-1 Supplement
        'Æ' to "AE", 'æ' to "ae",   // AE ligature (Danish, Norwegian, Old English)
        'Ð' to "D",  'ð' to "d",    // Eth (Icelandic, Old English)
        'Ø' to "O",  'ø' to "o",    // O with stroke (Danish, Norwegian)
        'Þ' to "TH", 'þ' to "th",   // Thorn (Icelandic, Old English)
        'ß' to "ss",                  // Sharp S (German)
        // Latin Extended-A
        'Ħ' to "H",  'ħ' to "h",    // H with stroke (Maltese)
        'ı' to "i",                   // Dotless i (Turkish, Azerbaijani)
        'Ĳ' to "IJ", 'ĳ' to "ij",   // IJ digraph (Dutch)
        'ĸ' to "k",                   // Kra (Greenlandic)
        'Ł' to "L",  'ł' to "l",    // L with stroke (Polish, Croatian, Sorbian)
        'Ŋ' to "N",  'ŋ' to "n",    // Eng (Sami, African languages)
        'Œ' to "OE", 'œ' to "oe",   // OE ligature (French)
        'Ŧ' to "T",  'ŧ' to "t",    // T with stroke (Sami)
        // Latin Extended-B
        'ƀ' to "b",  'Ƀ' to "B",    // B with stroke
        'Ɓ' to "B",                   // B with hook
        'Ƈ' to "C",  'ƈ' to "c",    // C with hook
        'Ɗ' to "D",                   // D with hook
        'ƌ' to "d",                   // D with topbar
        'Ƒ' to "F",  'ƒ' to "f",    // F with hook
        'Ɠ' to "G",                   // G with hook
        'Ɨ' to "I",                   // I with stroke
        'Ƙ' to "K",  'ƙ' to "k",    // K with hook
        'ƚ' to "l",                   // L with bar
        'Ɲ' to "N",  'ƞ' to "n",    // N with hook / N with long right leg
        'Œ' to "OE", 'œ' to "oe",
        'Ƥ' to "P",  'ƥ' to "p",    // P with hook
        'ƫ' to "t",                   // T with palatal hook
        'Ƭ' to "T",  'ƭ' to "t",    // T with hook
        'Ʈ' to "T",                   // T with retroflex hook
        'Ư' to "U",  'ư' to "u",    // U with horn (Vietnamese)
        'Ʋ' to "V",                   // V with hook
        'Ƴ' to "Y",  'ƴ' to "y",    // Y with hook
        'Ƶ' to "Z",  'ƶ' to "z",    // Z with stroke
        'Ǝ' to "E",  'ǝ' to "e",    // Reversed E / Schwa
        'Ɵ' to "O",                   // O with middle tilde
        'Ȼ' to "C",  'ȼ' to "c",    // C with stroke
        'Ɇ' to "E",  'ɇ' to "e",    // E with stroke
        'Ɉ' to "J",  'ɉ' to "j",    // J with stroke
        'Ɋ' to "Q",  'ɋ' to "q",    // Q with hook tail
        'Ɍ' to "R",  'ɍ' to "r",    // R with stroke
        'Ɏ' to "Y",  'ɏ' to "y",    // Y with stroke
    )

    /**
     * Checks whether the given string looks like a number or a boolean value.
     *
     * The function trims leading and trailing whitespace, then returns true if the
     * resulting value is either "true", "false", or a valid decimal number.
     */
    fun looksLikeNumberOrBoolean(s: String): Boolean {
        val t = s.trim()
        if (t == "true" || t == "false") return true
        return t.toBigDecimalOrNull() != null
    }
}
