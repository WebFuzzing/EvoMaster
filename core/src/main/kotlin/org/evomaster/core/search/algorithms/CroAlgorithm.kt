package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import kotlin.math.abs

/**
 * Chemical Reaction Optimization (CRO)
 *
 * Each molecule corresponds to a [WtsEvalIndividual] (a test suite). Potential energy (PE)
 * is defined as the negative of the combined fitness of the suite, so that minimization
 * semantics of the CRO equations align with EvoMaster's higher-is-better fitness.
 */
class CroAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    private data class Molecule<T : Individual>(
        var suite: WtsEvalIndividual<T>,
        var kineticEnergy: Double,
        var numCollisions: Int
    )

    private val molecules: MutableList<Molecule<T>> = mutableListOf()

    private var buffer: Double = 0.0
    private var initialEnergy: Double = 0.0

    override fun getType(): EMConfig.Algorithm = EMConfig.Algorithm.CRO

    override fun setupBeforeSearch() {
        // Reuse GA population initialization to sample and evaluate initial suites
        molecules.clear()
        buffer = 0.0

        // Initialize the underlying GA population to reuse sampling utilities
        super.setupBeforeSearch()

        // Convert GA population to CRO molecules with initial KE
        getViewOfPopulation().forEach { w ->
            molecules.add(Molecule(w.copy(), config.croInitialKineticEnergy, 0))
        }

        initialEnergy = getCurrentEnergy()
    }

    override fun searchOnce() {
        if (molecules.isEmpty()) return

        val binary = randomness.nextDouble() <= config.croMolecularCollisionRate && molecules.size > 1

        if (!binary) {
            // Uni-molecular collision
            val idx = randomness.nextInt(molecules.size)
            val m = molecules[idx]

            if (decompositionCheck(m)) {
                decomposition(m)?.let { offsprings ->
                    molecules.removeAt(idx)
                    molecules.addAll(offsprings)
                }
            } else {
                onWallIneffectiveCollision(m)?.let { nm ->
                    molecules[idx] = nm
                }
            }
        } else {
            // Inter-molecular collision
            val i1 = randomness.nextInt(molecules.size)
            var i2 = randomness.nextInt(molecules.size)
            while (i2 == i1) i2 = randomness.nextInt(molecules.size)

            val m1 = molecules[i1]
            val m2 = molecules[i2]

            if (synthesisCheck(m1) && synthesisCheck(m2)) {
                synthesis(m1, m2)?.let { off ->
                    val low = minOf(i1, i2)
                    val high = maxOf(i1, i2)
                    molecules[low] = off
                    molecules.removeAt(high)
                }
            } else {
                intermolecularIneffectiveCollision(m1, m2)?.let { (n1, n2) ->
                    molecules[i1] = n1
                    molecules[i2] = n2
                }
            }
        }

        // Adjust buffer if external factors changed fitness values, to conserve energy
        val current = getCurrentEnergy()
        if (abs(current - initialEnergy) > 1e-9) {
            val delta = current - initialEnergy
            buffer -= delta
        }
    }

    private fun potential(w: WtsEvalIndividual<T>): Double = -w.calculateCombinedFitness()

    private fun decompositionCheck(m: Molecule<T>): Boolean = m.numCollisions > config.croDecompositionThreshold

    private fun synthesisCheck(m: Molecule<T>): Boolean = m.kineticEnergy <= config.croSynthesisThreshold

    private fun onWallIneffectiveCollision(m: Molecule<T>): Molecule<T>? {
        val pe = potential(m.suite)
        val ke = m.kineticEnergy
        val newM = m.copy(suite = m.suite.copy(), numCollisions = m.numCollisions + 1)

        // mutate and evaluate in-place
        mutate(newM.suite)

        val peNew = potential(newM.suite)
        return if (pe + ke >= peNew) {
            val a = randomness.nextDouble(config.croKineticEnergyLossRate, 1.0)
            val surplus = (pe - peNew + ke)
            newM.kineticEnergy = surplus * a
            buffer += surplus * (1.0 - a)
            newM
        } else null
    }

    private fun decomposition(m: Molecule<T>): List<Molecule<T>>? {
        val pe = potential(m.suite)
        val ke = m.kineticEnergy

        val o1 = Molecule(m.suite.copy(), ke = 0.0, numCollisions = 0)
        val o2 = Molecule(m.suite.copy(), ke = 0.0, numCollisions = 0)

        mutate(o1.suite)
        mutate(o2.suite)

        val pe1 = potential(o1.suite)
        val pe2 = potential(o2.suite)

        var eDec = (pe + ke) - (pe1 + pe2)
        if (eDec < 0) {
            val d1 = randomness.nextDouble()
            val d2 = randomness.nextDouble()
            val canBorrow = d1 * d2 * buffer
            if (eDec + canBorrow >= 0) {
                buffer *= (1.0 - d1 * d2)
                eDec += canBorrow
            } else {
                m.numCollisions += 1
                return null
            }
        }

        // distribute kinetic energy and reset collisions
        val d3 = randomness.nextDouble()
        o1.kineticEnergy = eDec * d3
        o2.kineticEnergy = eDec * (1.0 - d3)
        o1.numCollisions = 0
        o2.numCollisions = 0
        return listOf(o1, o2)
    }

    private fun intermolecularIneffectiveCollision(m1: Molecule<T>, m2: Molecule<T>): Pair<Molecule<T>, Molecule<T>>? {
        val pe1 = potential(m1.suite)
        val ke1 = m1.kineticEnergy
        val pe2 = potential(m2.suite)
        val ke2 = m2.kineticEnergy

        val n1 = m1.copy(suite = m1.suite.copy(), numCollisions = m1.numCollisions + 1)
        val n2 = m2.copy(suite = m2.suite.copy(), numCollisions = m2.numCollisions + 1)

        mutate(n1.suite)
        mutate(n2.suite)

        val pe1n = potential(n1.suite)
        val pe2n = potential(n2.suite)

        val eInter = (pe1 + pe2 + ke1 + ke2) - (pe1n + pe2n)
        return if (eInter >= 0) {
            val d4 = randomness.nextDouble()
            n1.kineticEnergy = eInter * d4
            n2.kineticEnergy = eInter * (1.0 - d4)
            Pair(n1, n2)
        } else null
    }

    private fun synthesis(m1: Molecule<T>, m2: Molecule<T>): Molecule<T>? {
        val pe1 = potential(m1.suite)
        val ke1 = m1.kineticEnergy
        val pe2 = potential(m2.suite)
        val ke2 = m2.kineticEnergy

        val o1 = Molecule(m1.suite.copy(), 0.0, 0)
        val o2 = Molecule(m2.suite.copy(), 0.0, 0)

        // crossover suites
        xover(o1.suite, o2.suite)

        // choose the better offspring (higher combined fitness => lower potential energy)
        val best = if (o1.suite.calculateCombinedFitness() >= o2.suite.calculateCombinedFitness()) o1 else o2
        val pe = potential(best.suite)
        return if (pe1 + pe2 + ke1 + ke2 >= pe) {
            best.kineticEnergy = (pe1 + pe2 + ke1 + ke2) - pe
            best.numCollisions = 0
            best
        } else {
            m1.numCollisions += 1
            m2.numCollisions += 1
            null
        }
    }

    private fun getCurrentEnergy(): Double {
        var energy = buffer
        molecules.forEach { energy += potential(it.suite) + it.kineticEnergy }
        return energy
    }
}


