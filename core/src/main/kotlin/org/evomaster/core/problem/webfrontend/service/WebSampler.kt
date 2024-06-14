package org.evomaster.core.problem.webfrontend.service

import org.evomaster.client.java.controller.api.SeleniumEMUtils
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.problem.webfrontend.WebAction
import org.evomaster.core.problem.webfrontend.WebIndividual
import org.evomaster.core.remote.SutProblemException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct
import javax.inject.Inject

class WebSampler : EnterpriseSampler<WebIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(WebSampler::class.java)
    }

    @Inject
    private lateinit var browserController: BrowserController



    @PostConstruct
    open fun initialize() {

        log.debug("Initializing {}", WebSampler::class.simpleName)

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        val infoDto = rc.getSutInfo()
            ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        val startingPage = infoDto.webProblem.urlPathOfStartingPage
            ?: throw SutProblemException("Not specified urlPathOfStartingPage")

        if(startingPage.startsWith("http")){
            LoggingUtil.uniqueUserWarn("The urlPathOfStartingPage you provided starts with 'http'." +
                    " You sure you provided a path instead of a whole URL?")
        }

        val url = SeleniumEMUtils.combineBaseUrlAndUrlPath(infoDto.baseUrlOfSUT, startingPage)
        try {
            browserController.initUrlOfStartingPage(url, true)
        } catch (e: IllegalArgumentException){
            throw SutProblemException("Issue with inferred URL for home page: $url\n${e.message}")
        }
        browserController.startChromeInDocker()

        LoggingUtil.getInfoLogger().info("Home page of tested application -> $url")

       // setupAuthentication(infoDto)

        //TODO this will require refactoring
        initSqlInfo(infoDto)
        //initAdHocInitialIndividuals()
        //postInits()

        updateConfigBasedOnSutInfoDto(infoDto)

        if (config.seedTestCases)
            initSeededTests()

        log.debug("Done initializing {}", WebSampler::class.simpleName)
    }


    override fun sampleAtRandom(): WebIndividual {
        val actions = mutableListOf<EnterpriseActionGroup<*>>()
        val n = randomness.nextInt(1, getMaxTestSizeDuringSampler())

        (0 until n).forEach {
            val a = sampleUndefinedAction()
            actions.add(EnterpriseActionGroup(mutableListOf(a), WebAction::class.java))
        }
        val ind =  WebIndividual(SampleType.RANDOM, actions)
        ind.doGlobalInitialize(searchGlobalState)

        return ind
    }

    /**
     * @See [WebAction.isDefined]
     */
    fun sampleUndefinedAction() : WebAction{
        return WebAction()
    }

    override fun initSeededTests(infoDto: SutInfoDto?) {
        // not supported yet
    }
}
