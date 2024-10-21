package org.evomaster.core

object Lazy {


    /**
     * As in Java, we want assertions evaluated only if -ea.
     * In Kotlin, they are always evaluated.
     *
     * TODO refactor if/when Kotlin will support lazy asserts
     * https://youtrack.jetbrains.com/issue/KT-22292
     */
    fun assert(predicate: () -> Boolean) {
        if (javaClass.desiredAssertionStatus()) {
            assert(predicate.invoke())
        }
    }

    /**
     * Compute the given lambda, and returns its result, but only
     * if assertions are activated
     */
    fun <T> compute(lambda: () -> T) : T? {
        if (javaClass.desiredAssertionStatus()) {
            return lambda.invoke()
        }
        return null
    }
}