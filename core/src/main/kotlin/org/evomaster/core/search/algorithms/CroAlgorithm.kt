package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import kotlin.math.abs

/**
 * Chemical Reaction Optimization (CRO)
 *
 * Each molecule corresponds to a [WtsEvalIndividual] (a test suite).
 */
class CroAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    companion object {
        private const val ENERGY_TOLERANCE = 1e-9
    }

    private data class EnergyContext(var container: Double)

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
        getViewOfPopulation().forEach { evaluatedSuite ->
            molecules.add(Molecule(evaluatedSuite.copy(), config.croInitialKineticEnergy, 0))
        }

        // initialEnergy is the system’s starting total energy, used to enforce conservation.
        initialEnergy = getCurrentEnergy()
    }

    /**
     * Read-only snapshot of molecules for assertions in tests.
     */
    fun getMoleculesSnapshot(): List<Molecule<T>> = molecules.map { m ->
        Molecule(m.suite, m.kineticEnergy, m.numCollisions)
    }

    override fun searchOnce() {
        
        if (randomness.nextDouble() > config.croMolecularCollisionRate || molecules.size == 1) {
            // Uni-molecular collision
            val moleculeIndex = randomness.nextInt(molecules.size)
            val selectedMolecule = molecules[moleculeIndex]

            if (decompositionCheck(selectedMolecule)) {
                val energyCtx = EnergyContext(container)
                val decomposedOffspring = decomposition(
                    parent = selectedMolecule,
                    energy = energyCtx,
                )
                container = energyCtx.container
                if (decomposedOffspring != null) {
                    molecules.removeAt(moleculeIndex)
                    molecules.addAll(decomposedOffspring)
                }
            } else {
                val energyCtx = EnergyContext(container)
                val collidedMolecule = onWallIneffectiveCollision(
                    molecule = selectedMolecule,
                    energy = energyCtx,
                )
                container = energyCtx.container
                if (collidedMolecule != null) {
                    molecules[moleculeIndex] = collidedMolecule
                }
            }
        } else {
            // Inter-molecular collision
            val firstIndex = randomness.nextInt(molecules.size)
            var secondIndex = randomness.nextInt(molecules.size)
            while (secondIndex == firstIndex) {
                // find a different molecule as an inter-molecular collision involves at least two molecules
                secondIndex = randomness.nextInt(molecules.size)
            }

            val firstMolecule = molecules[firstIndex]
            val secondMolecule = molecules[secondIndex]

            val shouldSynthesize = synthesisCheck(firstMolecule) && synthesisCheck(secondMolecule)
            if (shouldSynthesize) {
                val fusedOffspring = synthesis(
                    first = firstMolecule,
                    second = secondMolecule,
                )
                if (fusedOffspring != null) {
                    val lowIndex = minOf(firstIndex, secondIndex)
                    val highIndex = maxOf(firstIndex, secondIndex)
                    molecules[lowIndex] = fusedOffspring
                    molecules.removeAt(highIndex)
                }
            } else {
                val updatedPair = intermolecularIneffectiveCollision(
                    first = firstMolecule,
                    second = secondMolecule,
                )
                if (updatedPair != null) {
                    val (updatedFirst, updatedSecond) = updatedPair
                    molecules[firstIndex] = updatedFirst
                    molecules[secondIndex] = updatedSecond
                }
            }
        }

        // Adjust container if external factors changed fitness values, to conserve energy
        val current = getCurrentEnergy()
        if (abs(current - initialEnergy) > ENERGY_TOLERANCE) {
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

    protected open fun computePotential(evaluatedSuite: WtsEvalIndividual<T>): Double = -evaluatedSuite.calculateCombinedFitness()

    protected open fun applyMutation(wts: WtsEvalIndividual<T>) {
        mutate(wts)
    }

    protected open fun applyCrossover(first: WtsEvalIndividual<T>, second: WtsEvalIndividual<T>) {
        xover(first, second)
    }

    private fun decompositionCheck(molecule: Molecule<T>): Boolean = molecule.numCollisions > config.croDecompositionThreshold

    private fun synthesisCheck(molecule: Molecule<T>): Boolean = molecule.kineticEnergy <= config.croSynthesisThreshold

    private fun getCurrentEnergy(): Double {
        var energy = container
        molecules.forEach { molecule -> energy += computePotential(molecule.suite) + molecule.kineticEnergy }
        return energy
    }

    /**
     * Given a certain amount of energy, it checks whether energy has been conserved in the system.
     *
     * @param energy current measured total energy (container + sum of potentials and kinetic energies)
     * @return true if energy has been conserved in the system, false otherwise
     */
    private fun hasEnergyBeenConserved(energy: Double): Boolean {
        return abs(this.initialEnergy - energy) < ENERGY_TOLERANCE
    }

    private fun computeEnergySurplus(totalBeforePotential: Double, totalBeforeKinetic: Double, totalAfterPotential: Double): Double =
        (totalBeforePotential + totalBeforeKinetic) - totalAfterPotential

    private fun tryBorrowFromContainerToCoverDeficit(deficit: Double, energy: EnergyContext): Double? {
        val fractionA = randomness.nextDouble()
        val fractionB = randomness.nextDouble()
        val borrowableAmount = fractionA * fractionB * energy.container
        return if (deficit + borrowableAmount >= 0) {
            energy.container *= (1.0 - fractionA * fractionB)
            deficit + borrowableAmount
        } else {
            null
        }
    }

    /**
     * Applies the uni-molecular on-wall ineffective collision:
     * mutate the molecule, keep it if energy surplus is non-negative,
     * split surplus between kinetic energy and the global container, and
     * increment collisions.
     */
    private fun onWallIneffectiveCollision(
        molecule: Molecule<T>,
        energy: EnergyContext,
    ): Molecule<T>? {
        val oldPotential = computePotential(molecule.suite)
        val oldKinetic = molecule.kineticEnergy
        val updated = molecule.copy(suite = molecule.suite.copy(), numCollisions = molecule.numCollisions + 1)

        applyMutation(updated.suite)

        val newPotential = computePotential(updated.suite)
        val netOnWallEnergy = computeEnergySurplus(oldPotential, oldKinetic, newPotential)
        if (netOnWallEnergy < 0) return null

        val retainedFraction = randomness.nextDouble(config.croKineticEnergyLossRate, 1.0)
        updated.kineticEnergy = netOnWallEnergy * retainedFraction
        energy.container += netOnWallEnergy * (1.0 - retainedFraction)
        return updated
    }

    /**
     * Performs decomposition of a molecule into two offspring.
     * Mutates two copies, accepts if total surplus (after optional borrowing
     * from the container) is non-negative, and distributes kinetic energy
     * and resets collisions.
     */
    private fun decomposition(
        parent: Molecule<T>,
        energy: EnergyContext,
    ): List<Molecule<T>>? {
        val parentPotential = computePotential(parent.suite)
        val parentKinetic = parent.kineticEnergy

        val first = Molecule(parent.suite.copy(), kineticEnergy = 0.0, numCollisions = 0)
        val second = Molecule(parent.suite.copy(), kineticEnergy = 0.0, numCollisions = 0)

        applyMutation(first.suite)
        applyMutation(second.suite)

        val firstPotential = computePotential(first.suite)
        val secondPotential = computePotential(second.suite)

        var netEnergyToDistribute = computeEnergySurplus(parentPotential, parentKinetic, firstPotential + secondPotential)
        if (netEnergyToDistribute < 0) {
            val covered = tryBorrowFromContainerToCoverDeficit(netEnergyToDistribute, energy)
            if (covered == null) {
                parent.numCollisions += 1
                return null
            }
            netEnergyToDistribute = covered
        }

        val energySplitFraction = randomness.nextDouble()
        first.kineticEnergy = netEnergyToDistribute * energySplitFraction
        second.kineticEnergy = netEnergyToDistribute * (1.0 - energySplitFraction)
        first.numCollisions = 0
        second.numCollisions = 0
        return listOf(first, second)
    }

    /**
     * Handles the inter-molecular ineffective collision, mutating both molecules
     * and accepting the new pair when energy is conserved or improved, while
     * splitting surplus kinetic energy between the offspring.
     */
    private fun intermolecularIneffectiveCollision(
        first: Molecule<T>,
        second: Molecule<T>,
    ): Pair<Molecule<T>, Molecule<T>>? {
        val firstPotential = computePotential(first.suite)
        val firstKinetic = first.kineticEnergy
        val secondPotential = computePotential(second.suite)
        val secondKinetic = second.kineticEnergy

        val updatedFirst = first.copy(suite = first.suite.copy(), numCollisions = first.numCollisions + 1)
        val updatedSecond = second.copy(suite = second.suite.copy(), numCollisions = second.numCollisions + 1)
        applyMutation(updatedFirst.suite)
        applyMutation(updatedSecond.suite)

        val updatedFirstPotential = computePotential(updatedFirst.suite)
        val updatedSecondPotential = computePotential(updatedSecond.suite)
        val netInterEnergy = computeEnergySurplus(
            firstPotential + secondPotential,
            firstKinetic + secondKinetic,
            updatedFirstPotential + updatedSecondPotential
        )

        if (netInterEnergy >= 0) {
            val energySplitFraction = randomness.nextDouble()
            updatedFirst.kineticEnergy = netInterEnergy * energySplitFraction
            updatedSecond.kineticEnergy = netInterEnergy * (1.0 - energySplitFraction)
            return Pair(updatedFirst, updatedSecond)
        }
        return null
    }

    /**
     * Executes synthesis between two molecules: crossover their suites, keep
     * the fitter fused offspring if energy allows, and reset the resulting
     * molecule’s collisions; otherwise increment collisions on the parents.
     */
    private fun synthesis(
        first: Molecule<T>,
        second: Molecule<T>,
    ): Molecule<T>? {
        val firstPotential = computePotential(first.suite)
        val firstKinetic = first.kineticEnergy
        val secondPotential = computePotential(second.suite)
        val secondKinetic = second.kineticEnergy

        val firstOffspring = Molecule(first.suite.copy(), 0.0, 0)
        val secondOffspring = Molecule(second.suite.copy(), 0.0, 0)

        applyCrossover(firstOffspring.suite, secondOffspring.suite)

        val fused = if (firstOffspring.suite.calculateCombinedFitness() >= secondOffspring.suite.calculateCombinedFitness()) firstOffspring else secondOffspring
        val fusedPotential = computePotential(fused.suite)

        val netSynthesisEnergy = computeEnergySurplus(
            firstPotential + secondPotential,
            firstKinetic + secondKinetic,
            fusedPotential
        )

        if (netSynthesisEnergy >= 0) {
            fused.kineticEnergy = netSynthesisEnergy
            fused.numCollisions = 0
            return fused
        }

        first.numCollisions += 1
        second.numCollisions += 1
        return null
    }
}


