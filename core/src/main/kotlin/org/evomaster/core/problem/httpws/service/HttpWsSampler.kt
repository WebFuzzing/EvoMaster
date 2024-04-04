package org.evomaster.core.problem.httpws.service

import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.auth.HeaderDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.problem.api.service.ApiWsSampler
import org.evomaster.core.problem.enterprise.auth.AuthSettings
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.auth.*
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

    //TODO move up to Enterprise
    val authentications = AuthSettings()


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

        val selection = authentications.getOfType(HttpWsAuthenticationInfo::class.java)

        return if (selection.isEmpty() || randomness.nextBoolean(noAuthP)) {
            HttpWsNoAuth()
        } else {
            //if there is auth, should have high probability of using one,
            //as without auth we would do little.
            randomness.choose(selection)
        }
    }


    protected fun addAuthFromConfig(){

        //first check if any configured in configuration file (if any)
        config.authFromFile?.forEach { handleAuthInfo(it) }

        //then check if any is passed on commandline
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
            dto.fixedHeaders.add(HeaderDto(name, content))
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

        val auth = try{
            HttpWsAuthenticationInfo.fromDto(i)
        }catch (e: Exception){
            throw SutProblemException("Failed to parse auth info: " + e.message!!)
        }

        authentications.addInfo(auth)
        return
    }


}