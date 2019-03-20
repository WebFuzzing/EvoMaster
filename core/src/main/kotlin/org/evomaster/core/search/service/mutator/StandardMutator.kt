package org.evomaster.core.search.service.mutator

import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N
import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N_BIASED_SQL
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Individual.GeneFilter.ALL
import org.evomaster.core.search.Individual.GeneFilter.NO_SQL
import org.evomaster.core.search.gene.*
import java.math.BigDecimal
import java.math.RoundingMode


open class StandardMutator<T> : Mutator<T>() where T : Individual {

    /**
     * List where each element at position "i" has value "2^i"
     */
    private val intpow2 = (0..30).map { Math.pow(2.0, it.toDouble()).toInt() }


    override fun doesStructureMutation(individual : T): Boolean {
        return individual.canMutateStructure() &&
                config.maxTestSize > 1 && // if the maxTestSize is 1, there is no point to do strcuture mutation
                randomness.nextBoolean(config.structureMutationProbability)
    }

    override fun genesToMutation(individual : T, evi: EvaluatedIndividual<T>) : List<Gene> {
        val filterMutate = if (config.generateSqlDataWithSearch) ALL else NO_SQL
        return individual.seeGenes(filterMutate).filter { it.isMutable() }
    }

    /**
     * Select genes to mutate based on Archive, there are several options:
     *      1. remove bad genes
     *      2. select good genes,
     *      3. recent good genes, e.g., feed-back sampling
     *
     * Feed-back sampling
     *  To improve performance, MIO employs a technique called feedback-directed sampling. For each
        population, there is a counter, initialised to zero. Every time an individual is sampled from a
        population X, its counter is increased by one. Every time a new, better test is successfully added to
        X, the counter for that population is reset to zero. When sampling a test from one of the populations,
        the population with the lowest counter is chosen. This helps focus the sampling on populations
        (one per testing target) for which there has been a recent improvement in the achieved fitness value.
        This is particularly effective to prevent spending significant search time on infeasible targets.
     */
    override fun selectGenesToMutate(individual: T, evi: EvaluatedIndividual<T>) : List<Gene>{
        val genesToMutate = genesToMutation(individual, evi)
        if(genesToMutate.isEmpty()) return mutableListOf()

        return if(randomness.nextBoolean(config.probOfArchiveMutation)){
            selectGenesByArchive(genesToMutate, individual, evi)
        }else selectGenesByDefault(genesToMutate, individual)
    }

    private fun selectGenesByArchive(genesToMutate : List<Gene>, individual: T, evi: EvaluatedIndividual<T>) : List<Gene>{

        val candidatesMap = individual.seeGenesIdMap().filter { genesToMutate.contains(it.key) }
        assert(candidatesMap.size == genesToMutate.size)

        val genes = when(config.geneSelectionMethod){
            EMConfig.GeneSelectionMethod.AWAY_BAD -> selectGenesAwayBad(genesToMutate,candidatesMap,evi)
            EMConfig.GeneSelectionMethod.APPROACH_GOOD -> selectGenesApproachGood(genesToMutate,candidatesMap,evi)
            EMConfig.GeneSelectionMethod.FEED_BACK -> selectGenesFeedback(genesToMutate, candidatesMap, evi)
            EMConfig.GeneSelectionMethod.NONE -> {
                emptyList()
            }
        }

        if (genes.isEmpty())
            return selectGenesByOneDivNum(genesToMutate, genesToMutate.size)

        return genes

    }

    private fun selectGenesAwayBad(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<T>): List<Gene>{
        //remove genes from candidate that has "bad" history with 90%, i.e., timesOfNoImpacts is not 0
        val genes =  genesToMutate.filter { g->
            evi.impactsOfGenes[candidatesMap.getValue(g)]?.timesOfNoImpacts?.let {
                it == 0 || (it > 0 && randomness.nextBoolean(0.1))
            }?:false
        }
        if(genes.isNotEmpty())
            return selectGenesByOneDivNum(genes, genes.size)
        else
            return emptyList()
    }

