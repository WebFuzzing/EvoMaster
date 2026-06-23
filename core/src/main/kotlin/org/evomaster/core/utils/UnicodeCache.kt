package org.evomaster.core.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * Cache for Unicode character ranges used in `\p{}` and `\P{}` regex escape sequences.
 *
 * Supported property types, with their accepted syntax:
 * - **Script**           : `IsArabic`, `sc=Latin`, `script=Latin`
 * - **Block**            : `InBasicLatin`, `blk=Greek`, `block=Greek`
 * - **General Category** : `Lu`, `gc=P`, `general_category=P`, `IsLu`
 * - **Binary Property**  : `IsAlphabetic`, `Ishex_digit`
 * - **Java method**      : `javaAlphabetic`, `javaWhitespace`
 *
 * Property name matching follows these rules:
 * - Keywords (`gc=`, `sc=`, `blk=`, etc.) are case-insensitive
 * - Prefixes (`Is`, `In`, `java`) are case-sensitive
 * - Script, block and binary property names are case-insensitive and allow single underscores as word separators
 * - General category and java method names are case-sensitive
 *
 * Results are computed lazily on first access and cached statically for the lifetime of the JVM.
 * The cache is shared across all instances of this class.
 */
class UnicodeCache {
    companion object {
        /**
         * WARNING: mutable static state. But as it is just a cache, it is not a problem.
         * Furthermore, although the hashmap is mutable, the values inside are not
         */
        private val cache = ConcurrentHashMap<String, MultiCharacterRange>()
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

    // UNICODE BLOCKS, names and keywords (blk, block) are case-insensitive, prefix is case-sensitive (In)
    private val blockPredicates: Map<String, (Int) -> Boolean> = Character.UnicodeBlock::class.java.fields
        .filter { it.type == Character.UnicodeBlock::class.java }
        .associate { field ->
            "blk=${field.name.lowercase()}" to { cp: Int ->
                Character.UnicodeBlock.of(cp) == field.get(null)
            }
        }

    // UNICODE SCRIPTS, names and keywords (sc, script) are case-insensitive, prefix is case-sensitive (Is)
    private val scriptPredicates: Map<String, (Int) -> Boolean> = Character.UnicodeScript.entries.associate { script ->
        "sc=${script.toString().lowercase()}" to { cp: Int ->
            Character.UnicodeScript.of(cp) == script
        }
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

    /*
       We need to normalize keys so that "gc=L" and "L" point to the same thing to avoid unnecessary re-computation.
       Keywords are always case-insensitive, prefixes are always case-sensitive. Most properties need either a keyword
       or a prefix (except for general categories)
     */
    private fun normalizeKey(pEscapeLabel: String): String {

        // Scripts: keywords or "Is" prefix, actual scripts name are case-insensitive and optionally in snake case.
        val scriptKey = if (pEscapeLabel.startsWith("Is")) {
            pEscapeLabel.removePrefix("Is")
        } else {
            pEscapeLabel.replace(Regex("^(script=|sc=)", RegexOption.IGNORE_CASE), "")
        }
            .lowercase() // scripts name are case-insensitive
            .replace(Regex("(?<=[^_])_(?=[^_])"), "") // handle snake case
        if ("sc=$scriptKey" in scriptPredicates) {
            return "sc=$scriptKey"
        }

        // Blocks: keywords or "In" prefix, actual blocks name are case-insensitive and optionally in snake case.
        val blockKey = if (pEscapeLabel.startsWith("In")) {
            pEscapeLabel.removePrefix("In")
        } else {
            pEscapeLabel.replace(Regex("^(block=|blk=)", RegexOption.IGNORE_CASE), "")
        }
            .lowercase() // blocks name are case-insensitive
            .replace(Regex("(?<=[^_])_(?=[^_])"), "") // handle snake case
        if ("blk=$blockKey" in blockPredicates) {
            return "blk=$blockKey"
        }

        // General categories: keywords, "Is", names are case-sensitive.
        val gcKey = if (pEscapeLabel.startsWith("Is")) {
            pEscapeLabel.removePrefix("Is")
        } else {
            pEscapeLabel.replace(Regex("^(general_category=|gc=)", RegexOption.IGNORE_CASE), "")
        }
        if ("gc=$gcKey" in generalCategoryPredicates) {
            return "gc=$gcKey"
        }

        // Binary properties: "Is" prefix, actual properties name are case-insensitive and optionally in snake case.
        val binaryKey = pEscapeLabel
            .removePrefix("Is")
            .lowercase() // binary properties name are case-insensitive
            .replace(Regex("(?<=[^_])_(?=[^_])"), "") // handle snake case
        if ("Is$binaryKey" in binaryPropertiesPredicates) {
            return "Is$binaryKey"
        }

        // Java character methods: java prefix, exact match, case-sensitive
        if (pEscapeLabel in javaCharacterMethodPredicates) {
            return pEscapeLabel
        }

        // no match found
        throw IllegalArgumentException("Unsupported/illegal category, binary property or java method")
    }

    /*
    Filters characters by predicate, constructing a list in a way that skips the MultiCharacterRange construction logic.
     */
    private fun computeRanges(key: String, predicate: (Int) -> Boolean): MultiCharacterRange {
        val list = mutableListOf<CharacterRange>()
        var start = Character.MIN_VALUE.code
        val end = Character.MAX_VALUE.code
        var prevPredicateResult = predicate(start)
        for (codePoint in 0..end) {
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
            list.add(CharacterRange(start, end))
        }

        return MultiCharacterRange(list.toList())
    }

    /**
     * Returns a [MultiCharacterRange] representing the set of Unicode code points matched by
     * a `\p{`[pEscapeLabel]`}` (or `\P{`[pEscapeLabel]`}` if [negated]) regex escape sequence.
     *
     * The result is computed lazily on first access and cached for subsequent calls.
     * [pEscapeLabel] is normalized before lookup, so e.g. `"hex_digit"` and `"hexdigit"` resolve to the same entry.
     *
     * The following Unicode property types are supported:
     * - Script (e.g. `IsArabic`, `sc=Latin`)
     * - Block (e.g. `InBasicLatin`, `blk=Greek`)
     * - General Category (e.g. `Lu`, `gc=P`)
     * - Binary Property (e.g. `IsAlphabetic`, `Ishex_digit`)
     * - Java character method (e.g. `javaAlphabetic`)
     *
     * @param pEscapeLabel the property name, including keywords or prefixes (e.g. `"gc="`, `"sc="`, `"Is"`, `"In"`)
     * @param negated if `true`, returns the complement of the matched set (equivalent to `\P{`[pEscapeLabel]`}`)
     * @throws NullPointerException if [pEscapeLabel] does not resolve to any known property
     */
    fun getRanges(pEscapeLabel: String, negated: Boolean): MultiCharacterRange {
        val key = normalizeKey(pEscapeLabel)
        val fullKey = if (negated) {
            "^$key"
        } else {
            key
        }

        val predicate = when (key) {
            in scriptPredicates -> scriptPredicates[key]
            in blockPredicates -> blockPredicates[key]
            in generalCategoryPredicates -> generalCategoryPredicates[key]
            in binaryPropertiesPredicates -> binaryPropertiesPredicates[key]
            in javaCharacterMethodPredicates -> javaCharacterMethodPredicates[key]
            else -> null
        }

        // first we compute and cache the base key (non-negated)
        cache.computeIfAbsent(key) {
            computeRanges(key, predicate!!)
        }

        // if the base kay was requested just return
        if (!negated) return cache[key]!!

        // else compute and cache full key (negated) from base key (non-negated)
        return cache.computeIfAbsent(fullKey) {
            MultiCharacterRange(true, cache[key]!!.ranges)
        }
    }
}