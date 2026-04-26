package org.evomaster.core.utils

/**
 *
 */
class UnicodeCache {
    companion object {
        /**
         * WARNING: mutable static state. But as it is just a cache, it is not a problem.
         * Furthermore, although the hashmap is mutable, the values inside are not
         */
        private val cache = HashMap<String, MultiCharacterRange>()
    }

    // UNICODE GENERAL CATEGORIES, keywords (gc, general_category) are case-insensitive,
    // prefix (Is) and names are case-sensitive

    /** CharCategory.code (e.g. "Lu") -> raw type integer from Character.getType() */
    private val categoryNameToType: Map<String, Int> = CharCategory.entries.associate { it.code to it.value }

    /**
     * Major general categories of the minor general categories, for example N groups the following:
     * Nd (Decimal Number), Nl (Letter Number), No (Other Number)
     */
    private val categoryGroups: Map<String, List<String>> = mapOf(
        "L" to listOf("Lu", "Ll", "Lt", "Lm", "Lo"),
        "M" to listOf("Mn", "Me", "Mc"),
        "N" to listOf("Nd", "Nl", "No"),
        "Z" to listOf("Zs", "Zl", "Zp"),
        "C" to listOf("Cc", "Cf", "Co", "Cs", "Cn"),
        "P" to listOf("Pd", "Ps", "Pe", "Pi", "Pf", "Po", "Pc"),
        "S" to listOf("Sm", "Sc", "Sk", "So"),
    )

    private val generalCategoryPredicates: Map<String, (Int) -> Boolean> =
        // minor categories: single Character.getType() comparison
        categoryNameToType.entries.associate { (name, type) ->
            "gc=$name" to { cp: Int -> Character.getType(cp) == type }
        } +
        // major categories: OR of their constituent minor predicates
        categoryGroups.entries.associate { (group, leaves) ->
            val types = leaves.map { categoryNameToType[it]!! }.toSet()
            "gc=$group" to { cp: Int -> Character.getType(cp) in types }
        }

    // UNICODE BINARY PROPERTIES, case-insensitive (except for "Is" prefix)
    private val binaryPropertiesPredicates: Map<String, (Int) -> Boolean> = mapOf(

        // common between java.lang.Character methods and Unicode binary properties
        "Isalphabetic" to { cp-> Character.isAlphabetic(cp) },
        "Isdigit" to { cp -> Character.isDigit(cp) },
        "Isideographic" to { cp -> Character.isIdeographic(cp) },
        "Isletter" to { cp -> Character.isLetter(cp) },
        "Islowercase" to { cp -> Character.isLowerCase(cp) },
        "Istitlecase" to { cp -> Character.isTitleCase(cp) },
        "Isuppercase" to { cp -> Character.isUpperCase(cp) },

        // Unicode binary properties
        // Whitespace is implemented different between Character.isWhitespace() and Unicode binary property
        "Iswhitespace" to { cp -> when (cp) {
            0x0085, 0x00A0, 0x2007, 0x202F -> true
            0x001C, 0x001D, 0x001E, 0x001F -> false
            else -> Character.isWhitespace(cp)
        } },

        // Unicode General Category P (all punctuation subcategories).
        "Ispunctuation" to { cp ->
            when (Character.getType(cp).toByte()) {
                Character.CONNECTOR_PUNCTUATION,  // Pc
                Character.DASH_PUNCTUATION,       // Pd
                Character.START_PUNCTUATION,      // Ps
                Character.END_PUNCTUATION,        // Pe
                Character.INITIAL_QUOTE_PUNCTUATION, // Pi
                Character.FINAL_QUOTE_PUNCTUATION,   // Pf
                Character.OTHER_PUNCTUATION       // Po
                    -> true
                else -> false
            }
        },

        // Unicode General Category Cc (control characters).
        "Iscontrol" to { cp ->
            Character.getType(cp) == Character.CONTROL.toInt()
        },

        // Hex_Digit: ASCII hex digits plus their fullwidth equivalents.
        "Ishexdigit" to { cp ->
            Character.isDigit(cp) ||   // all Unicode decimal digits (~660 chars)
                    cp in 0x0041..0x0046 ||   // A-F
                    cp in 0x0061..0x0066 ||   // a-f
                    cp in 0xFF21..0xFF26 ||   // A-F fullwidth
                    cp in 0xFF41..0xFF46      // a-f fullwidth
        },

        // Join_Control: exactly two codepoints 0x200C (ZWNJ) and 0x200D (ZWJ).
        // Defined in Unicode DerivedCoreProperties.txt as a fixed 2-element set.
        "Isjoincontrol" to { cp ->
            cp == 0x200C || cp == 0x200D
        },

        // Noncharacter_Code_Point: permanently reserved non-characters, never assigned a character.
        // Two fixed sets defined by Unicode:
        //   FDD0-FDEF: 32 code points in the Arabic Presentation Forms-A block
        //   Last two code points of each of the 17 Unicode planes (planes 0-16):
        //     FFFE,  FFFF  (plane 0, BMP)
        //     1FFFE, 1FFFF (plane 1)
        //     ... through 10FFFE, 10FFFF (plane 16)
        // The plane-end check uses (cp & 0xFFFF) to mask off the plane index,
        // leaving just the within-plane offset. If that is 0xFFFE or 0xFFFF,
        // the code point is a non-character regardless of which plane it is in.
        "Isnoncharactercodepoint" to { cp ->
            cp in 0xFDD0..0xFDEF ||
                    (cp and 0xFFFF) == 0xFFFE ||
                    (cp and 0xFFFF) == 0xFFFF
        },

        // Assigned: any codepoint that has been assigned a Unicode character,
        // i.e. its General Category is NOT Cn (UNASSIGNED).
        // Character.getType() returns UNASSIGNED (= 0) for unassigned codepoints.
        "Isassigned" to { cp ->
            Character.getType(cp) != Character.UNASSIGNED.toInt()
        },
    )

