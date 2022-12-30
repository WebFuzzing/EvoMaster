package org.evomaster.core.problem.httpws.service

import org.evomaster.client.java.controller.api.dto.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.HeaderDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.problem.api.service.ApiWsSampler
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.auth.AuthenticationHeader
import org.evomaster.core.problem.httpws.auth.CookieLogin
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.httpws.auth.JsonTokenPostLogin
import org.evomaster.core.problem.httpws.auth.NoAuth
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.Individual
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Common code shared among sampler that rely on a RemoteController, and
 * that use HTTP, ie typically Web Services like REST and GraphQL
 */
abstract class HttpWsSampler<T> : ApiWsSampler<T>() where T : Individual{

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HttpWsSampler::class.java)
    }


    protected val authentications: MutableList<HttpWsAuthenticationInfo> = mutableListOf()



    /**
     * Given the current schema definition, create a random action among the available ones.
     * All the genes in such action will have their values initialized at random, but still within
     * their given constraints (if any, e.g., a day number being between 1 and 12).
     *
     * @param noAuthP the probability of having an HTTP call without any authentication header.
     */
    fun sampleRandomAction(noAuthP: Double): HttpWsAction {
        val action = randomness.choose(actionCluster).copy() as HttpWsAction
        action.doInitialize(randomness)
        action.auth = getRandomAuth(noAuthP)
        return action
    }

    fun getRandomAuth(noAuthP: Double): HttpWsAuthenticationInfo {
        if (authentications.isEmpty() || randomness.nextBoolean(noAuthP)) {
            return NoAuth()
        } else {
            //if there is auth, should have high probability of using one,
            //as without auth we would do little.
            return randomness.choose(authentications)
        }
    }


    protected fun addAuthFromConfig(){

        val headers = listOf(config.header0, config.header1, config.header2)
                .filter { it.isNotBlank() }

        if(headers.isEmpty()){
            return //nothing to do
        }

        val dto = AuthenticationDto()
        headers.forEach {
            val k = it.indexOf(":")
            val name = it.substring(0, k)
            val content = it.substring(k+1)
            dto.headers.add(HeaderDto(name, content))
        }

        dto.name = "Fixed Headers"

        handleAuthInfo(dto)
    }

    protected fun setupAuthentication(infoDto: SutInfoDto) {

        addAuthFromConfig()

        val info = infoDto.infoForAuthentication ?: return

        info.forEach { handleAuthInfo(it) }
    }

    private fun handleAuthInfo(i: AuthenticationDto) {
        if (i.name == null || i.name.isBlank()) {
            throw SutProblemException("Missing name in authentication info")
        }

        val headers: MutableList<AuthenticationHeader> = mutableListOf()

        i.headers.forEach loop@{ h ->
            val name = h.name?.trim()
            val value = h.value?.trim()
            if (name == null || value == null) {
                throw SutProblemException("Invalid header in ${i.name}, $name:$value")
            }

            headers.add(AuthenticationHeader(name, value))
        }

        val cookieLogin = if (i.cookieLogin != null) {
            CookieLogin.fromDto(i.cookieLogin)
        } else {
            null
        }

        val jsonTokenPostLogin = if (i.jsonTokenPostLogin != null) {
            JsonTokenPostLogin.fromDto(i.jsonTokenPostLogin)
        } else {
            null
        }


        val auth = HttpWsAuthenticationInfo(i.name.trim(), headers, cookieLogin, jsonTokenPostLogin)

        authentications.add(auth)
        return
    }


}