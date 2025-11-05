package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.slf4j.LoggerFactory
import kotlin.math.abs

ollision rate cr, Decomposition threshold dt, Synthesis threshold st, Initial kinetic energy ke, Kinetic energy loss rate kr,

/**
 * Chemical Reaction Optimization (CRO)
 *
 * Each molecule corresponds to a [WtsEvalIndividual] (a test suite). Potential energy (PE)
 * is defined as the negative of the combined fitness of the suite, so that minimization
 * semantics of the CRO equations align with EvoMaster's higher-is-better fitness.
 */
class CroAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    companion object {
        private val log = LoggerFactory.getLogger(CroAlgorithm::class.java)
    }

    private data class Molecule<T : Individual>(
        var suite: WtsEvalIndividual<T>,
        var kineticEnergy: Double,
        var numCollisions: Int
    )

    private val molecules: MutableList<Molecule<T>> = mutableListOf()

    // container is the global energy reservoir.
    // It collects kinetic energy lost in reactions and can be borrowed to enable otherwise infeasible decompositions, keeping total energy conserved.
    private var container: Double = 0.0


    // initialEnergy is the system’s starting total energy, used to enforce conservation.
    // It’s computed right after building the initial molecules as: buffer + Σ(PE + KE) over all molecules.
    private var initialEnergy: Double = 0.0

    override fun getType(): EMConfig.Algorithm = EMConfig.Algorithm.CRO

    override fun setupBeforeSearch() {
        // Reuse GA population initialization to sample and evaluate initial suites
        molecules.clear()
        container = 0.0

        // Initialize the underlying GA population to reuse sampling utilities
        super.setupBeforeSearch()

        // Convert GA population to CRO molecules with initial KE
        getViewOfPopulation().forEach { w ->
            molecules.add(Molecule(w.copy(), config.croInitialKineticEnergy, 0))
        }


        // initialEnergy is the system’s starting total energy, used to enforce conservation.
        initialEnergy = getCurrentEnergy()
    }

    override fun searchOnce() {
        
        if (randomness.nextDouble() > config.croMolecularCollisionRate || molecules.size == 1) {
            log.debug("an uni-molecular collision has occurred")
            // Uni-molecular collision
            val idx = randomness.nextInt(molecules.size)
            val m = molecules[idx]

            if (decompositionCheck(m)) {
                log.debug("a decomposition has occurred")
                val offsprings = decomposition(m)
                if (offsprings != null) {
                    molecules.removeAt(idx)
                    molecules.addAll(offsprings)
                }
            } else {
                log.debug("an on-wall ineffective collision has occurred")
                val newMolecule = onWallIneffectiveCollision(m)
                if (newMolecule != null) {
                    molecules[idx] = newMolecule
                }
            }
        } else {
            log.debug("an inter-molecular collision has occurred")
            // Inter-molecular collision
            val i1 = randomness.nextInt(molecules.size)
            var i2 = randomness.nextInt(molecules.size)
            while (i2 == i1) {
                // find a different molecule as an inter-molecular collision involves at least two molecules
                i2 = randomness.nextInt(molecules.size)
            }

            val m1 = molecules[i1]
            val m2 = molecules[i2]

            val shouldSynthesize = synthesisCheck(m1) && synthesisCheck(m2)
            if (shouldSynthesize) {
                log.debug("a synthesis has occurred")
                val offspring = synthesis(m1, m2)
                if (offspring != null) {
                    val low = minOf(i1, i2)
                    val high = maxOf(i1, i2)
                    molecules[low] = offspring
                    molecules.removeAt(high)
                }
            } else {
                log.debug("an inter-molecular ineffective collision has occurred")
                val pair = intermolecularIneffectiveCollision(m1, m2)
                if (pair != null) {
                    val (n1, n2) = pair
                    molecules[i1] = n1
                    molecules[i2] = n2
                }
            }
        }

        // Adjust container if external factors changed fitness values, to conserve energy
        val current = getCurrentEnergy()
        if (abs(current - initialEnergy) > 1e-9) {
            val delta = current - initialEnergy
            container -= delta
        }

        // Sanity check: conservation of energy must hold
        val energyAfter = getCurrentEnergy()
        if (!hasEnergyBeenConserved(energyAfter)) {
            throw RuntimeException("Current amount of energy (" + energyAfter
                    + ") in the system is not equal to its initial amount of energy (" + this.initialEnergy
                    + "). Conservation of energy has failed!")
        }
    }

    private fun potential(w: WtsEvalIndividual<T>): Double = -w.calculateCombinedFitness()

    private fun decompositionCheck(m: Molecule<T>): Boolean = m.numCollisions > config.croDecompositionThreshold

    private fun synthesisCheck(m: Molecule<T>): Boolean = m.kineticEnergy <= config.croSynthesisThreshold

    /**
     * An on-wall ineffective collision represents the situation when a molecule collides with a wall
     * of the container and then bounces away remaining in one single unit.
     * @param m the input molecule
     * @return a new molecule if the on-wall mutation is energetically affordable; null otherwise
     */
    private fun onWallIneffectiveCollision(m: Molecule<T>): Molecule<T>? {
        val pe = potential(m.suite)
        val ke = m.kineticEnergy
        val newM = m.copy(suite = m.suite.copy(), numCollisions = m.numCollisions + 1)

        // mutate and evaluate in-place
        mutate(newM.suite)

        val peNew = potential(newM.suite)
        val netOnWallEnergy = calculateNetOnWallEnergy(pe, ke, peNew)
        if (netOnWallEnergy < 0) {
            return null
        }
        applyOnWallEnergy(newM, netOnWallEnergy)
        log.debug("(" + pe + "," + ke + ") vs (" + peNew + "," + newM.kineticEnergy + ")\n" + "Container: " + container)
        return newM
    }

    private fun calculateNetOnWallEnergy(pe: Double, ke: Double, peNew: Double): Double {
        // Available energy minus new potential energy; negative => not affordable
        return (pe + ke) - peNew
    }

    private fun applyOnWallEnergy(newM: Molecule<T>, netEnergy: Double) {
        val retainedFraction = randomness.nextDouble(config.croKineticEnergyLossRate, 1.0)
        newM.kineticEnergy = netEnergy * retainedFraction
        container += netEnergy * (1.0 - retainedFraction)
    }

    /**
     * Decomposition: unary global operator. A molecule hits a wall and breaks into two molecules.
     *
     * @param m the parent molecule to decompose
     * @return a list with two offspring if the split is energetically affordable; null otherwise
     */
    private fun decomposition(m: Molecule<T>): List<Molecule<T>>? {

        // The idea of decomposition is to allow the system to explore other regions of the solution
        // space after enough local search by the ineffective collisions, similar to what mutation does
        // in evolutionary algorithms.

        val pe = potential(m.suite)
        val ke = m.kineticEnergy

        // Clone the molecules for the decomposition 
        val o1 = Molecule(m.suite.copy(), ke = 0.0, numCollisions = 0)
        val o2 = Molecule(m.suite.copy(), ke = 0.0, numCollisions = 0)

        // Mutate them
        mutate(o1.suite)
        mutate(o2.suite)

        val pe1 = potential(o1.suite)
        val pe2 = potential(o2.suite)

        // Compute the net energy balance for decomposition; if it's negative (deficit),
        // try to borrow from the container. Abort decomposition if borrowing cannot cover it.
        var netEnergyToDistribute = calculateNetEnergyToDistribute(pe, ke, pe1, pe2)
        if (netEnergyToDistribute < 0) {
            val covered = tryBorrowFromContainerToCoverDeficit(netEnergyToDistribute)
            if (covered == null) {
                m.numCollisions += 1
                return null
            }
            netEnergyToDistribute = covered
        }

        // distribute kinetic energy after decomposition
        updateMoleculesAfterDecomposition(o1, o2, netEnergyToDistribute)
        log.debug("(" + pe + "," + ke + ") vs (" + pe1 + "," + o1.kineticEnergy + ") --- (" + pe2 + "," + o2.kineticEnergy + ")\n" + "Container: " + container + " of " + initialEnergy)
        return listOf(o1, o2)
    }

    private fun calculateNetEnergyToDistribute(pe: Double, ke: Double, pe1: Double, pe2: Double): Double {
        // Balance after paying offspring potentials; positive => surplus, negative => deficit
        return (pe + ke) - (pe1 + pe2)
    }

    private fun tryBorrowFromContainerToCoverDeficit(deficit: Double): Double? {
        // Draw a random fraction (d1*d2) of the container to cover the deficit if possible
        val d1 = randomness.nextDouble()
        val d2 = randomness.nextDouble()
        val canBorrow = d1 * d2 * container
        if (deficit + canBorrow >= 0) {
            container *= (1.0 - d1 * d2)
            return deficit + canBorrow
        }
        return null
    }

    private fun updateMoleculesAfterDecomposition(m1: Molecule<T>, m2: Molecule<T>, netEnergyToDistribute: Double) {
        // distribute energy
        val energySplitFraction = randomness.nextDouble()
        m1.kineticEnergy = netEnergyToDistribute * energySplitFraction
        m2.kineticEnergy = netEnergyToDistribute * (1.0 - energySplitFraction)
        // reset number of collisions
        m1.numCollisions = 0
        m2.numCollisions = 0
    }

    /**
     * Inter-molecular ineffective collision takes place when two molecules collide and then bounce away
     * as two separate molecules.
     *
     * @param m1 the first input molecule 
     * @param m2 the second input molecule 
     * @return a Pair of updated molecules if the combined energy can pay the new potentials (accepted);
     *         null otherwise (collision rejected)
     */
    private fun intermolecularIneffectiveCollision(m1: Molecule<T>, m2: Molecule<T>): Pair<Molecule<T>, Molecule<T>>? {
        // Snapshot current energies
        val pe1 = potential(m1.suite)
        val ke1 = m1.kineticEnergy
        val pe2 = potential(m2.suite)
        val ke2 = m2.kineticEnergy

        // Clone, mark collision, and mutate both
        val n1 = m1.copy(suite = m1.suite.copy(), numCollisions = m1.numCollisions + 1)
        val n2 = m2.copy(suite = m2.suite.copy(), numCollisions = m2.numCollisions + 1)
        mutate(n1.suite)
        mutate(n2.suite)

        // Compute new potentials and the net energy available to distribute as KE
        val pe1n = potential(n1.suite)
        val pe2n = potential(n2.suite)
        val netInterCollisionEnergy = calculateNetInterCollisionEnergy(pe1, ke1, pe2, ke2, pe1n, pe2n)

        if (netInterCollisionEnergy >= 0) {
            distributeInterCollisionEnergy(n1, n2, netInterCollisionEnergy)
            log.debug("(" + pe1 + "," + ke1 + ") vs (" + pe1n + "," + n1.kineticEnergy + ")\n(" + pe2 + "," + ke2 + ") vs (" + pe2n + "," + n2.kineticEnergy + ")\n" + "Container: " + container)
            return Pair(n1, n2)
        }
        return null
    }

    private fun calculateNetInterCollisionEnergy(
        pe1: Double, ke1: Double,
        pe2: Double, ke2: Double,
        pe1n: Double, pe2n: Double
    ): Double {
        // Balance before vs after the collision; positive -> surplus to split as KE
        return (pe1 + pe2 + ke1 + ke2) - (pe1n + pe2n)
    }

    private fun distributeInterCollisionEnergy(n1: Molecule<T>, n2: Molecule<T>, netEnergy: Double) {
        val energySplitFraction = randomness.nextDouble()
        n1.kineticEnergy = netEnergy * energySplitFraction
        n2.kineticEnergy = netEnergy * (1.0 - energySplitFraction)
    }

    /**
     * Synthesis: does the opposite of decomposition. A synthesis happens when multiple (assume two)
     * molecules hit against each other and fuse together.
     * @param m1 the first input molecule
     * @param m2 the second input molecule
     * @return the fused offspring if energetically affordable; null otherwise
     */
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
        val fused = selectBetterOffspring(o1, o2)
        val peNew = potential(fused.suite)

        // Compute net energy available to assign as KE to the fused molecule
        val netSynthesisEnergy = calculateNetSynthesisEnergy(pe1, ke1, pe2, ke2, peNew)

        if (netSynthesisEnergy >= 0) {
            applySynthesisEnergy(fused, netSynthesisEnergy)
            log.debug("(" + pe1 + "," + ke1 + ") --- (" + pe2 + "," + ke2 + ") vs (" + peNew + "," + fused.kineticEnergy + ")\n" + "Container: " + container)
            return fused
        }

        // Not affordable: reject and increase collision counters
        m1.numCollisions += 1
        m2.numCollisions += 1
        return null
    }

    private fun selectBetterOffspring(o1: Molecule<T>, o2: Molecule<T>): Molecule<T> {
        return if (o1.suite.calculateCombinedFitness() >= o2.suite.calculateCombinedFitness()) o1 else o2
    }

    private fun calculateNetSynthesisEnergy(
        pe1: Double, ke1: Double,
        pe2: Double, ke2: Double,
        peNew: Double
    ): Double {
        // Total energy before fusion minus new potential energy
        val totalBefore = pe1 + pe2 + ke1 + ke2
        return totalBefore - peNew
    }

    private fun applySynthesisEnergy(fused: Molecule<T>, netEnergy: Double) {
        fused.kineticEnergy = netEnergy
        fused.numCollisions = 0
    }

    private fun getCurrentEnergy(): Double {
        var energy = container
        molecules.forEach { energy += potential(it.suite) + it.kineticEnergy }
        return energy
    }

    /**
     * Given a certain amount of energy, it checks whether energy has been conserved in the system.
     *
     * @param energy current measured total energy (container + sum of potentials and kinetic energies)
     * @return true if energy has been conserved in the system, false otherwise
     */
    private fun hasEnergyBeenConserved(energy: Double): Boolean {
        return abs(this.initialEnergy - energy) < 0.000000001
    }
}


