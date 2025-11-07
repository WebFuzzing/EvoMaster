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

    private val molecules: MutableList<Molecule<T>> = mutableListOf()
    private lateinit var reactor: CroReactor<T>

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

        // Initialize reactor with dependencies once (allow overriding via useReactor in tests)
        if (!this::reactor.isInitialized) {
            reactor = CroReactor(
                config,
                randomness,
                this::mutate,
                this::potential,
                this::xover
            )
        }

        // Convert GA population to CRO molecules with initial KE
        getViewOfPopulation().forEach { evaluatedSuite ->
            molecules.add(Molecule(evaluatedSuite.copy(), config.croInitialKineticEnergy, 0))
        }

        // initialEnergy is the system’s starting total energy, used to enforce conservation.
        initialEnergy = getCurrentEnergy()
    }

    /**
     * Allows tests or callers to override the reactor instance.
     * Must be called before [setupBeforeSearch].
     */
    fun useReactor(reactor: CroReactor<T>) { this.reactor = reactor }

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
                val energyCtx = CroReactor.EnergyContext(container)
                val decomposedOffspring = reactor.decomposition(
                    parent = selectedMolecule,
                    energy = energyCtx,
                )
                container = energyCtx.container
                if (decomposedOffspring != null) {
                    molecules.removeAt(moleculeIndex)
                    molecules.addAll(decomposedOffspring)
                }
            } else {
                val energyCtx = CroReactor.EnergyContext(container)
                val collidedMolecule = reactor.onWallIneffectiveCollision(
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
                val fusedOffspring = reactor.synthesis(
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
                val updatedPair = reactor.intermolecularIneffectiveCollision(
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

    private fun potential(evaluatedSuite: WtsEvalIndividual<T>): Double = -evaluatedSuite.calculateCombinedFitness()

    private fun decompositionCheck(molecule: Molecule<T>): Boolean = molecule.numCollisions > config.croDecompositionThreshold

    private fun synthesisCheck(molecule: Molecule<T>): Boolean = molecule.kineticEnergy <= config.croSynthesisThreshold

    private fun getCurrentEnergy(): Double {
        var energy = container
        molecules.forEach { molecule -> energy += potential(molecule.suite) + molecule.kineticEnergy }
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


