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

    private fun normalizeKey(name: String): String {

        val gcKey = name
            .replace(Regex("^(general_category=|gc=)", RegexOption.IGNORE_CASE), "")
            .removePrefix("Is")
        if ("gc=$gcKey" in generalCategoryPredicates) {
            return "gc=$gcKey"
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