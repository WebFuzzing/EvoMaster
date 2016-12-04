package org.evomaster.core.search

import com.google.inject.Inject


abstract class Sampler<T> where T : Individual{

    @Inject
    protected lateinit var randomness : Randomness


    abstract fun sampleAtRandom() : T
}