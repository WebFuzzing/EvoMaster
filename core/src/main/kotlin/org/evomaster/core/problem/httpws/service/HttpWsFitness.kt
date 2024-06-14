package org.evomaster.core.problem.httpws.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.core.StaticCounter
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.auth.CookieWriter
import org.evomaster.core.output.auth.TokenWriter
import org.evomaster.core.problem.api.service.ApiWsFitness
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.httpws.auth.AuthUtils
import org.evomaster.core.problem.httpws.auth.EndpointCallLogin
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.remote.HttpClientFactory
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.Individual
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL
import javax.annotation.PostConstruct
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.Response

abstract class HttpWsFitness<T>: ApiWsFitness<T>() where T : Individual {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HttpWsFitness::class.java)

        init{
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        }
    }

    protected lateinit var client : Client


    @PostConstruct
    protected fun initialize() {

        log.debug("Initializing {}", HttpWsFitness::class.simpleName)

        client = HttpClientFactory.createTrustingJerseyClient(false, config.tcpTimeoutMs)

        if (!config.blackBox || config.bbExperiments) {
            rc.checkConnection()

            val started = rc.startSUT()
            if (!started) {
                throw SutProblemException("Failed to start the system under test")
            }

            infoDto = rc.getSutInfo()
                    ?: throw SutProblemException("Failed to retrieve the info about the system under test")
        }

        log.debug("Done initializing {}", HttpWsFitness::class.simpleName)
    }

    override fun reinitialize(): Boolean {

        try {
            if (!config.blackBox) {
                rc.stopSUT()
            }
            initialize()
        } catch (e: Exception) {
            log.warn("Failed to re-initialize the SUT: $e")
            return false
        }

        return true
    }



    open fun getlocation5xx(status: Int, additionalInfoList: List<AdditionalInfoDto>, indexOfAction: Int, result: HttpWsCallResult, name: String) : String?{
        var location5xx : String? = null
        if (status == 500){
            val statement = additionalInfoList[indexOfAction].lastExecutedStatement
            location5xx = statement ?: DEFAULT_FAULT_CODE
            result.setLastStatementWhen500(location5xx)
        }
        return location5xx
    }

    protected fun getBaseUrl(): String {
        var baseUrl = if (!config.blackBox || config.bbExperiments) {
            infoDto.baseUrlOfSUT
        } else {
            BlackBoxUtils.targetUrl(config, sampler)
        }

        try{
            /*
                Note: this in theory should already been checked: either in EMConfig for
                Black-box testing, or already in the driver for White-Box testing
             */
            URL(baseUrl)
        } catch (e: MalformedURLException){
            val base = "Invalid 'baseUrl'."
            val wb = "In the EvoMaster driver, in the startSut() method, you must make sure to return a valid URL."
            val err = " ERROR: $e"

            val msg = if(config.blackBox) "$base $err" else "$base $wb $err"
            throw SutProblemException(msg)
        }

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 1)
        }

        return baseUrl
    }




    @Deprecated("replaced by doDbCalls()")
    open fun doInitializingActions(ind: ApiWsIndividual) {

        if (log.isTraceEnabled){
            log.trace("do {} InitializingActions: {}", ind.seeInitializingActions().size,
                ind.seeInitializingActions().filterIsInstance<SqlAction>().joinToString(","){
                    it.getResolvedName()
                })
        }

        if (ind.seeInitializingActions().filterIsInstance<SqlAction>().none { !it.representExistingData }) {
            /*
                We are going to do an initialization of database only if there
                is data to add.
                Note that current data structure also keeps info on already
                existing data (which of course should not be re-inserted...)
             */
            return
        }

        val dto = SqlActionTransformer.transform(ind.seeInitializingActions().filterIsInstance<SqlAction>())
        dto.idCounter = StaticCounter.getAndIncrease()

        val ok = rc.executeDatabaseCommand(dto)
        if (!ok) {
            //this can happen if we do not handle all constraints
            LoggingUtil.uniqueWarn(log, "Failed in executing database command")
        }
    }



    protected fun handleHeaders(a: HttpWsAction, builder: Invocation.Builder, cookies: Map<String, List<NewCookie>>, tokens: Map<String, String>) {

        val prechosenAuthHeaders = a.auth.headers.map { it.name }

        val tokenHeader = a.auth.endpointCallLogin?.token?.httpHeaderName ?: null

        /*
            TODO: optimization, avoid mutating header gene if anyway
            using pre-chosen one
         */
        a.parameters.filterIsInstance<HeaderParam>()
                //TODO those should be skipped directly in the search, ie, right now they are useless genes
                .filter { !prechosenAuthHeaders.contains(it.name) }
                .filter { !(tokenHeader!=null && it.name.equals(tokenHeader, true)) }
                .filter{ it.isInUse()}
                .forEach {
                    builder.header(it.name, it.getRawValue())
                }

        AuthUtils.addAuthHeaders(a.auth, builder, cookies, tokens)
    }


}