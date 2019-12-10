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

        val evi_ind1 = simulatedMutator.getFakeEvaluatedIndividual()

        assert(evi_ind1.getSizeOfActionImpact(fromInitialization = false) == 2)
        val mutatedIndex = 1
        val spec = MutatedGeneSpecification()

        val evi_ind2 = simulatedMutator.fakeMutator(evi_ind1, mutatedIndex, spec)

        assert(spec.mutatedIndividual != null)
        assert(spec.mutatedGenes.size == 1)
        val mutatedGeneId = ImpactUtils.generateGeneId(spec.mutatedIndividual!!, spec.mutatedGenes.first())

        val impactTarget = mutableSetOf(simulatedMutator.getNewTarget())
        val improvedTarget = mutableSetOf(simulatedMutator.getNewTarget())

        evi_ind2.fitness.isDifferent(
                other = evi_ind1.fitness,
                targetSubset = simulatedMutator.getInitialTargets(),
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

        assert(tracked_evi_ind2.getSizeOfActionImpact(false) == 2)
        val evi_ind1impactInfo = evi_ind1.getImpactByAction(mutatedIndex, false)
        assert(evi_ind1impactInfo!= null)
        val ind1impact = evi_ind1impactInfo!![mutatedGeneId]
        assert(ind1impact != null)

        assert(ind1impact!!.getTimesOfImpacts().containsKey(2))
        assert(ind1impact.getTimesOfImpacts().containsKey(3))

        val tracked_evi_ind2impactInfo = tracked_evi_ind2.getImpactByAction(mutatedIndex, false)
        assert(tracked_evi_ind2impactInfo!=null)
        val ind2impact = tracked_evi_ind2impactInfo!![mutatedGeneId]

        val tracked_evi_ind2impactInfo_otheer = tracked_evi_ind2.getImpactByAction(0, false)
        assert(tracked_evi_ind2impactInfo_otheer!=null)
        val ind2impactdifAction = tracked_evi_ind2impactInfo_otheer!![mutatedGeneId]

        assert(ind2impact != null)
        assert(ind1impact.shared == ind2impact!!.shared)

        assert(ind2impact.getTimesOfImpacts().containsKey(2))
        assert(ind2impact.getTimesOfImpacts().containsKey(3))

        assert(ind2impactdifAction != null)
        assert(ind2impactdifAction!!.getTimesOfImpacts().isEmpty())
        assert(ind2impactdifAction.getTimesToManipulate() == 0)
    }

    @Test
    fun testGeneImpactUpdateByStructureMutator(){
        val simulatedMutator = SimulatedMutator()

        val evi_ind1 = simulatedMutator.getFakeEvaluatedIndividual()

        assert(evi_ind1.getSizeOfActionImpact(false) == 2)
        val mutatedIndex = 1
        val spec = MutatedGeneSpecification()

        val evi_ind2 = simulatedMutator.fakeStructureMutator(evi_ind1, mutatedIndex, true, spec)

        val impactTarget = mutableSetOf(simulatedMutator.getNewTarget())
        val improvedTarget = mutableSetOf(simulatedMutator.getNewTarget())

        evi_ind2.fitness.isDifferent(
                other = evi_ind1.fitness,
                targetSubset = simulatedMutator.getInitialTargets(),
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
                trackOperator = simulatedMutator, next = evi_ind2, copyFilter = copyFilter
        )

        assert(tracked_evi_ind2 != null)

        tracked_evi_ind2!!.updateImpactOfGenes(
                inTrack = true,
                mutatedGenes = spec,
                improvedTargets = improvedTarget,
                impactTargets = impactTarget
        )

        assert(tracked_evi_ind2.getSizeOfActionImpact(false) == 1)


    }
    class SimulatedMutator :TrackOperator{

        fun fakeMutator(evaluatedIndividual: EvaluatedIndividual<Ind>, mutatedIndex : Int, mutatedGeneSpecification: MutatedGeneSpecification) : EvaluatedIndividual<Ind>{
            val ind2 = evaluatedIndividual.individual.copy() as Ind

            val mutatedGene = (ind2.seeActions()[mutatedIndex].seeGenes()[1] as StringGene)
            mutatedGene.value = "mutated_index2"

            mutatedGeneSpecification.mutatedGenes.add(mutatedGene)
            mutatedGeneSpecification.mutatedPosition.add(mutatedIndex)
            mutatedGeneSpecification.setMutatedIndividual(ind2)

            val fv2 = evaluatedIndividual.fitness.copy()
            fv2.updateTarget(getNewTarget(), 0.5, mutatedIndex)
            fv2.updateTarget(getExistingImprovedTarget(), 0.5, mutatedIndex)

            return EvaluatedIndividual(fv2, ind2, listOf())
        }

        fun fakeStructureMutator(evaluatedIndividual: EvaluatedIndividual<Ind>, mutatedIndex : Int, remove: Boolean, mutatedGeneSpecification: MutatedGeneSpecification) : EvaluatedIndividual<Ind>{
            val ind2 = evaluatedIndividual.individual.copy() as Ind
            if (ind2.seeActions().size == 1 && remove)
                throw IllegalArgumentException("action cannot be removed since there is only one action")
            if (remove){
                val genes = ind2.seeActions()[mutatedIndex].seeGenes()
                mutatedGeneSpecification.removedGene.addAll(genes)
                genes.forEach { _ ->
                    mutatedGeneSpecification.mutatedPosition.add(mutatedIndex)
                }
                ind2.actions.removeAt(mutatedIndex)

            }else{
                val action = IndAction.getIndAction()
                action.seeGenes().forEach { _ ->
                    mutatedGeneSpecification.mutatedPosition.add(mutatedIndex)
                }
                ind2.actions.add(mutatedIndex, action)
            }

            mutatedGeneSpecification.setMutatedIndividual(ind2)

            val fv2 = evaluatedIndividual.fitness.copy()
            val actionIndex = if (remove){
                if (mutatedIndex - 1 >=0)
                    mutatedIndex - 1
                else
                    mutatedIndex + 1
            }else{
                mutatedIndex
            }

            fv2.updateTarget(getNewTarget(), 0.5, actionIndex)
            fv2.updateTarget(getExistingImprovedTarget(), 0.5, actionIndex)
            return EvaluatedIndividual(fv2, ind2, listOf())
        }

        fun getNewTarget() = 3
        fun getExistingImprovedTarget() = 2

        fun getInitialTargets() = setOf(1,2)

        fun getFakeEvaluatedIndividual() : EvaluatedIndividual<Ind>{
            val ind1 = Ind.getInd()
            val fv1 = FitnessValue(ind1.seeActions().size.toDouble())

            fv1.updateTarget(id = 1, value = 0.1, actionIndex = 0)
            fv1.updateTarget(id = 2, value = 0.1, actionIndex = 1)

            return EvaluatedIndividual(
                    fitness = fv1, individual = ind1, results = listOf(),
                    enableTracking = true, trackOperator = this, enableImpact = true
            )
        }

    }

    class Ind(val actions : MutableList<IndAction>) : Individual(){
        companion object{
            fun getInd() : Ind{
                return Ind(mutableListOf(IndAction.getIndAction(), IndAction.getIndAction()))
            }
        }
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

        companion object{
            fun getIndAction(): IndAction{
                return IndAction(
                        listOf(StringGene("index1","index1"), StringGene("index2", "index2"))
                )
            }
        }

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