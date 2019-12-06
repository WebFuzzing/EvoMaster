package org.evomaster.core.search.impact.individual

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Action
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2019-12-05
 */
class IndividualGeneImpactTest {

    @Test
    fun testGeneImpactUpdate(){
        val simulatedMutator = SimulatedMutator()

        val indAction1 = IndAction(
                listOf(StringGene("index1","index1"), StringGene("index2", "index2"))
        )
        val indAction2 = IndAction(
                listOf(StringGene("index1","index1"), StringGene("index2", "index2"))
        )

        val ind1 = Ind(mutableListOf(indAction1, indAction2))

        val fv1 = FitnessValue(ind1.seeActions().size.toDouble())

        val targets = mutableSetOf(1,2)
        fv1.updateTarget(id = 1, value = 0.1, actionIndex = 0)
        fv1.updateTarget(id = 2, value = 0.1, actionIndex = 1)

        val evi_ind1 = EvaluatedIndividual(
                fitness = fv1,
                individual = ind1,
                results = listOf(),
                enableTracking = true,
                trackOperator = simulatedMutator,
                enableImpact = true
        )
        assert(evi_ind1.getActionGeneImpact().size == 2)

        val ind2 = ind1.copy()
        val mutatedIndex = 1
        val mutatedGene = (ind2.seeActions()[mutatedIndex].seeGenes()[1] as StringGene)
        mutatedGene.value = "mutated_index2"
        val mutatedGeneId = ImpactUtils.generateGeneId(ind2, mutatedGene)

        val spec = MutatedGeneSpecification()
        spec.mutatedGenes.add(mutatedGene)
        spec.mutatedPosition.add(mutatedIndex)
        spec.setMutatedIndividual(ind2)

        val fv2 = fv1.copy()
        fv2.updateTarget(3, 0.5, mutatedIndex)
        fv2.updateTarget(2, 0.5, mutatedIndex)

        val evi_ind2 = EvaluatedIndividual(fv2, ind2, listOf())
        val impactTarget = mutableSetOf(3)
        val improvedTarget = mutableSetOf(3)

        fv2.isDifferent(
                other = fv1,
                targetSubset = targets,
                improved = improvedTarget,
                different = impactTarget,
                withExtra = false,
                strategy = EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE_SAME_N_ACTIONS,
                bloatControlForSecondaryObjective = false
        )
        assert(improvedTarget.size == 2)
        assert(impactTarget.size == 2)

        assert(improvedTarget.containsAll(setOf(2,3)))
        assert(impactTarget.containsAll(setOf(2,3)))

        val copyFilter = object : TraceableElementCopyFilter(EvaluatedIndividual.WITH_TRACK_WITH_CLONE_IMPACT){}

        val tracked_evi_ind2 = evi_ind1.next(
                trackOperator = simulatedMutator,
                next = evi_ind2,
                copyFilter = copyFilter
        )

        assert(tracked_evi_ind2 != null)

        tracked_evi_ind2!!.updateImpactOfGenes(
                inTrack = true,
                mutatedGenes = spec,
                improvedTargets = improvedTarget,
                impactTargets = impactTarget
        )

        assert(tracked_evi_ind2.getActionGeneImpact().size == 2)

        val ind1impact = evi_ind1.getActionGeneImpact()[mutatedIndex][mutatedGeneId]
        assert(ind1impact != null)

        assert(ind1impact!!.getTimesOfImpacts().containsKey(2))
        assert(ind1impact.getTimesOfImpacts().containsKey(3))


        val ind2impact = tracked_evi_ind2.getActionGeneImpact()[mutatedIndex][mutatedGeneId]
        val ind2impactdifAction = tracked_evi_ind2.getActionGeneImpact()[0][mutatedGeneId]

        assert(ind2impact != null)
        assert(ind1impact.shared == ind2impact!!.shared)

        assert(ind2impact.getTimesOfImpacts().containsKey(2))
        assert(ind2impact.getTimesOfImpacts().containsKey(3))

        assert(ind2impactdifAction != null)
        assert(ind2impactdifAction!!.getTimesOfImpacts().isEmpty())
        assert(ind2impactdifAction.getTimesToManipulate() == 0)

    }

    class SimulatedMutator :TrackOperator

    class Ind(val actions : MutableList<IndAction>) : Individual(){
        override fun copy(): Individual {
            return Ind(actions.map { it.copy() as IndAction }.toMutableList())
        }

        override fun seeGenes(filter: GeneFilter): List<out Gene> {
           return seeActions().flatMap(Action::seeGenes)
        }

        override fun size(): Int = seeActions().size

        override fun seeActions(): List<out Action> {
            return actions
        }

        override fun verifyInitializationActions(): Boolean {
            return true
        }

        override fun repairInitializationActions(randomness: Randomness) {}
    }

    class IndAction(private val genes : List<out Gene>) : Action{

        override fun getName(): String {
            return genes.map { it.name }.joinToString(",")
        }

        override fun seeGenes(): List<out Gene> {
            return genes
        }

        override fun copy(): Action {
            return IndAction(genes.map { it.copy() })
        }

        override fun shouldCountForFitnessEvaluations(): Boolean = true

    }
}