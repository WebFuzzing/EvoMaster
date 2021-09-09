package org.evomaster.core.problem.graphql.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.problem.graphql.*
import org.evomaster.core.problem.httpws.service.HttpWsSampler
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.service.AbstractRestSampler
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
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

    @Inject(optional = true)
    private lateinit var rc: RemoteController


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

        val iq = IntrospectiveQuery()
        val schema = iq.fetchSchema(gqlEndpoint)

        actionCluster.clear()
        //val skip = getEndpointsToSkip(swagger, infoDto) //TODO maybe in future wants to support

        GraphQLActionBuilder.addActionsFromSchema(schema, actionCluster)

        setupAuthentication(infoDto)

        //TODO this will require refactoring
        initSqlInfo(infoDto)
        //initAdHocInitialIndividuals()
        //postInits()

        updateConfigForTestOutput(infoDto)

        log.debug("Done initializing {}", AbstractRestSampler::class.simpleName)
    }

    private fun initForBlackBox() {
        val gqlEndpoint = config.bbTargetUrl

        val iq = IntrospectiveQuery()
        val schema = iq.fetchSchema(gqlEndpoint)

        actionCluster.clear()

        GraphQLActionBuilder.addActionsFromSchema(schema, actionCluster)
    }


    override fun sampleAtRandom(): GraphQLIndividual {
        val actions = mutableListOf<GraphQLAction>()
        val n = randomness.nextInt(1, config.maxTestSize)

        (0 until n).forEach {
            actions.add(sampleRandomAction(0.05) as GraphQLAction)
        }
        val ind =  GraphQLIndividual(actions, SampleType.RANDOM, mutableListOf())
        GraphQLUtils.repairIndividual(ind)
        return ind
    }

    /*
        TODO smart sampling, in which we have only a single Query at the end, and variable
        number of Mutation before
     */

    override fun initSqlInfo(infoDto: SutInfoDto) {
        if (infoDto.sqlSchemaDto != null && config.shouldGenerateSqlData()) {

            sqlInsertBuilder = SqlInsertBuilder(infoDto.sqlSchemaDto, rc)
            existingSqlData = sqlInsertBuilder!!.extractExistingPKs()
        }
    }
}