    private fun selectGenesApproachGood(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<T>): List<Gene>{

        val sortedByCounter = genesToMutate.toList().sortedBy { g->
            evi.impactsOfGenes[candidatesMap.getValue(g)]?.timesOfImpact
        }

        //first 10%?
        val size = (genesToMutate.size * config.perOfCandidateGenesToMutate).let {
            if(it > 1.0) it.toInt() else 1
        }

        val genes = genesToMutate.filter { sortedByCounter.subList(0, size).contains(it) }

        return selectGenesByOneDivNum(genes, genes.size)
    }

    private fun selectGenesFeedback(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<T>): List<Gene>{
        val notVisited =  genesToMutate.filter { g->
            evi.impactsOfGenes[candidatesMap.getValue(g)]?.let {
                it.timesToManipulate == 0
            }?:false
        }
        if(notVisited.isNotEmpty())
            return selectGenesByOneDivNum(notVisited, notVisited.size)

        val zero = genesToMutate.filter { g->
            evi.impactsOfGenes[candidatesMap.getValue(g)]?.let {
                it.counter == 0 && it.timesToManipulate > 0
            }?:false
        }

        /*
            TODO: shall we control the size in case of a large size of zero?
         */
        if(zero.isNotEmpty()){
            return zero
        }

        val sortedByCounter = genesToMutate.toList().sortedByDescending { g->
            evi.impactsOfGenes[candidatesMap.getValue(g)]?.counter
        }

        //first 10%?
        val size = (genesToMutate.size * config.perOfCandidateGenesToMutate).let {
            if(it > 1.0) it.toInt() else 1
        }

        val genes = genesToMutate.filter { sortedByCounter.subList(0, size).contains(it) }

        return selectGenesByOneDivNum(genes, genes.size)

    }

    private fun selectGenesByDefault(genesToMutate : List<Gene>,  individual: T) : List<Gene>{
        val filterN = when (config.geneMutationStrategy) {
            ONE_OVER_N -> ALL
            ONE_OVER_N_BIASED_SQL -> NO_SQL
        }
        val n = Math.max(1, individual.seeGenes(filterN).filter { it.isMutable() }.count())
        return selectGenesByOneDivNum(genesToMutate, n)
    }

    private fun selectGenesByOneDivNum(genesToMutate : List<Gene>, n : Int): List<Gene>{
        val genesToSelect = mutableListOf<Gene>()

        val p = 1.0 / n

        var mutated = false

        while (!mutated) { //no point in returning a next that is not mutated

            for (gene in genesToMutate) {

                if (!randomness.nextBoolean(p)) {
                    continue
                }

                if (gene is DisruptiveGene<*> && !randomness.nextBoolean(gene.probability)) {
                    continue
                }

                genesToSelect.add(gene)

                mutated = true
            }
        }
        return genesToSelect
    }

    private fun innerMutate(individual: EvaluatedIndividual<T>, mutatedGene: MutableList<Gene>) : T{
        val individualToMutate = individual.individual

        val copy =
                (if(config.enableTrackIndividual && individualToMutate.isCapableOfTracking())
                    individualToMutate.next(structureMutator.getTrackOperator()!!)
                else individualToMutate.copy()) as T

        if (doesStructureMutation(individualToMutate)) {
            //usually, either delete an action, or add a new random one
            structureMutator.mutateStructure(copy)
            return copy
        }

        val allGenes = copy.seeGenes().flatMap { it.flatView() }

        val selectGeneToMutate = selectGenesToMutate(copy, individual)

        if(selectGeneToMutate.isEmpty())
            return copy

        for (gene in selectGeneToMutate){
            mutatedGene.add(gene)
            mutateGene(gene, allGenes)
        }

        Lazy.assert {
            DbActionUtils.verifyForeignKeys(
                    individualToMutate.seeInitializingActions().filterIsInstance<DbAction>())
        }

        return copy
    }

    open fun repairAfterMutation(individual: T){
        individual.repairInitializationActions(randomness)
    }

    open fun verifyAfterMutation(individual: T){
        Lazy.assert { individual.verifyInitializationActions() }

    }

