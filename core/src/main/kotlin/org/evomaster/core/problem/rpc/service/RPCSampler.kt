package org.evomaster.core.problem.rpc.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.httpws.service.HttpWsSampler
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/**
 * created by manzhang on 2021/11/25
 */
class RPCSampler: HttpWsSampler<RPCIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RPCSampler::class.java)
    }

    @Inject
    protected lateinit var configuration: EMConfig

    @Inject(optional = true)
    protected lateinit var rc: RemoteController

    @Inject
    protected lateinit var convertor: RPCDtoConvertor

    @PostConstruct
    fun initialize() {
        log.debug("Initializing {}", RPCSampler::class.simpleName)

        if(configuration.blackBox && !configuration.bbExperiments){
            throw IllegalStateException("Not support black-box testing for RPC with interface")
        }

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }



        val infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        val problem = infoDto.rpcProblem
                ?: throw java.lang.IllegalStateException("Missing problem definition object")

        convertor.initActionCluster(problem, actionCluster)

        //TODO Man: need to check whether we need following for RPC

//        setupAuthentication(infoDto)
//        initSqlInfo(infoDto)
//
//        initAdHocInitialIndividuals()
//
//        postInits()
//
//        updateConfigForTestOutput(infoDto)
//
//        partialOracles.setupForRest(swagger)

        log.debug("Done initializing {}", RPCSampler::class.simpleName)

    }

    override fun initSqlInfo(infoDto: SutInfoDto) {
        TODO("Not yet implemented")
    }

    override fun sampleAtRandom(): RPCIndividual {
        TODO("Not yet implemented")
    }
}