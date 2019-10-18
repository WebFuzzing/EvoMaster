package org.evomaster.core.search.impact

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.sql.*
import org.evomaster.core.search.impact.sql.*
import org.evomaster.core.search.impact.value.*
import org.evomaster.core.search.impact.value.collection.ArrayGeneImpact
import org.evomaster.core.search.impact.value.collection.MapGeneImpact
import org.evomaster.core.search.impact.value.collection.EnumGeneImpact
import org.evomaster.core.search.impact.value.date.DateGeneImpact
import org.evomaster.core.search.impact.value.date.DateTimeGeneImpact
import org.evomaster.core.search.impact.value.date.TimeGeneImpact
import org.evomaster.core.search.impact.value.numeric.*
import org.evomaster.core.Lazy

/**
 * created by manzh on 2019-09-09
 *
 * this utility can be used to e.g.,
 * 1) create new impact
 * 2) generate gene id for linking with its impact
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
                is Base64StringGene -> StringGeneImpact(id, gene.data)
                is ObjectGene -> ObjectGeneImpact(id, gene)
                is MapGene<*>-> MapGeneImpact(id)
                is ArrayGene<*> -> ArrayGeneImpact(id)
                is DateGene -> DateGeneImpact(id, gene)
                is DateTimeGene -> DateTimeGeneImpact(id, gene)
                is TimeGene -> TimeGeneImpact(id, gene)
                //sql
                is SqlNullable -> SqlNullableImpact(id, gene)
                is SqlJSONGene -> SqlJsonGeneImpact(id, gene)
                is SqlXMLGene -> SqlXmlGeneImpact(id, gene)
                is SqlUUIDGene -> SqlUUIDGeneImpact(id, gene)
                is SqlPrimaryKeyGene -> SqlPrimaryKeyGeneImpact(id, gene)
                is SqlForeignKeyGene -> SqlForeignKeyGeneImpact(id)
                else ->{
                    //TODO for RegexGene
                    GeneImpact(id)
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
            individual.seeInitializingActions().find { da->
                da.seeGenes().contains(gene)
            }?.let {
                return generateGeneId(it, gene)
            }
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
                        val previous = findGeneById(previousIndividual, id, action.getName(), index, false)?: throw IllegalArgumentException("mismatched previous individual")
                        contexts.add(MutatedGeneWithContext(g, action.getName(), index, previous))
                    }
                }
            }


            Lazy.assert{
                mutatedGenesWithContext.values.sumBy { it.size } == mutatedGenes.size
            }
            return mutatedGenesWithContext
        }

        fun extractMutatedDbGeneWithContext(mutatedGenes : MutableList<Gene>, individual: Individual, previousIndividual: Individual) : Map<String, MutableList<MutatedGeneWithContext>>{
            val mutatedGenesWithContext = mutableMapOf<String, MutableList<MutatedGeneWithContext>>()

            individual.seeInitializingActions().forEachIndexed { index, action ->
                action.seeGenes().filter { mutatedGenes.contains(it) }.forEach { g->
                    val id = generateGeneId(action, g)
                    val contexts = mutatedGenesWithContext.getOrPut(id){ mutableListOf()}
                    val previous = findGeneById(previousIndividual, id, action.getName(), index, true)
                    contexts.add(MutatedGeneWithContext(g, action.getName(), index, previous))
                }
            }

            Lazy.assert{mutatedGenesWithContext.values.sumBy { it.size } == mutatedGenes.size}
            return mutatedGenesWithContext
        }

        fun findGeneById(individual: Individual, id : String, actionName : String, indexOfAction : Int, isDb : Boolean):Gene?{
            if (isDb && indexOfAction > individual.seeInitializingActions().size) return null
            val action = if (isDb) individual.seeInitializingActions()[indexOfAction] else individual.seeActions()[indexOfAction]
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

            Lazy.assert{names.size == 2}
            return actions.filter { it.getName() == names[0] }.flatMap { it.seeGenes() }.filter { it.name == names[1] }.toMutableList()
        }

        fun isAnyChange(geneA : Gene, geneB : Gene) : Boolean{
            Lazy.assert{geneA::class.java.simpleName == geneB::class.java.simpleName}
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

        fun getImpactDistribution(impacts : List<Impact>, property: ImpactProperty, targets : Set<Int>) : ImpactPropertyDistribution{
            val specified = impacts.filter {
                when(property){
                    ImpactProperty.TIMES_NO_IMPACT -> it.timesOfNoImpacts > 0
                    ImpactProperty.TIMES_IMPACT ->  it.timesOfImpact.any { t -> (targets.isEmpty() || targets.contains(t.key)) && t.value > 0}
                    ImpactProperty.TIMES_CONS_NO_IMPACT_FROM_IMPACT -> it.noImpactFromImpact.any { t -> (targets.isEmpty() || targets.contains(t.key)) && t.value > 0 }
                    ImpactProperty.TIMES_CONS_NO_IMPROVEMENT -> it.noImprovement.any { t -> (targets.isEmpty() || targets.contains(t.key)) && t.value > 0}
                }
            }.size
            return when{
                specified == 0 -> ImpactPropertyDistribution.NONE
                specified == impacts.size -> ImpactPropertyDistribution.ALL
                specified < impacts.size * 0.3 -> ImpactPropertyDistribution.FEW
                specified > impacts.size * 0.7 -> ImpactPropertyDistribution.MOST
                else -> ImpactPropertyDistribution.EQUAL
            }
        }

    }
}