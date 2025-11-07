package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CroReactorTest {

    private fun <T : Individual> newReactor(
        config: EMConfig = EMConfig(),
        randomness: Randomness = Randomness(),
        mutate: (WtsEvalIndividual<T>) -> Unit = {},
        potential: (WtsEvalIndividual<T>) -> Double,
        xover: (WtsEvalIndividual<T>, WtsEvalIndividual<T>) -> Unit = { _, _ -> }
    ): CroReactor<T> = CroReactor(config, randomness, mutate, potential, xover)

    @Test
    fun computeEnergySurplus_positive_zero_negative() {
        val reactor = newReactor<Individual>(potential = { 0.0 })

        // positive surplus (affordable)
        assertEquals(5.0, reactor.computeEnergySurplus(10.0, 0.0, 5.0), 1e-9)

        // zero surplus (just affordable)
        assertEquals(0.0, reactor.computeEnergySurplus(10.0, 0.0, 10.0), 1e-9)

        // negative surplus (deficit)
        assertEquals(-2.5, reactor.computeEnergySurplus(7.5, 0.0, 10.0), 1e-9)
    }

    @Test
    fun decomposition_borrowFailsWhenContainerZero_incrementsCollisions() {
        // parent 10 → offspring 20,20 → deficit -30; container=0 → cannot borrow
        val potentials = ArrayDeque(listOf(10.0, 20.0, 20.0)) // parent -> first -> second
        val potentialFn: (WtsEvalIndividual<Individual>) -> Double = { _ -> potentials.removeFirst() }
        val randomness = Randomness().apply { updateSeed(123) }
        val reactor = newReactor(config = EMConfig(), randomness = randomness, mutate = {}, potential = potentialFn)

        val parent = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 0.0, numCollisions = 0)
        val energy = CroReactor.EnergyContext(container = 0.0)

        val result = reactor.decomposition(parent, energy)
        assertNull(result)
        assertEquals(1, parent.numCollisions)
        assertEquals(0.0, energy.container, 1e-9)
    }

    @Test
    fun decomposition_borrowSucceedsWhenContainerLarge_affordableAndContainerDecreases() {
        // parent 10 → offspring 20,20 → deficit -30; container large → borrowing covers
        val potentials = ArrayDeque(listOf(10.0, 20.0, 20.0))
        val potentialFn: (WtsEvalIndividual<Individual>) -> Double = { _ -> potentials.removeFirst() }
        val randomness = Randomness().apply { updateSeed(123) }
        val reactor = newReactor(config = EMConfig(), randomness = randomness, mutate = {}, potential = potentialFn)

        val parent = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 0.0, numCollisions = 0)
        val initialContainer = 1_000_000.0
        val energy = CroReactor.EnergyContext(container = initialContainer)

        val result = reactor.decomposition(parent, energy)
        assertNotNull(result)
        val (first, second) = result!!
        // KE distributed is non-negative
        assertTrue(first.kineticEnergy >= 0.0 && second.kineticEnergy >= 0.0)
        // container decreased due to borrowing
        assertTrue(energy.container < initialContainer)
        // parent collisions unchanged on success
        assertEquals(0, parent.numCollisions)
    }

    @Test
    fun onWallIneffectiveCollision_negativeNetEnergy_returnsNullAndNoStateChange() {
        // oldPotential=10, oldKE=0, newPotential=20 => netOnWallEnergy = -10 (reject)
        val potentials = ArrayDeque(listOf(10.0, 20.0)) // old -> new
        val potentialFn: (WtsEvalIndividual<Individual>) -> Double = { _ -> potentials.removeFirst() }
        val randomness = Randomness().apply { updateSeed(321) }
        val reactor = newReactor(config = EMConfig(), randomness = randomness, mutate = {}, potential = potentialFn)

        val parent = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 0.0, numCollisions = 0)
        val initialContainer = 100.0
        val energy = CroReactor.EnergyContext(container = initialContainer)

        val result = reactor.onWallIneffectiveCollision(parent, energy)
        assertNull(result)
        // original molecule unchanged
        assertEquals(0, parent.numCollisions)
        assertEquals(0.0, parent.kineticEnergy, 1e-9)
        // container unchanged
        assertEquals(initialContainer, energy.container, 1e-9)
    }

    @Test
    fun onWallIneffectiveCollision_positiveNetEnergy_updatesKineticAndContainer() {
        // oldPotential=10, oldKE=5, newPotential=12 => netOnWallEnergy = 3 (accept)
        val config = EMConfig().apply { croKineticEnergyLossRate = 0.5 }
        val potentials = ArrayDeque(listOf(10.0, 12.0)) // old -> new
        val potentialFn: (WtsEvalIndividual<Individual>) -> Double = { _ -> potentials.removeFirst() }
        val seed = 123L
        val randomness = Randomness().apply { updateSeed(seed) }
        val reactor = newReactor(config = config, randomness = randomness, mutate = {}, potential = potentialFn)

        val parent = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 5.0, numCollisions = 0)
        val initialContainer = 7.0
        val energy = CroReactor.EnergyContext(container = initialContainer)

        val expectedNet = 3.0

        val updated = reactor.onWallIneffectiveCollision(parent, energy)
        assertNotNull(updated)
        val m = updated!!
        
        // updated has incremented collisions
        assertEquals(1, m.numCollisions)

        // KE and container were updated correctly
        assertTrue(m.kineticEnergy >= 0.0)
        assertTrue(energy.container >= initialContainer)
        assertTrue(m.kineticEnergy <= expectedNet)
        assertTrue((energy.container - initialContainer) <= expectedNet)

        // Conservation of energy: KE gained + container increase == net energy
        val deltaContainer = energy.container - initialContainer
        assertEquals(expectedNet, m.kineticEnergy + deltaContainer, 1e-9)

        // original molecule not mutated
        assertEquals(0, parent.numCollisions)
        assertEquals(5.0, parent.kineticEnergy, 1e-9)
    }

    @Test
    fun intermolecularIneffectiveCollision_negativeNetEnergy_returnsNullAndOriginalsUnchanged() {
        // first old=10, second old=10; updatedFirst=25, updatedSecond=0 => net = (10+10) - (25+0) = -5
        val potentials = ArrayDeque(listOf(10.0, 10.0, 25.0, 0.0)) // first, second, updatedFirst, updatedSecond
        val potentialFn: (WtsEvalIndividual<Individual>) -> Double = { _ -> potentials.removeFirst() }
        val randomness = Randomness().apply { updateSeed(111) }
        val reactor = newReactor(config = EMConfig(), randomness = randomness, mutate = {}, potential = potentialFn)

        val first = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 0.0, numCollisions = 0)
        val second = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 0.0, numCollisions = 0)

        val result = reactor.intermolecularIneffectiveCollision(first, second)
        assertNull(result)
        // originals unchanged
        assertEquals(0, first.numCollisions)
        assertEquals(0, second.numCollisions)
        assertEquals(0.0, first.kineticEnergy, 1e-9)
        assertEquals(0.0, second.kineticEnergy, 1e-9)
    }

    @Test
    fun intermolecularIneffectiveCollision_positiveNetEnergy_updatesKineticSplitAndIncrementsCollisions() {
        // first old=10, second old=15; updatedFirst=20, updatedSecond=0 => net = (10+15) - (20+0) = 5
        val potentials = ArrayDeque(listOf(10.0, 15.0, 20.0, 0.0))
        val potentialFn: (WtsEvalIndividual<Individual>) -> Double = { _ -> potentials.removeFirst() }
        val seed = 222L
        val randomness = Randomness().apply { updateSeed(seed) }
        val reactor = newReactor(config = EMConfig(), randomness = randomness, mutate = {}, potential = potentialFn)

        val first = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 0.0, numCollisions = 0)
        val second = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 0.0, numCollisions = 0)

        val updatedPair = reactor.intermolecularIneffectiveCollision(first, second)
        assertNotNull(updatedPair)
        val (u1, u2) = updatedPair!!
        // collisions incremented on the returned copies; originals unchanged
        assertEquals(1, u1.numCollisions)
        assertEquals(1, u2.numCollisions)
        assertEquals(0, first.numCollisions)
        assertEquals(0, second.numCollisions)
        // KE non-negative and bounded by net; conservation holds
        val net = 5.0
        assertTrue(u1.kineticEnergy >= 0.0)
        assertTrue(u2.kineticEnergy >= 0.0)
        assertTrue(u1.kineticEnergy <= net)
        assertTrue(u2.kineticEnergy <= net)
        assertEquals(net, u1.kineticEnergy + u2.kineticEnergy, 1e-9)
    }

    @Test
    fun synthesis_negativeNetEnergy_returnsNullAndParentsCollisionsIncrement() {
        // first: pot=10, ke=0; second: pot=10, ke=0; fused pot=25 => net = (10+10) - 25 = -5
        val potentials = ArrayDeque(listOf(10.0, 10.0, 25.0)) // first, second, fused
        val potentialFn: (WtsEvalIndividual<Individual>) -> Double = { _ -> potentials.removeFirst() }
        val randomness = Randomness().apply { updateSeed(333) }
        val reactor = newReactor(config = EMConfig(), randomness = randomness, mutate = {}, potential = potentialFn)

        val first = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 0.0, numCollisions = 0)
        val second = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 0.0, numCollisions = 0)

        val result = reactor.synthesis(first, second)
        assertNull(result)
        // parents collisions incremented on rejection
        assertEquals(1, first.numCollisions)
        assertEquals(1, second.numCollisions)
    }

    @Test
    fun synthesis_positiveNetEnergy_returnsFusedWithKineticEnergyAndParentsUnchanged() {
        // first: pot=10, ke=2; second: pot=10, ke=1; fused pot=20 => net = (10+10) + (2+1) - 20 = 3
        val potentials = ArrayDeque(listOf(10.0, 10.0, 20.0))
        val potentialFn: (WtsEvalIndividual<Individual>) -> Double = { _ -> potentials.removeFirst() }
        val randomness = Randomness().apply { updateSeed(444) }
        val reactor = newReactor(config = EMConfig(), randomness = randomness, mutate = {}, potential = potentialFn)

        val first = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 2.0, numCollisions = 0)
        val second = Molecule(WtsEvalIndividual<Individual>(mutableListOf()), kineticEnergy = 1.0, numCollisions = 0)

        val fused = reactor.synthesis(first, second)
        assertNotNull(fused)
        val m = fused!!
        // kinetic energy equals exact net; collisions reset to 0
        assertEquals(3.0, m.kineticEnergy, 1e-9)
        assertEquals(0, m.numCollisions)
        // parents unchanged on success
        assertEquals(0, first.numCollisions)
        assertEquals(0, second.numCollisions)
        assertEquals(2.0, first.kineticEnergy, 1e-9)
        assertEquals(1.0, second.kineticEnergy, 1e-9)
    }
}


