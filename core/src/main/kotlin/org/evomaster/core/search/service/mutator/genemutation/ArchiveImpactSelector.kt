package org.evomaster.core.search.service.mutator.genemutation

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.math.max

/**
 * Archive impact-based mutator which handle
 * - archive-based gene selection to mutate
 * - mutate the selected genes based on their performance (i.e., results of fitness evaluation)
 * - besides, string mutation is designed regarding fitness evaluation using LeftAlignmentDistance
 */
class ArchiveImpactSelector {

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config: EMConfig

    @Inject
    lateinit var apc: AdaptiveParameterControl

    @Inject
    lateinit var time : SearchTimeController

    /**
     * calculate weights of genes (ie, [map]) based on impacts
     */
    fun calculateWeightByArchive(genesToMutate : List<Gene>, map: MutableMap<Gene, Double>, individual: Individual,  evi: EvaluatedIndividual<*>, targets : Set<Int>){

        val geneImpacts =  genesToMutate.map { g ->
            evi.getImpact(individual, g) ?: evi.addGeneImpact(individual, g)?:
                    throw IllegalArgumentException("mismatched gene and impact info during mutation")
        }

        calculateWeightByArchive(genesToMutate, map, geneImpacts, targets)

        ArchiveMutationUtils.saveWeight(
                config = config,
                map = map,
                targets = targets,
                index = time.evaluatedIndividuals
        )
    }

    fun calculateWeightByArchive(genesToMutate : List<Gene>, map: MutableMap<Gene, Double>, impacts: List<Impact>, targets : Set<Int>){

        val weights = impactBasedOnWeights(impacts, targets)

        genesToMutate.forEachIndexed { index, gene ->
            map[gene] = weights[index]
        }
    }

    fun impactBasedOnWeights(impacts: List<Impact>, targets: Set<Int>): Array<Double> {
        //TODO later adaptive decide selection method
        val method = decideArchiveGeneSelectionMethod()
        if (method.adaptive)
            throw IllegalArgumentException("the decided method should be a fixed method")

        return impactBasedOnWeights(impacts, method, targets)
    }

