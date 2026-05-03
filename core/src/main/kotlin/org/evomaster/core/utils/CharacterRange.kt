package org.evomaster.core.utils

class CharacterRange(val start: Char, val end: Char){
    constructor(a: Int, b: Int) : this(a.toChar(), b.toChar())

    init {
        if (start > end){
            throw IllegalArgumentException("Range out of order in character class")
        }
    }

    val size: Int
        get() = end.code - start.code + 1
    operator fun contains(char: Char): Boolean = char in start..end

    operator fun component1(): Char = start
    operator fun component2(): Char = end
}