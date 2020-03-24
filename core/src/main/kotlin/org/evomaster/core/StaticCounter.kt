package org.evomaster.core


/**
 * WARNING: here we have mutable static state.
 * In general, we should avoid doing something like this.
 * But we need to have way to create unique ids withount a dependency injection.
 * As this should have extremely low impact on the execution, forgetting to reset
 * it at each test case should no impact, apart from very, very special cases (eg,
 * when we test for determinism)
 */
class StaticCounter {

    companion object{

        private var counter = 0

        fun getAndIncrease() = counter++

        fun reset() {
            counter = 0
        }
    }
}