    // JAVA LANG CHARACTER METHODS, case-sensitive, prefixed by "java"
    private val javaCharacterMethodPredicates: Map<String, (Int) -> Boolean> = mapOf(

        // java.lang.Character methods
        "javaDefined" to { cp -> Character.isDefined(cp) },
        "javaIdentifierIgnorable" to { cp -> Character.isIdentifierIgnorable(cp) },
        "javaISOControl" to { cp -> Character.isISOControl(cp) },
        "javaJavaIdentifierPart" to { cp -> Character.isJavaIdentifierPart(cp) },
        "javaJavaIdentifierStart" to { cp -> Character.isJavaIdentifierStart(cp) },
        "javaLetterOrDigit" to { cp -> Character.isLetterOrDigit(cp) },
        "javaMirrored" to { cp -> Character.isMirrored(cp) },
        "javaSpaceChar" to { cp -> Character.isSpaceChar(cp) },
        "javaUnicodeIdentifierPart" to { cp -> Character.isUnicodeIdentifierPart(cp) },
        "javaUnicodeIdentifierStart" to { cp -> Character.isUnicodeIdentifierStart(cp) },
        "javaWhitespace" to { cp -> Character.isWhitespace(cp) },

        // common between java.lang.Character methods and Unicode binary properties
        "javaAlphabetic" to { cp -> Character.isAlphabetic(cp) },
        "javaDigit" to { cp -> Character.isDigit(cp) },
        "javaIdeographic" to { cp -> Character.isIdeographic(cp) },
        "javaLetter" to { cp -> Character.isLetter(cp) },
        "javaLowerCase" to { cp -> Character.isLowerCase(cp) },
        "javaTitleCase" to { cp -> Character.isTitleCase(cp) },
        "javaUpperCase" to { cp -> Character.isUpperCase(cp) },
    )

    private fun normalizeKey(name: String): String {

        val gcKey = name
            .replace(Regex("^(general_category=|gc=)", RegexOption.IGNORE_CASE), "")
            .removePrefix("Is")
        if ("gc=$gcKey" in generalCategoryPredicates) {
            return "gc=$gcKey"
        }

        val binaryKey = name
            .removePrefix("Is")
            .lowercase()
            .replace(Regex("(?<=[^_])_(?=[^_])"), "")
        if ("Is$binaryKey" in binaryPropertiesPredicates) {
            return "Is$binaryKey"
        }

        if (name in javaCharacterMethodPredicates) {
            return name
        }

        // no match found
        throw IllegalArgumentException("Unsupported/illegal category, binary property or java method")
    }

    private val minCharCode = 0
    private val maxCharCode = 0xffff

    private fun computeAndCache(key: String, predicate: (Int) -> Boolean){
        val list = mutableListOf<CharacterRange>()
        var start = minCharCode
        var prevPredicateResult = predicate(start)
        for (codePoint in minCharCode..maxCharCode) {
            val predicateResult = predicate(codePoint)
            if ( prevPredicateResult != predicateResult ) {
                if (prevPredicateResult) {
                    list.add(CharacterRange(start, codePoint - 1))
                }
                prevPredicateResult = predicateResult
                start = codePoint
            }
        }
        if (prevPredicateResult) {
            list.add(CharacterRange(start, maxCharCode))
        }

        if(list.isEmpty()) {
            throw IllegalArgumentException("Can not create empty character class for $key")
        }
        cache[key] = MultiCharacterRange(list.toList())
    }

    fun getRanges(name: String, negated: Boolean): MultiCharacterRange {
        val key = normalizeKey(name)
        val fullKey = if (negated) {
            "^$key"
        } else {
            key
        }
        return if (fullKey in cache) {
            cache[fullKey]!!
        } else {
            val predicate = when (key) {
                in generalCategoryPredicates -> generalCategoryPredicates[key]
                in binaryPropertiesPredicates -> binaryPropertiesPredicates[key]
                in javaCharacterMethodPredicates -> javaCharacterMethodPredicates[key]
                else -> null
            }
            if (key !in cache) {
                computeAndCache(key, predicate!!)
            }
            if (negated) {
                cache[fullKey] = MultiCharacterRange(true, cache[key]!!.ranges)
            }
            cache[fullKey]!!
        }
    }
}