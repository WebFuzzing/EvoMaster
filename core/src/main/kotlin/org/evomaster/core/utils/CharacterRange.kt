package org.evomaster.core.utils

class CharacterRange private constructor(val start: Char, val end: Char){
    companion object {
        operator fun invoke(a: Char, b: Char): CharacterRange =
            if (a <= b) CharacterRange(a, b) else CharacterRange(b, a)

        operator fun invoke(a: Int, b: Int): CharacterRange =
            invoke(a.toChar(), b.toChar())
    }

    val size: Int
        get() = end.code - start.code + 1
    operator fun contains(char: Char): Boolean = char in start..end

    operator fun component1(): Char = start
    operator fun component2(): Char = end
}