package org.evomaster.core.problem.httpws.service

import com.webfuzzing.commons.auth.Header
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.AnsiColor
import org.evomaster.core.DocumentationLinks.EM_AUTH_LINK
import org.evomaster.core.logging.LoggingUtil
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
    open fun sampleRandomAction(noAuthP: Double): HttpWsAction {
        val action = randomness.choose(actionCluster).copy() as HttpWsAction
        action.doInitialize(randomness)
        action.auth = getRandomAuth(noAuthP)
        action.forceNewTaints()
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


    private fun addAuthFromConfig(){

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
            val header = Header()
            header.name = name
            header.value = content
            dto.fixedHeaders.add(header)
        }

        dto.name = "Fixed Headers"

        handleAuthInfo(dto)
    }

    private fun checkAuthSettings(){
        val n = authentications.size()
        if(n==0){
            LoggingUtil.getInfoLogger().warn(
                AnsiColor.inYellow("WARNING: No authentication info was provided." +
                    " Unless you are testing an example API, you should setup some authentication info for different users." +
                    " If this is the first time you are using EvoMaster, and you just want to get a feeling of how it works," +
                    " then ignore this warning." +
                    " However, to get better results, you will need setup authentication info, eventually." +
                    " More info is currently available at ") + AnsiColor.inBlue(EM_AUTH_LINK)
            )
        }
        if(n==1){
            //TODO if/when in the future we enable dynamic registration of users, likely we will need to update this
            // warning message
            LoggingUtil.getInfoLogger().warn(
                AnsiColor.inYellow("WARNING: You have provided authentication information only for a single user." +
                        " Many of the automatic checks done by EvoMaster for access policy validation are based on the" +
                        " interactions of 2 or more users." +
                        " To get better results, you are strongly recommended to provide more user authentication info," +
                        " at the very minimum 2 in total, but better if at least 1 for each different access role you have in your system" +
                        " that you are testing." +
                        " More info is currently available at ") + AnsiColor.inBlue(EM_AUTH_LINK)
            )
        }
    }

    protected fun setupAuthenticationForBlackBox(){
        addAuthFromConfig()
        checkAuthSettings()
    }

    protected fun setupAuthenticationForWhiteBox(infoDto: SutInfoDto) {

        addAuthFromConfig()

        val info = infoDto.infoForAuthentication ?: return

        info.forEach { handleAuthInfo(it) }

        checkAuthSettings()
    }

    private fun handleAuthInfo(i: AuthenticationDto) {

        val auth = try{
            HttpWsAuthenticationInfo.fromDto(i, config.overrideAuthExternalEndpointURL)
        }catch (e: Exception){
            throw SutProblemException("Failed to parse auth info: " + e.message!!)
        }

        authentications.addInfo(auth)
        return
    }


}
