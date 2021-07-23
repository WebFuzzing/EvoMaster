package org.evomaster.core.search.impact.impactinfocollection.individual

import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionResult
import org.evomaster.core.output.EvaluatedIndividualBuilder.Companion.generateIndividualResults
import org.evomaster.core.search.*
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
/**
 * created by manzh on 2019-12-05
 */
class IndividualGeneImpactTest {

    @Test
    fun testCompleteInitializationImpactUpdate(){
        val simulatedMutator = SimulatedMutator()
        simulatedMutator.config.abstractInitializationGeneToMutate = false
        val evi_ind1 = simulatedMutator.getFakeEvaluatedIndividualWithInitialization(initActionSize = 0)
        val addedInit = IndAction.getSeqIndAction(templates = arrayOf(2,3), repeat = arrayOf(1,3))

        evi_ind1.initAddedInitializationGenes(addedInit,0)
        assertNotNull(evi_ind1.impactInfo)
        assertEquals(2*(1+1) + 3*(3+1), evi_ind1.getInitializationGeneImpact().size)
    }

    @Test
    fun testAbstractInitializationImpactUpdate(){
        val simulatedMutator = SimulatedMutator()
        simulatedMutator.config.abstractInitializationGeneToMutate = true
        val evi_ind1 = simulatedMutator.getFakeEvaluatedIndividualWithInitialization(initActionSize = 0)
        val addedInit = IndAction.getSeqIndAction(templates = arrayOf(2,3), repeat = arrayOf(1,3))

        evi_ind1.initAddedInitializationGenes(addedInit,0)
        assertNotNull(evi_ind1.impactInfo)
        assertEquals(5, evi_ind1.getInitializationGeneImpact().size)
    }

    @Test
    fun testGeneImpactUpdate(){
        val simulatedMutator = SimulatedMutator()
        val config = EMConfig()

        val evi_ind1 = simulatedMutator.getFakeEvaluatedIndividual()
        evi_ind1.wrapWithTracking(null, 10, mutableListOf(evi_ind1.copy(TraceableElementCopyFilter.WITH_ONLY_EVALUATED_RESULT)))

        assert(evi_ind1.getSizeOfImpact(fromInitialization = false) == 2)
        val mutatedIndex = 1
        val spec = MutatedGeneSpecification()

        val evi_ind2 = simulatedMutator.fakeMutator(evi_ind1, mutatedIndex, spec, 1)

        assert(spec.mutatedIndividual != null)
        assert(spec.mutatedGenes.size == 1)
        val mutatedGeneId = ImpactUtils.generateGeneId(spec.mutatedIndividual!!, spec.mutatedGenes.first().gene!!)

        val evaluatedTargets = mutableMapOf<Int, EvaluatedMutation>()
        evaluatedTargets[simulatedMutator.getNewTarget()] = EvaluatedMutation.BETTER_THAN


        evi_ind1.fitness.computeDifference(
                other = evi_ind2.fitness,
                targetSubset = simulatedMutator.getInitialTargets(),
                targetInfo = evaluatedTargets,
                config = config
        )

        val improvedTarget = evaluatedTargets.filter { it.value == EvaluatedMutation.BETTER_THAN }
        val impactTarget = evaluatedTargets.filter { it.value != EvaluatedMutation.EQUAL_WITH }

        assertEquals(2, impactTarget.size)
        assertEquals(2, improvedTarget.size)

        assert(improvedTarget.keys.containsAll(setOf(2,3)))
        assert(impactTarget.keys.containsAll(setOf(2,3)))

        val tracked_evi_ind2 = evi_ind1.next(
                next = evi_ind2,
                copyFilter = TraceableElementCopyFilter.WITH_ONLY_EVALUATED_RESULT,
                evaluatedResult = EvaluatedMutation.BETTER_THAN
        )

        assert(tracked_evi_ind2 != null)

        tracked_evi_ind2!!.updateImpactOfGenes(
                previous = evi_ind1,
                mutated = tracked_evi_ind2,
                mutatedGenes = spec,
                targetsInfo = evaluatedTargets
        )

        assert(tracked_evi_ind2.getSizeOfImpact(false) == 2)
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
        evi_ind1.wrapWithTracking(null, 10, mutableListOf(evi_ind1.copy(TraceableElementCopyFilter.WITH_ONLY_EVALUATED_RESULT)))

        assert(evi_ind1.getSizeOfImpact(false) == 2)
        val mutatedIndex = 1
        val spec = MutatedGeneSpecification()

        val evi_ind2 = simulatedMutator.fakeStructureMutator(evi_ind1, mutatedIndex, true, spec, 1)
        val evaluatedTargets = mutableMapOf<Int, EvaluatedMutation>()
        evaluatedTargets[simulatedMutator.getNewTarget()] = EvaluatedMutation.BETTER_THAN

        evi_ind1.fitness.computeDifference(
                other = evi_ind2.fitness,
                targetSubset = simulatedMutator.getInitialTargets(),
                targetInfo = evaluatedTargets,
                config = EMConfig()
        )

        val improvedTarget = evaluatedTargets.filter { it.value == EvaluatedMutation.BETTER_THAN }
        val impactTarget = evaluatedTargets.filter { it.value != EvaluatedMutation.EQUAL_WITH }


        assertEquals(2, impactTarget.size)
        assertEquals(2, improvedTarget.size)

        assert(improvedTarget.keys.containsAll(setOf(2,3)))
        assert(impactTarget.keys.containsAll(setOf(2,3)))


        val tracked_evi_ind2 = evi_ind1.next(
                next = evi_ind2, copyFilter = TraceableElementCopyFilter.WITH_ONLY_EVALUATED_RESULT, evaluatedResult = EvaluatedMutation.BETTER_THAN
        )

        assert(tracked_evi_ind2 != null)

        tracked_evi_ind2!!.updateImpactOfGenes(
                previous = evi_ind1,
                mutated = evi_ind2,
                mutatedGenes = spec,
                targetsInfo = evaluatedTargets
        )

        assertEquals(1, tracked_evi_ind2.getSizeOfImpact(false))


    }

