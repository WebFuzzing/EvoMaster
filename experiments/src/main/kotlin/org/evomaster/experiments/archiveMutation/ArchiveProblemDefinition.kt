package org.evomaster.experiments.archiveMutation

import com.google.inject.Inject
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Randomness

/**
 * created by manzh on 2019-09-23
 */
abstract class ArchiveProblemDefinition<T> where T : Individual{
    @Inject
    lateinit var randomness : Randomness

    var nTargets = 1

    var nGenes = 1

    /**
     * return
     * key is target
     * value is fitness value
     */
    abstract fun distance(individual : T) : Map<Int, Double>

    fun y(value :String) = "hello"+value.map { c -> if (c.toInt() < Char.MAX_VALUE.toInt()-32) (c.toInt() + 32).toChar() else (c.toInt() -32).toChar() }.joinToString()
}