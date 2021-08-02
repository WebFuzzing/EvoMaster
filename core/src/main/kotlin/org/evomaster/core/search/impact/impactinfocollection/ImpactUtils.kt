package org.evomaster.core.search.impact.impactinfocollection

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.sql.*
import org.evomaster.core.search.impact.impactinfocollection.sql.*
import org.evomaster.core.search.impact.impactinfocollection.value.*
import org.evomaster.core.search.impact.impactinfocollection.value.collection.ArrayGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.collection.MapGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.collection.EnumGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.date.DateGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.date.DateTimeGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.date.TimeGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.*
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.regex.*
import org.evomaster.core.search.impact.impactinfocollection.regex.*
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * created by manzh on 2019-09-09
 *
 * this utility can be used to e.g.,
 * 1) create new impact
 * 2) generate gene id for linking with its impact
 */
class ImpactUtils {

    companion object{

        private val log: Logger = LoggerFactory.getLogger(ImpactUtils::class.java)

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
                // regex
                is RegexGene -> RegexGeneImpact(id, gene)
                is DisjunctionListRxGene -> DisjunctionListRxGeneImpact(id, gene)
                is DisjunctionRxGene -> DisjunctionRxGeneImpact(id, gene)
                is QuantifierRxGene -> QuantifierRxGeneImpact(id, gene)
                is RxAtom -> RxAtomImpact(id)
                is RxTerm -> RxTermImpact(id)
                else ->{
                    LoggingUtil.uniqueWarn(log, "the impact of {} was collected in a general manner, i.e., GeneImpact", gene::class.java.simpleName)
                    GeneImpact(id)
                }
            }
        }

        private const val SEPARATOR_ACTION_TO_GENE = "::"
        private const val SEPARATOR_GENE = ";"
        private const val SEPARATOR_GENE_WITH_TYPE = ">"

        fun generateGeneId(action: Action, gene : Gene) : String = "${action.getName()}$SEPARATOR_ACTION_TO_GENE${generateGeneId(gene)}$SEPARATOR_ACTION_TO_GENE${action.seeGenes().indexOf(gene)}"

        fun extractActionName(geneId : String) : String?{
            if (!geneId.contains(SEPARATOR_ACTION_TO_GENE)) return null
            return geneId.split(SEPARATOR_ACTION_TO_GENE).first()
        }

        fun <T : Individual> generateGeneId(individual: T, gene: Gene) : String{
            if (!individual.seeGenes().contains(gene)){
                log.warn("cannot find this gene ${gene.name} ($gene) in this individual")
                return generateGeneId(gene)
            }
            individual.seeInitializingActions().find { da->
                da.seeGenes().contains(gene)
            }?.let {
                return generateGeneId(it, gene)
            }
            individual.seeActions(ActionFilter.NO_INIT).find { a-> a.seeGenes().contains(gene) }?.let {
                return generateGeneId(action = it, gene = gene)
            }
            return generateGeneId(gene)
        }

        /**
         * extract info regarding a gene (on a action of an individual if it has) before mutated and the gene after mutated
         *
         * @param mutatedGenes genes were mutated
         * @param individual a mutated individual with [mutatedGenes]
         * @param previousIndividual mutating [previousIndividual] becomes [individual]
         */
        private fun extractMutatedGeneWithContext(
                mutatedGenes : MutableList<Gene>,
                individual: Individual,
                previousIndividual: Individual
        ) : Map<String, MutableList<MutatedGeneWithContext>>{
            val mutatedGenesWithContext = mutableMapOf<String, MutableList<MutatedGeneWithContext>>()

            if (individual.seeActions().isEmpty()){
                individual.seeGenes().filter { mutatedGenes.contains(it) }.forEach { g->
                    val id = generateGeneId(individual, g)
                    val contexts = mutatedGenesWithContext.getOrPut(id){ mutableListOf()}
                    val previous = findGeneById(previousIndividual, id)?: throw IllegalArgumentException("mismatched previous individual")
                    contexts.add(MutatedGeneWithContext(g, previous = previous, numOfMutatedGene = mutatedGenes.size))
                }
            }else{
                individual.seeActions().forEachIndexed { index, action ->
                    action.seeGenes().filter { mutatedGenes.contains(it) }.forEach { g->
                        val id = generateGeneId(action, g)
                        val contexts = mutatedGenesWithContext.getOrPut(id){ mutableListOf()}
                        val previous = findGeneById(previousIndividual, id, action.getName(), index, false)?: throw IllegalArgumentException("mismatched previous individual")
                        contexts.add(MutatedGeneWithContext(g, action.getName(), index, previous, mutatedGenes.size))
                    }
                }
            }


            Lazy.assert{
                mutatedGenesWithContext.values.sumBy { it.size } == mutatedGenes.size
            }
            return mutatedGenesWithContext
        }

        fun extractMutatedGeneWithContext(mutatedGeneSpecification: MutatedGeneSpecification,
                                          individual: Individual,
                                          previousIndividual: Individual,
                                          fromInitialization : Boolean) : MutableList<MutatedGeneWithContext>{
            val num = mutatedGeneSpecification.numOfMutatedGeneInfo()

            val actions = if (fromInitialization) individual.seeInitializingActions() else individual.seeActions(ActionFilter.NO_INIT)
            val list = mutableListOf<MutatedGeneWithContext>()
            if (actions.isEmpty()) return list

            if (actions.isNotEmpty()){
                actions.forEach { a ->
                    val index = actions.indexOf(a)
                    val manipulated = mutatedGeneSpecification.isActionMutated(index, !fromInitialization)
                    if (manipulated){
                        a.seeGenes().filter {
                            if (fromInitialization)
                                mutatedGeneSpecification.mutatedDbGeneInfo().contains(it)
                            else
                                mutatedGeneSpecification.mutatedGeneInfo().contains(it)
                        }.forEach { mutatedg->
                            val id = generateGeneId(a, mutatedg)
                            val previous = findGeneById(
                                    individual=previousIndividual,
                                    id = id,
                                    actionName = a.getName(),
                                    indexOfAction = index,
                                    isDb = fromInitialization
                            )
                            list.add(MutatedGeneWithContext(current = mutatedg, previous = previous, position = index, action = a.getName(), numOfMutatedGene = num))
                        }
                    }
                }
                Lazy.assert {
                    list.size == if (fromInitialization) mutatedGeneSpecification.mutatedDbGenes.size else mutatedGeneSpecification.mutatedGenes.size
                }
                return list
            }

            Lazy.assert { !fromInitialization }

            individual.seeGenes().filter { mutatedGeneSpecification.mutatedGeneInfo().contains(it) }.forEach { g->
                val id = generateGeneId(individual, g)
                val previous = findGeneById(previousIndividual, id)?: throw IllegalArgumentException("mismatched previous individual")
                list.add(MutatedGeneWithContext(g, previous = previous, numOfMutatedGene = num))
            }
            Lazy.assert {
                list.size == mutatedGeneSpecification.mutatedGenes.size
            }
            return list
        }


        private fun findGeneById(individual: Individual, id : String, actionName : String, indexOfAction : Int, isDb : Boolean):Gene?{
            if (indexOfAction >= (if (isDb) individual.seeInitializingActions() else individual.seeActions(ActionFilter.NO_INIT)).size) return null
            val action = if (isDb) individual.seeInitializingActions()[indexOfAction] else individual.seeActions(ActionFilter.NO_INIT)[indexOfAction]
            if (action.getName() != actionName)
                throw IllegalArgumentException("mismatched gene mutated info")
            return action.seeGenes().find { generateGeneId(action, it) == id }
        }

        private fun findGeneById(individual: Individual, id : String):Gene?{
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

        /**
         * find a gene that has the same with [gene], but different value
         * @param gene is one of root genes of [action]
         */
        fun findMutatedGene(action: Action, gene : Gene, includeSameValue : Boolean = false) : Gene?{
            return findMutatedGene(action.seeGenes(), gene, includeSameValue)
        }


        fun findMutatedGene(genes: List<Gene>, gene : Gene, includeSameValue : Boolean = false) : Gene?{
            val template = ParamUtil.getValueGene(gene)
            return genes.filter {o->
                val g = ParamUtil.getValueGene(o)
                g.name == template.name && g::class.java.simpleName == template::class.java.simpleName && (includeSameValue || !g.containsSameValueAs(template))
            }.also {
                if (it.size > 1)
                    log.warn("{} genes have been mutated with the name {}",it.size, gene.name)
            }.firstOrNull()
        }
    }
}