    /**
     * @return weights calculated based on [impacts]
     * @param impacts a list of impacts
     * @param method a method to calculate weights of [impacts]
     * @param targets applied targets to assess their impact
     */
    private fun impactBasedOnWeights(impacts: List<Impact>, method: GeneMutationSelectionMethod, targets: Set<Int>): Array<Double> {
        return when (method) {
            GeneMutationSelectionMethod.AWAY_NOIMPACT -> impactBasedOnWeights(impacts, targets = targets, properties = arrayOf(ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET))
            GeneMutationSelectionMethod.APPROACH_IMPACT -> impactBasedOnWeights(impacts, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT))
            GeneMutationSelectionMethod.APPROACH_LATEST_IMPACT -> impactBasedOnWeights(impacts, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT, ImpactProperty.TIMES_CONS_NO_IMPACT_FROM_IMPACT))
            GeneMutationSelectionMethod.APPROACH_LATEST_IMPROVEMENT -> impactBasedOnWeights(impacts, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT, ImpactProperty.TIMES_CONS_NO_IMPROVEMENT))
            GeneMutationSelectionMethod.BALANCE_IMPACT_NOIMPACT -> impactBasedOnWeights(impacts, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT, ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET))
            GeneMutationSelectionMethod.BALANCE_IMPACT_NOIMPACT_WITH_E -> impactBasedOnWeights(impacts, targets = targets, properties = arrayOf(ImpactProperty.E_IMPACT_DIVIDE_NO_IMPACT))

            else -> {
                throw IllegalArgumentException("invalid gene selection method: method cannot be NONE or adaptive, but $method")
            }
        }
    }

    /**
     * decide an archive-based gene selection method when the selection is adaptive (i.e., [GeneMutationSelectionMethod.adaptive])
     */
    private fun decideArchiveGeneSelectionMethod() : GeneMutationSelectionMethod {
        return when (config.adaptiveGeneSelectionMethod) {
            GeneMutationSelectionMethod.ALL_FIXED_RAND -> randomGeneSelectionMethod()
            else -> config.adaptiveGeneSelectionMethod
        }
    }

    private fun randomGeneSelectionMethod(): GeneMutationSelectionMethod = randomness.choose(GeneMutationSelectionMethod.values().filter { !it.adaptive && it.archive })

    /**
     * this fun is used by [Archive] to sample an individual from population (i.e., [individuals])
     * if [ArchiveImpactSelector.enableArchiveSelection] is true.
     * In order to identify impacts of genes, we prefer to select an individual which has some impact info.
     */
    fun <T : Individual> selectIndividual(individuals: List<EvaluatedIndividual<T>>): EvaluatedIndividual<T> {
        if (randomness.nextBoolean(0.1)) return randomness.choose(individuals)
        val impacts = individuals.filter { it.anyImpactfulGene() }
        if (impacts.isNotEmpty()) return randomness.choose(impacts)
        return randomness.choose(individuals)
    }

    private fun <T> prioritizeNoVisit(genes: List<Pair<T, Impact>>): List<Pair<T, Impact>> {
        val noVisit = genes.filter { it.second.getTimesToManipulate() == 0 }.toMutableList()
        noVisit.shuffle()
        return noVisit
    }

    /**
     * extract values of [impacts] based on the specified [targets] and [properties] of impacts
     * @param impacts to be extracted
     * @param targets to be optimized
     * @param properties are properties of impacts, e.g., times Of NoImpact, times of impacts
     * @param usingCounter specifies whether to employ the counter (ie, times) to extract impacts, otherwise degree ([0.0, 1.0] times/total)
     */
    fun impactBasedOnWeights(impacts : List<Impact>, targets: Set<Int>, properties: Array<ImpactProperty>, usingCounter: Boolean? = null) : Array<Double>{

        val values : List<MutableList<Double>> = impacts.map { impact ->
            properties.map { p->
                if (usingCounter == null){
                    when(config.geneWeightBasedOnImpactsBy){
                        EMConfig.GeneWeightBasedOnImpact.COUNTER, EMConfig.GeneWeightBasedOnImpact.SORT_COUNTER -> getCounterByProperty(impact, p, targets)
                        // weight = degree * 100, otherwise the difference is quite minor
                        EMConfig.GeneWeightBasedOnImpact.RATIO, EMConfig.GeneWeightBasedOnImpact.SORT_RATIO -> getDegreeByProperty(impact, p, targets) * 100
                    }
                }else{
                    if (usingCounter) getCounterByProperty(impact, p, targets).toDouble()
                    else getDegreeByProperty(impact, p, targets) * 100
                }
            }.toMutableList()
        }

        handleNotVisit(values, properties.size)

        //Man: shall we normalize data?
        return when(config.geneWeightBasedOnImpactsBy){
            EMConfig.GeneWeightBasedOnImpact.COUNTER, EMConfig.GeneWeightBasedOnImpact.RATIO -> values.map { it.sum() + 1.0 }.toTypedArray()
            EMConfig.GeneWeightBasedOnImpact.SORT_COUNTER, EMConfig.GeneWeightBasedOnImpact.SORT_RATIO -> {
                val weights = Array(impacts.size){Array(properties.size){0.0}}
                (0 until properties.size).map {pi->
                    values.mapIndexed { index, list -> Pair(index, list[pi]) }.sortedBy { it.second }.forEachIndexed { index, pair ->
                        weights[pair.first][pi] = index + 1.0
                    }
                }
                weights.map { it.average() }.toTypedArray()
            }
        }
    }


    private fun handleNotVisit(values : List<MutableList<Double>>, sizeOfProperty: Int){

        (0 until sizeOfProperty).forEach {pi->
            val r = max(0.0,values.map { v->v[pi] }.maxOrNull()?:0.0)
            values.forEachIndexed { index, list -> if (list[pi] < 0) values[index][pi] = r }
        }
    }

    /**
     * the more，the better
     */
    private fun getCounterByProperty(impact: Impact, property: ImpactProperty, targets: Set<Int>): Double {
        val value = impact.getCounter(property, targets, By.SUM, true)?: return 0.0

        if (value < 0) return value

        return when (property) {
            ImpactProperty.TIMES_IMPACT -> value
            else -> impact.getTimesToManipulate() - value
        }
    }

    /**
     * the more，the better
     */
    private fun getDegreeByProperty(impact: Impact, property: ImpactProperty, targets: Set<Int>): Double {
        val value = impact.getDegree(property, targets, By.SUM, true) ?: return 1.0

        return when (property) {
            ImpactProperty.TIMES_IMPACT, ImpactProperty.E_IMPACT_DIVIDE_NO_IMPACT -> value
            else -> 1.0 - value
        }
    }

    /**************************** utilities ********************************************/

    /**
     * @return whether apply archive-based gene selection for individual or eg, ObjectGene
     */
    fun applyArchiveSelection() = config.isEnabledArchiveGeneSelection() && randomness.nextBoolean(config.probOfArchiveMutation)

    /**
     * export impact info collected during search that is normally used for experiment
     */
    fun exportImpacts(solution: Solution<*>) {
        val path = Paths.get(config.impactFile)
        if (path.parent != null) Files.createDirectories(path.parent)

        val content = mutableListOf<String>()
        content.add(mutableListOf("test", "actionIndex", "rootGene").plus(Impact.toCSVHeader()).joinToString(","))
        solution.individuals.forEachIndexed { index, e ->
            e.getInitializationGeneImpact().forEachIndexed { aindex, mutableMap ->
                mutableMap.forEach { (t, geneImpact) ->
                    content.add(mutableListOf(index.toString(), "Initialization$aindex", t).plus(geneImpact.toCSVCell()).joinToString(","))
                    geneImpact.flatViewInnerImpact().forEach { (name, impact) ->
                        content.add(mutableListOf(index.toString(), "Initialization$aindex", "$t-$name").plus(impact.toCSVCell()).joinToString(","))
                    }
                }
            }
            e.getActionGeneImpact().forEachIndexed { aindex, mutableMap ->
                mutableMap.forEach { (t, geneImpact) ->
                    content.add(mutableListOf(index.toString(), "Action$aindex", t).plus(geneImpact.toCSVCell()).joinToString(","))
                    geneImpact.flatViewInnerImpact().forEach { (name, impact) ->
                        content.add(mutableListOf(index.toString(), "Action$aindex", "$t-$name").plus(impact.toCSVCell()).joinToString(","))
                    }
                }
            }
        }
        if (content.size > 1) {
            Files.write(path, content)
        }
    }

    /**
     * save impact info
     * NOTE THAT it is only used for debugging
     */
    fun saveImpactSnapshot(index : Int, checkedTargets: Set<Int>, targetsInfo : Map<Int, EvaluatedMutation>, result: EvaluatedMutation, evaluatedIndividual: EvaluatedIndividual<*>) {
        if (!config.isEnabledImpactCollection()) return
        if(!config.saveImpactAfterMutation) return

        val path = Paths.get(config.impactAfterMutationFile)
        if (path.parent != null) Files.createDirectories(path.parent)
        if (Files.notExists(path)) Files.createFile(path)
        val text = "$index,${checkedTargets.joinToString("-")},${targetsInfo.filterValues { it.value >=0 }.keys.joinToString("-")},${targetsInfo.filterValues { it.isImproved() }.keys.joinToString("-")},$result,"
        val content = evaluatedIndividual.exportImpactAsListString(checkedTargets.plus(targetsInfo.keys).toSet()).map { "$text,$it" }
        if (content.isNotEmpty()) Files.write(path, content, StandardOpenOption.APPEND)
    }
}

