package org.evomaster.core.problem.graphql.service

import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.graphql.*
import org.evomaster.core.problem.graphql.builder.GraphQLActionBuilder
import org.evomaster.core.problem.httpws.service.HttpWsSampler
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.remote.SutProblemException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/*
    TODO: some code here will need to be refactored out with AbstractRestSampler to avoid duplication
 */

class GraphQLSampler : HttpWsSampler<GraphQLIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GraphQLSampler::class.java)
    }


    @PostConstruct
    open fun initialize() {

        log.debug("Initializing {}", GraphQLSampler::class.simpleName)

        if(config.blackBox && !config.bbExperiments){
            initForBlackBox()
            return
        }

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        val infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        var gqlEndpoint = infoDto.graphQLProblem?.endpoint
                ?: throw IllegalStateException("Missing information about the GraphQL endpoint URL")

        if(! gqlEndpoint.startsWith("http", true)){
            gqlEndpoint = infoDto.baseUrlOfSUT + gqlEndpoint
        }
        /*
                Configuration of the headers
                */
        val headers = listOf(config.header0, config.header1, config.header2)
            .filter { it.isNotBlank() }

        val iq = IntrospectiveQuery()
        val schema = iq.fetchSchema(gqlEndpoint, headers)

        actionCluster.clear()
        //val skip = getEndpointsToSkip(swagger, infoDto) //TODO maybe in future wants to support

        if (schema != null) {
            GraphQLActionBuilder.addActionsFromSchema(schema, actionCluster, config.treeDepth)
        }

        setupAuthentication(infoDto)

        //TODO this will require refactoring
        initSqlInfo(infoDto)
        //initAdHocInitialIndividuals()
        //postInits()

        updateConfigBasedOnSutInfoDto(infoDto)

        if (config.seedTestCases)
            initSeededTests()

        log.debug("Done initializing {}", GraphQLSampler::class.simpleName)
    }

    private fun initForBlackBox() {
        val gqlEndpoint = config.bbTargetUrl

        /*
         Configuration of the headers
         */
        val headers = listOf(config.header0, config.header1, config.header2)
            .filter { it.isNotBlank() }

        val iq = IntrospectiveQuery()
        val schema = iq.fetchSchema(gqlEndpoint, headers)

        addAuthFromConfig()

        actionCluster.clear()

        if (schema != null) {
            GraphQLActionBuilder.addActionsFromSchema(schema, actionCluster, config.treeDepth)
        }
    }


    override fun sampleAtRandom(): GraphQLIndividual {
        val actions = mutableListOf<EnterpriseActionGroup<*>>()
        val n = randomness.nextInt(1, getMaxTestSizeDuringSampler())

        (0 until n).forEach {
            val a = sampleRandomAction(0.05) as GraphQLAction
            actions.add(EnterpriseActionGroup(mutableListOf(a),GraphQLAction::class.java))
        }
        val ind =  GraphQLIndividual(SampleType.RANDOM, actions)
        GraphQLUtils.repairIndividual(ind)
        ind.doGlobalInitialize(searchGlobalState)

        return ind
    }

    /*
        TODO smart sampling, in which we have only a single Query at the end, and variable
        number of Mutation before
     */

    override fun initSeededTests(infoDto: SutInfoDto?) {
        // not supported yet
    }

}