    class SimulatedMutator :TrackOperator{

        val config = EMConfig()

        init {
            config.enableTrackEvaluatedIndividual = true
            //config.probOfArchiveMutation = 0.5
            config.doCollectImpact = true
        }

        fun fakeMutator(evaluatedIndividual: EvaluatedIndividual<Ind>, mutatedIndex : Int, mutatedGeneSpecification: MutatedGeneSpecification, index: Int) : EvaluatedIndividual<Ind>{
            val ind2 = evaluatedIndividual.individual.copy() as Ind

            val mutatedGene = (ind2.seeActions()[mutatedIndex].seeGenes()[1] as StringGene)

            mutatedGeneSpecification.addMutatedGene(
                    isDb = false,
                    position = mutatedIndex,
                    valueBeforeMutation = mutatedGene.value,
                    gene = mutatedGene
            )

            mutatedGene.value = "mutated_index2"
            mutatedGeneSpecification.setMutatedIndividual(ind2)

            val fv2 = evaluatedIndividual.fitness.copy()
            fv2.updateTarget(getNewTarget(), 0.5, mutatedIndex)
            fv2.updateTarget(getExistingImprovedTarget(), 0.5, mutatedIndex)

            return EvaluatedIndividual(fv2, ind2, generateIndividualResults(ind2), index = index)
        }

        fun fakeStructureMutator(evaluatedIndividual: EvaluatedIndividual<Ind>, mutatedIndex : Int, remove: Boolean, mutatedGeneSpecification: MutatedGeneSpecification, index : Int) : EvaluatedIndividual<Ind>{
            val ind2 = evaluatedIndividual.individual.copy() as Ind
            if (ind2.seeActions().size == 1 && remove)
                throw IllegalArgumentException("action cannot be removed since there is only one action")
            if (remove){
                val removedAction = ind2.seeActions()[mutatedIndex]

                mutatedGeneSpecification.addRemovedOrAddedByAction(
                    removedAction,
                    mutatedIndex,
                    true,mutatedIndex
                )
                ind2.actions.removeAt(mutatedIndex)

            }else{
                val action = IndAction.getIndAction(1).first()

                mutatedGeneSpecification.addRemovedOrAddedByAction(
                    action,
                    mutatedIndex,
                    false,mutatedIndex
                )

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

            return EvaluatedIndividual(fv2, ind2, generateIndividualResults(ind2), index = index)
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
                    fitness = fv1, individual = ind1, results = generateIndividualResults(ind1),
                    trackOperator = this, config = config)
        }