    override fun mutate(individual: EvaluatedIndividual<T>, mutatedGene: MutableList<Gene>): T {

        // First mutate the individual
        val mutatedIndividual = innerMutate(individual, mutatedGene)

        // Second repair the initialization actions (if needed)
        repairAfterMutation(mutatedIndividual)

        // Check that the repair was successful
        verifyAfterMutation(mutatedIndividual)

        return mutatedIndividual
    }

    private fun mutateGene(latest: Gene, gene: Gene, all: List<Gene>) {
        when (gene) {
            is SqlForeignKeyGene -> handleSqlForeignKeyGene(gene, all)
            is DisruptiveGene<*> -> mutateGene(gene.gene, all)
            is OptionalGene -> handleOptionalGene(gene, all)
            is IntegerGene -> handleIntegerGene(gene)
            is DoubleGene -> handleDoubleGene(gene)
            is StringGene -> handleStringGene(gene, all)
            else -> {
                gene.randomize(randomness, true, all)
            }
        }
    }

    private fun mutateGene(gene: Gene, all: List<Gene>) {
        when (gene) {
            is SqlForeignKeyGene -> handleSqlForeignKeyGene(gene, all)
            is DisruptiveGene<*> -> mutateGene(gene.gene, all)
            is OptionalGene -> handleOptionalGene(gene, all)
            is IntegerGene -> handleIntegerGene(gene)
            is DoubleGene -> handleDoubleGene(gene)
            is StringGene -> handleStringGene(gene, all)
            else -> {
                gene.randomize(randomness, true, all)
            }
        }
    }

    private fun handleSqlForeignKeyGene(gene: SqlForeignKeyGene, all: List<Gene>) {
        gene.randomize(randomness, true, all)
    }

    private fun handleStringGene(gene: StringGene, all: List<Gene>) {

        val p = randomness.nextDouble()
        val s = gene.value

        /*
            What type of mutations we do on Strings is strongly
            correlated on how we define the fitness functions.
            When dealing with equality, as we do left alignment,
            then it makes sense to prefer insertion/deletion at the
            end of the strings, and reward more "change" over delete/add
         */

        val others = all.flatMap { g -> g.flatView() }
                .filterIsInstance<StringGene>()
                .map { g -> g.value }
                .filter { it != gene.value }

        gene.value = when {
            //seeding: replace
            p < 0.02 && !others.isEmpty() -> {
                randomness.choose(others)
            }
            //change
            p < 0.8 && s.isNotEmpty() -> {
                val delta = getDelta(start = 6, end = 3)
                val sign = randomness.choose(listOf(-1, +1))
                val i = randomness.nextInt(s.length)
                val array = s.toCharArray()
                array[i] = s[i] + (sign * delta)
                String(array)
            }
            //delete last
            p < 0.9 && s.isNotEmpty() && s.length > gene.minLength -> {
                s.dropLast(1)
            }
            //append new
            s.length < gene.maxLength -> {
                if (s.isEmpty() || randomness.nextBoolean(0.8)) {
                    s + randomness.nextWordChar()
                } else {
                    val i = randomness.nextInt(s.length)
                    if (i == 0) {
                        randomness.nextWordChar() + s
                    } else {
                        s.substring(0, i) + randomness.nextWordChar() + s.substring(i, s.length)
                    }
                }
            }
            else -> {
                //do nothing
                s
            }
        }
    }


    protected fun handleOptionalGene(gene: OptionalGene, all: List<Gene>) {
        if (!gene.isActive) {
            gene.isActive = true
        } else {

            if (randomness.nextBoolean(0.01)) {
                gene.isActive = false
            } else {
                mutateGene(gene.gene, all)
            }
        }
    }

    private fun handleDoubleGene(gene: DoubleGene) {
        //TODO min/max for Double
        val d = gene.value
        gene.value = when (randomness.choose(listOf(0, 1, 2))) {
            //for small changes
            0 -> gene.value + randomness.nextGaussian()
            //for large jumps
            1 -> gene.value + (getDelta() * randomness.nextGaussian())
            //to reduce precision, ie chop off digits after the "."
            2 -> BigDecimal(gene.value).setScale(randomness.nextInt(15), RoundingMode.HALF_EVEN).toDouble()
            else -> throw IllegalStateException("Regression bug")
        }
    }

