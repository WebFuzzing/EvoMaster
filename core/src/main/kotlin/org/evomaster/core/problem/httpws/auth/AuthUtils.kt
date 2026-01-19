package org.evomaster.core.problem.httpws.auth

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.auth.CookieWriter
import org.evomaster.core.output.auth.TokenWriter
import org.evomaster.core.problem.enterprise.auth.NoAuth
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.rest.data.ContentType
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.search.Individual
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.Response

object AuthUtils {

    private val log: Logger = LoggerFactory.getLogger(AuthUtils::class.java)


    fun getTokens(client: Client, baseUrl: String, ind: Individual): Map<String, String>{
        val tokensLogin = TokenWriter.getTokenLoginAuth(ind)
        return getTokens(client, baseUrl, tokensLogin)
    }

    /**
     * If any action needs auth based on tokens via JSON, do a "login" before
     * running the actions, and store the tokens
     */
    fun getTokens(client: Client, baseUrl: String, tokensLogin: List<EndpointCallLogin>): Map<String, String>{

        //from userId to Token
        val map = mutableMapOf<String, String>()

        for(tl in tokensLogin){

            if(tl.expectsCookie()){
                throw IllegalArgumentException("Token based login does not expect cookies")
            }
            val data = tl.token ?: throw IllegalArgumentException("Token based login requires token definition")

            val response = makeCall(client, tl, baseUrl)
                ?: continue

            var token = when(data.extractFrom){
                TokenHandling.ExtractFrom.BODY -> {
                    if(! response.hasEntity()){
                        log.warn("Login request failed, with no body response from which to extract the auth token")
                        continue
                    }

                    val body = response.readEntity(String::class.java)
                    response.close()

                    val jackson = ObjectMapper()
                    val tree = jackson.readTree(body)
                    val token = tree.at(tl.token!!.extractSelector).asText()
                    if(token == null || token.isEmpty()){
                        log.warn("Failed login. Cannot extract token '${data.extractSelector}' from response: $body")
                        continue
                    }
                    token
                }
                TokenHandling.ExtractFrom.HEADER -> {
                    val header = response.getHeaderString(data.extractSelector)
                    if(header == null || header.isEmpty()){
                        log.warn("Failed login. No token to extract from header '${data.extractSelector}'")
                        continue
                    }
                    header
                }
            }

            if(data.sendTemplate.isNotEmpty()){
                token = data.sendTemplate.replace("{token}",  token)
            }

            map[tl.name] = token
        }

        return map
    }


    fun getCookies(client: Client, baseUrl: String, ind: Individual): Map<String, List<NewCookie>> {

        val cookieLogins = CookieWriter.getCookieLoginAuth(ind)
        return getCookies(client, baseUrl, cookieLogins)
    }

    /**
     * If any action needs auth based on cookies, do a "login" before
     * running the actions, and collect the cookies from the server.
     *
     * @return a map from username to auth cookie for those users
     */
    fun getCookies(client: Client, baseUrl: String, cookieLogins: List<EndpointCallLogin>): Map<String, List<NewCookie>> {

        val map: MutableMap<String, List<NewCookie>> = mutableMapOf()

        for (cl in cookieLogins) {

            if(!cl.expectsCookie()){
                throw IllegalArgumentException("Cookie based login expects cookies")
            }


            val response = makeCall(client, cl, baseUrl)
                ?: continue
            response.close()

            if (response.cookies.isEmpty()) {
                log.warn("Cookie-based login request did not give back any new cookie")
                continue
            }

            map[cl.name] = response.cookies.values.toList()
        }

        return map
    }



    private fun makeCall(client: Client, x: EndpointCallLogin, baseUrl: String) : Response?{

        val mediaType = when (x.contentType) {
            ContentType.X_WWW_FORM_URLENCODED -> MediaType.APPLICATION_FORM_URLENCODED_TYPE
            ContentType.JSON -> MediaType.APPLICATION_JSON_TYPE
            null -> null
        }

        val bodyEntity = if(mediaType != null) {
            Entity.entity(x.payload, mediaType)
        } else {
            null
        }

        val builder =  client.target(x.getUrl(baseUrl)).request()

        x.headers.forEach { builder.header(it.name, it.value) }

        if(mediaType!=null){
            builder.header("Content-Type", mediaType)
        }

        //TODO duplicated code, should put in a utility
        val invocation = if(bodyEntity != null) {
            when (x.verb) {
                HttpVerb.GET -> builder.buildGet()
                HttpVerb.DELETE -> builder.build("DELETE", bodyEntity)
                HttpVerb.POST -> builder.buildPost(bodyEntity)
                HttpVerb.PUT -> builder.buildPut(bodyEntity)
                HttpVerb.PATCH -> builder.build("PATCH", bodyEntity)
                HttpVerb.OPTIONS -> builder.build("OPTIONS")
                HttpVerb.HEAD -> builder.build("HEAD")
                HttpVerb.TRACE -> builder.build("TRACE")
            }
        } else {
            builder.build(x.verb.toString())
        }

        val response = try {
            invocation.invoke()
        } catch (e: Exception) {
            log.warn("Failed to login for ${x.name}: $e")
            return null
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
                    response.close()
                    return null
                }
            } else {
                log.warn("Login request failed with status ${response.status}")
                response.close()
                return null
            }
        }

        return response
    }

    fun addAuthHeaders(
        auth: HttpWsAuthenticationInfo,
        builder: Invocation.Builder,
        cookies: Map<String, List<NewCookie>>,
        tokens: Map<String, String>
    ) {
        auth.headers.forEach {
            builder.header(it.name, it.value)
        }

        val ecl = auth.endpointCallLogin

        if (ecl != null) {
            if (ecl.expectsCookie()) {
                val list = cookies[ecl.name]
                if (list.isNullOrEmpty()) {
                    log.warn("No cookies for ${ecl.name}")
                } else {
                    list.forEach {
                        builder.cookie(it.toCookie())
                    }
                }
            } else {
                val token = tokens[ecl.name]
                if (token.isNullOrEmpty()) {
                    log.warn("No auth token for ${ecl.name}")
                } else {
                    builder.header(ecl.token!!.sendName, token)
                }
            }
        }
    }

    /**
     * Check if we have valid credentials, but got 401
     */
     fun checkUnauthorizedWithAuth(status: Int, a: HttpWsAction) : Boolean{
        if (status == 401 && a.auth !is NoAuth && !a.auth.requireMockHandling) {
            /*
                if the endpoint itself is to get auth info, we might exclude auth check for it
                eg,
                    the auth is Login with foo,
                    then the action is to Login with a generated account (eg bar)
                    thus, the response would likely be 401
             */
            if (!a.auth.excludeAuthCheck(a)) {
                /*
                    this would likely be a misconfiguration in the SUT controller.
                    However, there could be at least 2 cases in which it could happen
                    1) reusing tokens between tests for performance reasons
                    2) wrongly returning 401 instead of 403
                 */
                val endpoint = when (a) {
                    is RestCallAction -> a.path.toString()
                    is GraphQLAction -> a.methodName
                    else -> "undefined"
                }
                LoggingUtil.uniqueWarn(log, "Got 401 although having auth for '${a.auth.name}' on endpoint: $endpoint")
                return false
            }
        }
        return true
    }


}