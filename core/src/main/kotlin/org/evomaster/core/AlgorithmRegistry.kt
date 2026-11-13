package org.evomaster.core

import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.mcp.McpIndividual
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.webfrontend.WebIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.BreederGeneticAlgorithm
import org.evomaster.core.search.algorithms.CellularGeneticAlgorithm
import org.evomaster.core.search.algorithms.CroAlgorithm
import org.evomaster.core.search.algorithms.LIPSAlgorithm
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.MonotonicGeneticAlgorithm
import org.evomaster.core.search.algorithms.MosaAlgorithm
import org.evomaster.core.search.algorithms.MuLambdaEvolutionaryAlgorithm
import org.evomaster.core.search.algorithms.MuPlusLambdaEvolutionaryAlgorithm
import org.evomaster.core.search.algorithms.OnePlusLambdaLambdaGeneticAlgorithm
import org.evomaster.core.search.algorithms.RandomAlgorithm
import org.evomaster.core.search.algorithms.RandomWalkAlgorithm
import org.evomaster.core.search.algorithms.SmartsAlgorithm
import org.evomaster.core.search.algorithms.StandardGeneticAlgorithm
import org.evomaster.core.search.algorithms.SteadyStateGeneticAlgorithm
import org.evomaster.core.search.algorithms.WtsAlgorithm
import org.evomaster.core.search.service.SearchAlgorithm
import kotlin.reflect.KClass
import com.google.inject.Key
import com.google.inject.util.Types

object AlgorithmRegistry {

    private val algorithms: Map<EMConfig.Algorithm, KClass<out SearchAlgorithm<*>>> = mapOf(
        EMConfig.Algorithm.SMARTS to SmartsAlgorithm::class,
        EMConfig.Algorithm.RANDOM to RandomAlgorithm::class,
        EMConfig.Algorithm.MIO to MioAlgorithm::class,
        EMConfig.Algorithm.WTS to WtsAlgorithm::class,
        EMConfig.Algorithm.MOSA to MosaAlgorithm::class,
        EMConfig.Algorithm.RW to RandomWalkAlgorithm::class,
        EMConfig.Algorithm.StandardGA to StandardGeneticAlgorithm::class,
        EMConfig.Algorithm.MonotonicGA to MonotonicGeneticAlgorithm::class,
        EMConfig.Algorithm.SteadyStateGA to SteadyStateGeneticAlgorithm::class,
        EMConfig.Algorithm.LIPS to LIPSAlgorithm::class,
        EMConfig.Algorithm.MuPlusLambdaEA to MuPlusLambdaEvolutionaryAlgorithm::class,
        EMConfig.Algorithm.MuLambdaEA to MuLambdaEvolutionaryAlgorithm::class,
        EMConfig.Algorithm.BreederGA to BreederGeneticAlgorithm::class,
        EMConfig.Algorithm.CellularGA to CellularGeneticAlgorithm::class,
        EMConfig.Algorithm.OnePlusLambdaLambdaGA to OnePlusLambdaLambdaGeneticAlgorithm::class,
        EMConfig.Algorithm.CRO to CroAlgorithm::class
    )

    private val individualTypes: Map<EMConfig.ProblemType, KClass<out Individual>> = mapOf(
        EMConfig.ProblemType.REST       to RestIndividual::class,
        EMConfig.ProblemType.GRAPHQL    to GraphQLIndividual::class,
        EMConfig.ProblemType.RPC        to RPCIndividual::class,
        EMConfig.ProblemType.WEBFRONTEND to WebIndividual::class,
        EMConfig.ProblemType.MCP        to McpIndividual::class,
        EMConfig.ProblemType.ASYNCAPI   to AsyncApiIndividual::class,
    )

    fun key(config: EMConfig): Key<out SearchAlgorithm<*>> {
        return key(config.algorithm, config.problemType)
    }

    fun key(algorithm: EMConfig.Algorithm, problemType: EMConfig.ProblemType): Key<out SearchAlgorithm<*>> {
        val algorithmClass = algorithms[algorithm]
            ?: throw IllegalStateException("Unrecognized algorithm $algorithm")
        val individualClass = individualTypes[problemType]
            ?: throw IllegalStateException("Unrecognized problem type $problemType")

        val type = Types.newParameterizedType(algorithmClass.java, individualClass.java)
        return Key.get(type) as Key<out SearchAlgorithm<*>>
    }

    fun keys(problemType: EMConfig.ProblemType): List<Key<out SearchAlgorithm<*>>> {
        return algorithms.map { key(it.key, problemType) }
    }
}