        fun getFakeEvaluatedIndividualWithInitialization(actionSize: Int = 2, initActionSize: Int) : EvaluatedIndividual<Ind>{
            val ind1 = Ind.getIndWithInitialization(actionSize, initActionSize)


            val fv1 = FitnessValue(actionSize.toDouble())

            fv1.updateTarget(id = 1, value = 0.1, actionIndex = 0)
            fv1.updateTarget(id = 2, value = 0.1, actionIndex = 1)

            return EvaluatedIndividual(
                    fitness = fv1, individual = ind1, results = generateIndividualResults(ind1),
                    trackOperator = this, config = config)
        }

    }

    class Ind(val actions : MutableList<IndAction>, val initialization : MutableList<IndAction> = mutableListOf()) : Individual(children = initialization.plus(actions)){
        companion object{
            fun getInd() : Ind{
                return Ind(IndAction.getIndAction(2).toMutableList())
            }

            fun getIndWithInitialization(actionSize: Int, initializationSize: Int) : Ind{
                return Ind(IndAction.getIndAction(2).toMutableList(), IndAction.getSeqIndAction(initializationSize).toMutableList())
            }
        }
        override fun copyContent(): Individual {
            return Ind(actions.map { it.copyContent() as IndAction }.toMutableList())
        }

        override fun getChildren(): List<Action> = initialization.plus(actions)

        override fun seeGenes(filter: GeneFilter): List<out Gene> {
           return when(filter){
               GeneFilter.ONLY_SQL -> seeInitializingActions().flatMap(Action::seeGenes)
               GeneFilter.NO_SQL -> seeActions().flatMap(Action::seeGenes)
               GeneFilter.ALL -> seeInitializingActions().plus(seeActions()).flatMap(Action::seeGenes)
               else -> throw IllegalArgumentException("$filter is not supported by ImpactTest Individual")
           }
        }

        override fun size(): Int = seeActions().size

        override fun seeActions(): List<out Action> {
            return actions
        }

        override fun seeInitializingActions(): List<Action> {
            return initialization
        }

        override fun verifyInitializationActions(): Boolean {
            return true
        }

        override fun repairInitializationActions(randomness: Randomness) {}
    }

    class IndAction(private val genes : List<out Gene>) : Action(genes){

        override fun getChildren(): List<Gene> = genes

        companion object{
            fun getIndAction(size: Int = 1): List<IndAction>{
                if(size < 1) throw IllegalArgumentException("size should be at least 1, but $size")
                return (0 until size).map { IndAction(listOf(StringGene("index1","index1"), StringGene("index2", "index2")))}
            }

            fun getSeqIndAction(size : Int) : List<IndAction>{
                if(size < 0) throw IllegalArgumentException("size should not be less than 0, but $size")
                if (size == 0) return  listOf()
                return (0 until size).map { IndAction(listOf(IntegerGene(
                        name = "index$it",
                        value = it
                ))) }
            }

            fun getSeqIndAction(templates : Array<Int>, repeat : Array<Int>) : List<List<IndAction>>{
                if(templates.any { it < 1 }) throw IllegalArgumentException("size of template should be at least 1")
                if (repeat.any { it < 0 }) throw IllegalArgumentException("repeat times should not be less than 0")
                if (templates.size != repeat.size) throw IllegalArgumentException("size of the configuration for template and repeat should be same")
                val actions = mutableListOf<List<IndAction>>()
                templates.forEachIndexed { t, i ->
                    (0..repeat[t]).forEach { _->
                        actions.add((0 until i).map {
                            IndAction(listOf(IntegerGene(
                                    name = "index$t$it",
                                    value = it
                            )))
                        })
                    }
                }
                return actions
            }
        }

        override fun getName(): String {
            return genes.joinToString(",") { it.name }
        }

        override fun seeGenes(): List<out Gene> {
            return genes
        }

        override fun copyContent(): Action {
            return IndAction(genes.map { it.copyContent() })
        }

        override fun shouldCountForFitnessEvaluations(): Boolean = true

        override fun randomize(randomness: Randomness, forceNewValue: Boolean, all: List<Action>) {
            seeGenes().forEach { it.randomize(randomness, forceNewValue) }
        }

    }
}