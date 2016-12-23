package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Randomness


abstract class Sampler<T> where T : Individual {

    @Inject
    protected lateinit var randomness : Randomness


    abstract fun sampleAtRandom() : T
}