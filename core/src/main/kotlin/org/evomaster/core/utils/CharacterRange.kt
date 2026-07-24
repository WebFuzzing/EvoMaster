package org.evomaster.core.utils

class CharacterRange(val start: Char, val end: Char){
    constructor(start: Int, end: Int) : this(start.toChar(), end.toChar())
    constructor(char: Char) : this(char, char)

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