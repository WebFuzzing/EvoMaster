package org.evomaster.core.search.impact

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.value.DisruptiveGeneImpact
import org.evomaster.core.search.impact.value.ObjectGeneImpact
import org.evomaster.core.search.impact.value.OptionalGeneImpact
import org.evomaster.core.search.impact.value.StringGeneImpact
import org.evomaster.core.search.impact.value.collection.CollectionGeneImpact
import org.evomaster.core.search.impact.value.collection.EnumGeneImpact
import org.evomaster.core.search.impact.value.date.DateGeneImpact
import org.evomaster.core.search.impact.value.date.DateTimeGeneImpact
import org.evomaster.core.search.impact.value.date.TimeGeneImpact
import org.evomaster.core.search.impact.value.numeric.*

/**
 * created by manzh on 2019-09-09
 */
class ImpactUtils {

    companion object{

        fun createGeneImpact(gene : Gene, id : String) : GeneImpact{
            return when(gene){
                is DisruptiveGene<*> -> DisruptiveGeneImpact(id, gene)
                is OptionalGene -> OptionalGeneImpact(id, gene)
                is BooleanGene -> BinaryGeneImpact(id)
                is EnumGene<*> -> EnumGeneImpact(id, gene)
                is IntegerGene -> IntegerGeneImpact(id)
                is LongGene -> LongGeneImpact(id)
                is DoubleGene -> DoubleGeneImpact(id)
                is FloatGene -> FloatGeneImpact(id)
                is StringGene -> StringGeneImpact(id, gene)
                is ObjectGene -> ObjectGeneImpact(id, gene)
                is MapGene<*>, is ArrayGene<*> -> CollectionGeneImpact(id)
                is DateGene -> DateGeneImpact(id, gene)
                is DateTimeGene -> DateTimeGeneImpact(id, gene)
                is TimeGene -> TimeGeneImpact(id, gene)
                else ->{
                    throw IllegalStateException("do not support to generate impacts for ${gene::class.java.simpleName}")
                }
            }
        }


        private const val SEPARATOR_ACTION_TO_GENE = "::"
        private const val SEPARATOR_GENE = ";"
        private const val SEPARATOR_GENE_WITH_TYPE = ">"

        fun generateGeneId(action: Action, gene : Gene) : String = "${action.getName()}$SEPARATOR_ACTION_TO_GENE${generateGeneId(gene)}"

        fun <T : Individual> generateGeneId(individual: T, gene: Gene) : String{
            if (!individual.seeGenes().contains(gene))
                throw IllegalArgumentException("cannot find this gene in this individual")
            individual.seeActions().find { a-> a.seeGenes().contains(gene) }?.let {
                return generateGeneId(it, gene)
            }
            return generateGeneId(gene)
        }

        fun extractMutatedGeneWithContext(mutatedGenes : MutableList<Gene>, individual: Individual, previousIndividual: Individual) : Map<String, MutableList<MutatedGeneWithContext>>{
            val mutatedGenesWithContext = mutableMapOf<String, MutableList<MutatedGeneWithContext>>()

            if (individual.seeActions().isEmpty()){
                //throw IllegalArgumentException("do not support to extract contexts of mutated genes for an individual which does not have any action, i.e., seeAction() is empty.")
                individual.seeGenes().filter { mutatedGenes.contains(it) }.forEach { g->
                    val id = generateGeneId(individual, g)
                    val contexts = mutatedGenesWithContext.getOrPut(id){ mutableListOf()}
                    val previous = findGeneById(previousIndividual, id)?: throw IllegalArgumentException("mismatched previous individual")
                    contexts.add(MutatedGeneWithContext(g, previous = previous))
                }
            }else{
                individual.seeActions().forEachIndexed { index, action ->
                    action.seeGenes().filter { mutatedGenes.contains(it) }.forEach { g->
                        val id = generateGeneId(action, g)
                        val contexts = mutatedGenesWithContext.getOrPut(id){ mutableListOf()}
                        val previous = findGeneById(previousIndividual, id, action.getName(), index)?: throw IllegalArgumentException("mismatched previous individual")
                        contexts.add(MutatedGeneWithContext(g, action.getName(), index, previous))
                    }
                }
            }

            assert(mutatedGenesWithContext.values.sumBy { it.size } == mutatedGenes.size)
            return mutatedGenesWithContext
        }

        fun findGeneById(individual: Individual, id : String, actionName : String, indexOfAction : Int):Gene?{
            val action = individual.seeActions()[indexOfAction]
            if (action.getName() != actionName)
                throw IllegalArgumentException("mismatched gene mutated info")
            return action.seeGenes().find { generateGeneId(action, it) == id }
        }

        fun findGeneById(individual: Individual, id : String):Gene?{
            return individual.seeGenes().find { generateGeneId(individual, it) == id }
        }

        fun extractGeneById(actions: List<Action>, id: String) : MutableList<Gene>{
            if (actions.isEmpty() || id.contains(SEPARATOR_ACTION_TO_GENE)) return mutableListOf()

            val names = id.split(SEPARATOR_ACTION_TO_GENE)

            assert(names.size == 2)
            return actions.filter { it.getName() == names[0] }.flatMap { it.seeGenes() }.filter { it.name == names[1] }.toMutableList()
        }

        fun isAnyChange(geneA : Gene, geneB : Gene) : Boolean{
            assert(geneA::class.java.simpleName == geneB::class.java.simpleName)
            return geneA.getValueAsRawString() == geneB.getValueAsRawString()
        }

        /**
         * TODO: should handle SQL related genes separately?
         */
        fun generateGeneId(gene: Gene):String{
            return when(gene){
                is DisruptiveGene<*> -> "DisruptiveGene$SEPARATOR_GENE_WITH_TYPE${generateGeneId(gene.gene)}"
                is OptionalGene -> "OptionalGene$SEPARATOR_GENE_WITH_TYPE${generateGeneId(gene.gene)}"
                is ObjectGene -> if (gene.refType.isNullOrBlank()) gene.name else "${gene.refType}$SEPARATOR_GENE_WITH_TYPE${gene.name}"
                else -> gene.name
            }
        }

        /**
         * TODO: handle SQL Actions in an individual
         */
        fun generateIndividualId(individual: Individual) : String = individual.seeActions().joinToString(SEPARATOR_GENE) { it.getName() }


        fun processImpact(impact : GeneImpact, gc : MutatedGeneWithContext, hasImpact : Boolean, countDeepObjectImpact : Boolean = false, isWorse : Boolean){

            impact.countImpactAndPerformance(hasImpact, isWorse)

            when(impact){
                is ObjectGeneImpact -> {
                    if (gc.previous !is ObjectGene || gc.current !is ObjectGene)
                        throw IllegalStateException("previous and current gene should be ObjectGene")
                    impact.countFieldImpact(previous = gc.previous,current =  gc.current, hasImpact = hasImpact, countDeepImpact = countDeepObjectImpact, isWorse = isWorse)
                }
                is CollectionGeneImpact -> {
                    val diff = when{
                        gc.previous is MapGene<*> && gc.current is MapGene<*> -> gc.previous.elements.size != gc.current.elements.size
                        gc.previous is ArrayGene<*> && gc.current is ArrayGene<*> -> gc.previous.elements.size != gc.current.elements.size
                        else -> throw IllegalStateException("previous and current gene should be MapGene or ArrayGene")
                    }
                    impact.countSizeImpact(diff, hasImpact, isWorse)
                }
                is EnumGeneImpact ->{
                    if (gc.current !is EnumGene<*>)
                        throw IllegalStateException("previous and current gene should be EnumGene")
                    impact.countValueImpact(gc.current.index, hasImpact, isWorse)
                }
                is OptionalGeneImpact ->{
                    if (gc.current !is OptionalGene || gc.previous !is OptionalGene)
                        throw IllegalStateException("previous and current gene should be OptionalGene")
                    impact.countActiveImpact(gc.current.isActive, hasImpact, isWorse)

                    val gcGene = MutatedGeneWithContext(current = gc.current.gene, previous = gc.previous.gene, action = gc.action, position = gc.position)
                    processImpact(impact.geneImpact, gcGene, hasImpact, isWorse = isWorse)
                }
                is DisruptiveGeneImpact ->{
                    if (gc.current !is DisruptiveGene<*> || gc.previous !is DisruptiveGene<*>)
                        throw IllegalStateException("previous and current gene should be DisruptiveGene")
                    val gcGene = MutatedGeneWithContext(current = gc.current.gene, previous = gc.previous.gene, action = gc.action, position = gc.position)
                    processImpact(impact.geneImpact, gcGene, hasImpact, isWorse = isWorse)
                }
                is DateTimeGeneImpact ->{
                    if (gc.current !is DateTimeGene || gc.previous !is DateTimeGene)
                        throw IllegalStateException("previous and current gene should be DateTimeGene")
                    impact.countDateTimeImpact(current = gc.current, previous = gc.previous, hasImpact = hasImpact, isWorse = isWorse)
                }
                is TimeGeneImpact -> {
                    if (gc.current !is TimeGene || gc.previous !is TimeGene)
                        throw IllegalStateException("previous and current gene should be TimeGene")
                    impact.countTimeImpact(current = gc.current, previous = gc.previous, hasImpact = hasImpact, isWorse = isWorse)
                }
                is DateGeneImpact ->{
                    if (gc.current !is DateGene || gc.previous !is DateGene)
                        throw IllegalStateException("previous and current gene should be DateGene")
                    impact.countDateImpact(current = gc.current, previous = gc.previous, hasImpact = hasImpact, isWorse = isWorse)
                }
                is StringGeneImpact ->{
                    /*
                     TODO ask Andrea: is a type of specializationGene of StringGene dynamic or stable ?
                     */
                    if (gc.current !is StringGene || gc.previous !is StringGene)
                        throw IllegalStateException("previous and current gene should be DateTimeGene")
                    impact.countSpecializationGeneImpact(current = gc.current, previous = gc.previous, hasImpact = hasImpact, isWorse = isWorse)
                }
            }
        }

        private fun prioritizeNoVisit(genes : List<Pair<Gene, GeneImpact>>): List<Gene>{
            return genes.filter { it.second.timesToManipulate == 0 }.map { it.first }
        }

        fun selectGenesAwayBad(genes : List<Pair<Gene, GeneImpact>>, percentage : Double, prioritizeNoVisit : Boolean = true) : List<Gene>{
            if (prioritizeNoVisit) prioritizeNoVisit(genes).let { if (it.isNotEmpty()) return it }
            val size = decideSize(genes.size, percentage)
            return genes.sortedBy { it.second.timesOfNoImpacts }.subList(0, genes.size - size).map { it.first }
        }

        fun selectApproachGood(genes : List<Pair<Gene, GeneImpact>>, percentage : Double, prioritizeNoVisit : Boolean = true) : List<Gene>{
            if (prioritizeNoVisit) prioritizeNoVisit(genes).let { if (it.isNotEmpty()) return it }
            val size = decideSize(genes.size, percentage)
            return sortGenes(genes).subList(0, size)
        }

        private fun sortGenes(genes : List<Pair<Gene, GeneImpact>>, feedback : Boolean = false) : List<Gene>{
            val no = genes.sortedBy { if (feedback) it.second.counter else it.second.timesOfNoImpacts }
            val worse = genes.sortedBy { it.second.wCounter }
            return genes.sortedBy { g-> no.indexOf(g) + worse.indexOf(g) }.map { it.first }
        }

//        fun selectFeedback(genes : List<Pair<Gene, GeneImpact>>, percentage : Double, prioritizeNoVisit : Boolean = true) : List<Gene>{
//            if (prioritizeNoVisit) prioritizeNoVisit(genes).let { if (it.isNotEmpty()) return it }
//            val size = decideSize(genes.size, percentage)
//            return sortGenes(genes, true).subList(0, size)
//        }

        private fun decideSize(list : Int, percentage : Double) = (list * percentage).run {
            if(this < 1.0) 1 else this.toInt()
        }
    }
}