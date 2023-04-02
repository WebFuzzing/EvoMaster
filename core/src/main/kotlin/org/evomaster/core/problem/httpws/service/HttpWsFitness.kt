package org.evomaster.core.problem.httpws.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.core.StaticCounter
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.CookieWriter
import org.evomaster.core.output.TokenWriter
import org.evomaster.core.problem.api.service.ApiWsFitness
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL
import javax.annotation.PostConstruct
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
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

        val clientConfiguration = ClientConfig()
                .property(ClientProperties.CONNECT_TIMEOUT, 10_000)
                .property(ClientProperties.READ_TIMEOUT, config.tcpTimeoutMs)
                //workaround bug in Jersey client
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
                .property(ClientProperties.FOLLOW_REDIRECTS, false)

        client = ClientBuilder.newClient(clientConfiguration)

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

    /**
     * If any action needs auth based on tokens via JSON, do a "login" before
     * running the actions, and store the tokens
     */
    protected fun getTokens(ind: T): Map<String, String>{

        val tokensLogin = TokenWriter.getTokenLoginAuth(ind)

        //from userId to Token
        val map = mutableMapOf<String, String>()

        val baseUrl = getBaseUrl()

        for(tl in tokensLogin){

            val response = try {
                client.target(baseUrl + tl.endpoint)
                        .request()
                        .buildPost(Entity.entity(tl.jsonPayload, MediaType.APPLICATION_JSON_TYPE))
                        .invoke()
            } catch (e: Exception) {
                log.warn("Failed to login for ${tl.userId}: $e")
                continue
            }

            if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {
                log.warn("Login request failed with status ${response.status}")
                continue
            }

            if(! response.hasEntity()){
                log.warn("Login request failed, with no body response from which to extract the auth token")
                continue
            }

            val body = response.readEntity(String::class.java)
            val jackson = ObjectMapper()
            val tree = jackson.readTree(body)
            var token = tree.at(tl.extractTokenField).asText()
            if(token == null || token.isEmpty()){
                log.warn("Failed login. Cannot extract token '${tl.extractTokenField}' from response: $body")
                continue
            }

            if(tl.headerPrefix.isNotEmpty()){
                token = tl.headerPrefix + token
            }

            map[tl.userId] = token
        }

        return map
    }

    /**
     * If any action needs auth based on cookies, do a "login" before
     * running the actions, and collect the cookies from the server.
     *
     * @return a map from username to auth cookie for those users
     */
    protected fun getCookies(ind: T): Map<String, List<NewCookie>> {

        val cookieLogins = CookieWriter.getCookieLoginAuth(ind)

        val map: MutableMap<String, List<NewCookie>> = mutableMapOf()

        val baseUrl = getBaseUrl()

        for (cl in cookieLogins) {

            val mediaType = when (cl.contentType) {
                ContentType.X_WWW_FORM_URLENCODED -> MediaType.APPLICATION_FORM_URLENCODED_TYPE
                ContentType.JSON -> MediaType.APPLICATION_JSON_TYPE
            }

            val response = try {
                client.target(cl.getUrl(baseUrl))
                        .request()
                        //TODO could consider other cases besides POST
                        .buildPost(Entity.entity(cl.payload(), mediaType))
                        .invoke()
            } catch (e: Exception) {
                log.warn("Failed to login for ${cl.username}/${cl.password}: $e")
                continue
            }

            if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {

                /*
                    if it is a 3xx, we need to look at Location header to determine
                    if a success or failure.
                    TODO: could explicitly ask for this info in the auth DTO.
                    However, as 3xx makes little sense in a REST API, maybe not so
                    important right now, although had this issue with some APIs using
                    default settings in Spring Security
                */
                if (response.statusInfo.family == Response.Status.Family.REDIRECTION) {
                    val location = response.getHeaderString("location")
                    if (location != null && (location.contains("error", true) || location.contains("login", true))) {
                        log.warn("Login request failed with ${response.status} redirection toward $location")
                        continue
                    }
                } else {
                    log.warn("Login request failed with status ${response.status}")
                    continue
                }
            }

            if (response.cookies.isEmpty()) {
                log.warn("Cookie-based login request did not give back any new cookie")
                continue
            }

            map[cl.username] = response.cookies.values.toList()
        }

        return map
    }





    @Deprecated("replaced by doDbCalls()")
    open fun doInitializingActions(ind: ApiWsIndividual) {

        if (log.isTraceEnabled){
            log.trace("do {} InitializingActions: {}", ind.seeInitializingActions().size,
                ind.seeInitializingActions().filterIsInstance<DbAction>().joinToString(","){
                    it.getResolvedName()
                })
        }

        if (ind.seeInitializingActions().filterIsInstance<DbAction>().none { !it.representExistingData }) {
            /*
                We are going to do an initialization of database only if there
                is data to add.
                Note that current data structure also keeps info on already
                existing data (which of course should not be re-inserted...)
             */
            return
        }

        val dto = DbActionTransformer.transform(ind.seeInitializingActions().filterIsInstance<DbAction>())
        dto.idCounter = StaticCounter.getAndIncrease()

        val ok = rc.executeDatabaseCommand(dto)
        if (!ok) {
            //this can happen if we do not handle all constraints
            LoggingUtil.uniqueWarn(log, "Failed in executing database command")
        }
    }



    protected fun handleHeaders(a: HttpWsAction, builder: Invocation.Builder, cookies: Map<String, List<NewCookie>>, tokens: Map<String, String>) {
        a.auth.headers.forEach {
            builder.header(it.name, it.value)
        }

        val prechosenAuthHeaders = a.auth.headers.map { it.name }

        /*
            TODO: optimization, avoid mutating header gene if anyway
            using pre-chosen one
         */

        a.parameters.filterIsInstance<HeaderParam>()
                //TODO those should be skipped directly in the search, ie, right now they are useless genes
                .filter { !prechosenAuthHeaders.contains(it.name) }
                .filter { !(a.auth.jsonTokenPostLogin != null && it.name.equals("Authorization", true)) }
                .filter{ it.isInUse()}
                .forEach {
                    builder.header(it.name, it.gene.getValueAsRawString())
                }

        if (a.auth.cookieLogin != null) {
            val list = cookies[a.auth.cookieLogin!!.username]
            if (list == null || list.isEmpty()) {
                log.warn("No cookies for ${a.auth.cookieLogin!!.username}")
            } else {
                list.forEach {
                    builder.cookie(it.toCookie())
                }
            }
        }

        if (a.auth.jsonTokenPostLogin != null) {
            val token = tokens[a.auth.jsonTokenPostLogin!!.userId]
            if (token == null || token.isEmpty()) {
                log.warn("No auth token for ${a.auth.jsonTokenPostLogin!!.userId}")
            } else {
                builder.header("Authorization", token)
            }
        }
    }
}