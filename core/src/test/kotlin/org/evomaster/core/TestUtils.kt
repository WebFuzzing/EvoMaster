package org.evomaster.core

import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Randomness


object TestUtils {

    /**
     * handle initialization of individual which are created in unit and integration tests
     */
    fun doInitializeIndividualForTesting(individual: Individual, randomness: Randomness? = null){
        individual.doInitializeLocalId()
        individual.doInitialize(randomness)
    }

    /**
     * Unfortunately JUnit 5 does not handle flaky tests, and Maven is not upgraded yet.
     * See https://github.com/junit-team/junit5/issues/1558#issuecomment-414701182
     *
     * TODO: once that issue is fixed (if it will ever be fixed), then this method
     * will no longer be needed
     *
     * @param lambda
     */
    fun handleFlaky(lambda: () -> Unit) {

        val attempts = 3
        var error: Throwable? = null

        for (i in 0 until attempts) {

            try {
                lambda.invoke()
                return
            } catch (e: OutOfMemoryError) {
                throw e
            } catch (t: Throwable) {
                error = t
            }

        }

        throw error!!
    }
}