    private fun getDelta(
            range: Long = Long.MAX_VALUE,
            start: Int = intpow2.size,
            end: Int = 10
    ): Int {
        val maxIndex = apc.getExploratoryValue(start, end)

        var n = 0
        for (i in 0 until maxIndex) {
            n = i + 1
            if (intpow2[i] > range) {
                break
            }
        }

        //choose an i for 2^i modification
        val delta = randomness.chooseUpTo(intpow2, n)

        return delta
    }


    private fun handleIntegerGene(gene: IntegerGene) {
        val i = gene.value
        Lazy.assert { gene.min < gene.max && gene.isMutable() }

        //check maximum range. no point in having a delta greater than such range
        val range: Long = gene.max.toLong() - gene.min.toLong()

        //choose an i for 2^i modification
        val delta = getDelta(range)

        val sign = when (gene.value) {
            gene.max -> -1
            gene.min -> +1
            else -> randomness.choose(listOf(-1, +1))
        }

        val res: Long = (gene.value.toLong()) + (sign * delta)

        gene.value = when {
            res > gene.max -> gene.max
            res < gene.min -> gene.min
            else -> res.toInt()
        }
    }


    //archived mutation
    private fun handleDoubleGene(latest: DoubleGene, gene: DoubleGene) {
        val diff = gene.value - latest.value
        gene.value = gene.value + diff
    }

    private fun handleIntegerGene(latest: IntegerGene, gene: IntegerGene) {
        Lazy.assert { gene.min < gene.max && gene.isMutable() }

        //check maximum range. no point in having a delta greater than such range
        val range: Long = gene.max.toLong() - gene.min.toLong()

        //choose an i for 2^i modification
        val delta = getDelta(range)

        val sign = when {
            (gene.value > latest.value) -> +1
            else -> -1
        }

        val res: Long = (gene.value.toLong()) + (sign * delta)

        gene.value = when {
            res > gene.max -> gene.max
            res < gene.min -> gene.min
            else -> res.toInt()
        }
    }

    //TODO MAN: find how to change the string, and follow same kind of mutation
    private fun handleStringGene(latest: StringGene, gene: StringGene, all: List<Gene>) {

        val p = randomness.nextDouble()
        val s = gene.value

        /*
            What type of mutations we do on Strings is strongly
            correlated on how we define the fitness functions.
            When dealing with equality, as we do left alignment,
            then it makes sense to prefer insertion/deletion at the
            end of the strings, and reward more "change" over delete/add
         */

        val others = all.flatMap { g -> g.flatView() }
                .filterIsInstance<StringGene>()
                .map { g -> g.value }
                .filter { it != gene.value }

        gene.value = when {
            //seeding: replace
            p < 0.02 && !others.isEmpty() -> {
                randomness.choose(others)
            }
            //change
            p < 0.8 && s.isNotEmpty() -> {
                val delta = getDelta(start = 6, end = 3)
                val sign = randomness.choose(listOf(-1, +1))
                val i = randomness.nextInt(s.length)
                val array = s.toCharArray()
                array[i] = s[i] + (sign * delta)
                String(array)
            }
            //delete last
            p < 0.9 && s.isNotEmpty() && s.length > gene.minLength -> {
                s.dropLast(1)
            }
            //append new
            s.length < gene.maxLength -> {
                if (s.isEmpty() || randomness.nextBoolean(0.8)) {
                    s + randomness.nextWordChar()
                } else {
                    val i = randomness.nextInt(s.length)
                    if (i == 0) {
                        randomness.nextWordChar() + s
                    } else {
                        s.substring(0, i) + randomness.nextWordChar() + s.substring(i, s.length)
                    }
                }
            }
            else -> {
                //do nothing
                s
            }
        }
    }


    override fun getTrackOperator(): String {
        return StandardMutator::class.java.simpleName
    }
}