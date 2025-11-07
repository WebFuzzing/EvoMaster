package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Randomness

class CroReactor<T>(
    private val config: EMConfig,
    private val randomness: Randomness,
    private val mutate: (WtsEvalIndividual<T>) -> Unit,
    private val potential: (WtsEvalIndividual<T>) -> Double,
    private val xover: (WtsEvalIndividual<T>, WtsEvalIndividual<T>) -> Unit
) where T : Individual {

    data class EnergyContext(var container: Double)

    fun computeEnergySurplus(totalBeforePotential: Double, totalBeforeKinetic: Double, totalAfterPotential: Double): Double =
        (totalBeforePotential + totalBeforeKinetic) - totalAfterPotential

    private fun tryBorrowFromContainerToCoverDeficit(deficit: Double, energy: EnergyContext, randomness: Randomness): Double? {
        val fractionA = randomness.nextDouble()
        val fractionB = randomness.nextDouble()
        val borrowableAmount = fractionA * fractionB * energy.container
        if (deficit + borrowableAmount >= 0) {
            energy.container *= (1.0 - fractionA * fractionB)
            return deficit + borrowableAmount
        }
        return null
    }

    fun onWallIneffectiveCollision(
        molecule: Molecule<T>,
        energy: EnergyContext,
    ): Molecule<T>? {
        val oldPotential = potential(molecule.suite)
        val oldKinetic = molecule.kineticEnergy
        val updated = molecule.copy(suite = molecule.suite.copy(), numCollisions = molecule.numCollisions + 1)

        mutate(updated.suite)

        val newPotential = potential(updated.suite)
        val netOnWallEnergy = computeEnergySurplus(oldPotential, oldKinetic, newPotential)
        if (netOnWallEnergy < 0) return null

        val retainedFraction = randomness.nextDouble(config.croKineticEnergyLossRate, 1.0)
        updated.kineticEnergy = netOnWallEnergy * retainedFraction
        energy.container += netOnWallEnergy * (1.0 - retainedFraction)
        return updated
    }

    fun decomposition(
        parent: Molecule<T>,
        energy: EnergyContext,
    ): List<Molecule<T>>? {
        val parentPotential = potential(parent.suite)
        val parentKinetic = parent.kineticEnergy

        val first = Molecule(parent.suite.copy(), ke = 0.0, numCollisions = 0)
        val second = Molecule(parent.suite.copy(), ke = 0.0, numCollisions = 0)

        mutate(first.suite)
        mutate(second.suite)

        val firstPotential = potential(first.suite)
        val secondPotential = potential(second.suite)

        var netEnergyToDistribute = computeEnergySurplus(parentPotential, parentKinetic, firstPotential + secondPotential)
        if (netEnergyToDistribute < 0) {
            val covered = tryBorrowFromContainerToCoverDeficit(netEnergyToDistribute, energy, randomness)
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

    fun intermolecularIneffectiveCollision(
        first: Molecule<T>,
        second: Molecule<T>,
    ): Pair<Molecule<T>, Molecule<T>>? {
        val firstPotential = potential(first.suite)
        val firstKinetic = first.kineticEnergy
        val secondPotential = potential(second.suite)
        val secondKinetic = second.kineticEnergy

        val updatedFirst = first.copy(suite = first.suite.copy(), numCollisions = first.numCollisions + 1)
        val updatedSecond = second.copy(suite = second.suite.copy(), numCollisions = second.numCollisions + 1)
        mutate(updatedFirst.suite)
        mutate(updatedSecond.suite)

        val updatedFirstPotential = potential(updatedFirst.suite)
        val updatedSecondPotential = potential(updatedSecond.suite)
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

    fun synthesis(
        first: Molecule<T>,
        second: Molecule<T>,
    ): Molecule<T>? {
        val firstPotential = potential(first.suite)
        val firstKinetic = first.kineticEnergy
        val secondPotential = potential(second.suite)
        val secondKinetic = second.kineticEnergy

        val firstOffspring = Molecule(first.suite.copy(), 0.0, 0)
        val secondOffspring = Molecule(second.suite.copy(), 0.0, 0)

        xover(firstOffspring.suite, secondOffspring.suite)

        val fused = if (firstOffspring.suite.calculateCombinedFitness() >= secondOffspring.suite.calculateCombinedFitness()) firstOffspring else secondOffspring
        val fusedPotential = potential(fused.suite)

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


