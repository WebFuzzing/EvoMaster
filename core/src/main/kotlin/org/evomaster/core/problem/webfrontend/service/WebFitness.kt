package org.evomaster.core.problem.webfrontend.service

import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.core.Lazy
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.problem.enterprise.service.EnterpriseFitness
import org.evomaster.core.problem.webfrontend.*
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.taint.TaintAnalysis
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import javax.inject.Inject


class WebFitness : EnterpriseFitness<WebIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(WebFitness::class.java)
    }

    @Inject
    private lateinit var browserController: BrowserController

    @Inject
    private lateinit var pageIdentifier: WebPageIdentifier

    @Inject
    private lateinit var webGlobalState: WebGlobalState


    override fun doCalculateCoverage(
        individual: WebIndividual,
        targets: Set<Int>,
        allCovered: Boolean
    ): EvaluatedIndividual<WebIndividual>? {

        rc.resetSUT()
        browserController.cleanBrowser() //TODO these 2 calls could be made in parallel

        val actionResults: MutableList<ActionResult> = mutableListOf()

        doDbCalls(individual.seeInitializingActions().filterIsInstance<SqlAction>(), actionResults = actionResults)

        val fv = FitnessValue(individual.size().toDouble())

        val actions = individual.seeMainExecutableActions()

        browserController.goToStartingPage()
        checkHtmlGlobalOracle(browserController.getCurrentPageSource(), browserController.getCurrentUrl(), fv)
        //if starting page is invalid, not much we can do at all...
        //TODO maybe should have explicit check at the beginning of the search

        //run the test, one action at a time
        for (i in actions.indices) {

            val a = actions[i]

            registerNewAction(a, i)

            val ok = handleWebAction(a, actionResults, fv)
            actionResults.filterIsInstance<WebResult>()[i].stopping = !ok

            if (!ok) {
                break
            }
        }

        val dto = updateFitnessAfterEvaluation(targets, allCovered, individual, fv)
            ?: return null

        handleExtra(dto, fv)

        val webResults = actionResults.filterIsInstance<WebResult>()
        handleResponseTargets(fv, actions, webResults, dto.additionalInfoList)


        if (config.isEnabledTaintAnalysis()) {
            Lazy.assert { webResults.size == dto.additionalInfoList.size }
            TaintAnalysis.doTaintAnalysis(individual, dto.additionalInfoList, randomness, config.enableSchemaConstraintHandling)
        }

        return EvaluatedIndividual(
            fv,
            individual.copy() as WebIndividual,
            actionResults,
            trackOperator = individual.trackOperator,
            index = time.evaluatedIndividuals,
            config = config
        )
    }

    private fun handleWebAction(a: WebAction, actionResults: MutableList<ActionResult>, fv: FitnessValue): Boolean {

        //TODO should check if current "page" is not html, eg an image

        val pageBeforeExecutingAction = browserController.getCurrentPageSource()
        val urlBeforeExecutingAction = browserController.getCurrentUrl()
        val possibilities = BrowserActionBuilder.createPossibleActions(pageBeforeExecutingAction)

        var blocking = false

        if(!a.isDefined() ||
            !a.isApplicableInGivenPage(pageBeforeExecutingAction)){
            //not applicable might happen if mutation in previous action led to different page

            //TODO possibly add "back" and "refresh" actions, but with a probability

            //TODO if page is invalid, should always return with "back"

            if(possibilities.isEmpty()){
                blocking = true
            } else {
                //TODO check archive for missing targets when choosing possibilities
                val chosen = randomness.choose(possibilities)
                assert(chosen.isDefined())
                a.copyValueFrom(chosen)
            }
        }
        assert(blocking || (a.isDefined() && a.isApplicableInGivenPage(pageBeforeExecutingAction)))

        if(!blocking) {
            val inputs = a.userInteractions.filter { it.userActionType == UserActionType.FILL_TEXT }
            //TODO first fill all inputs

            val interactions = a.userInteractions.filter { it.userActionType != UserActionType.FILL_TEXT }
            interactions.forEach {

                when (it.userActionType) {
                    UserActionType.CLICK -> {
                        //TODO in try/catch... what to do in catch?
                        browserController.clickAndWaitPageLoad(it.cssSelector)
                        //TODO better wait
                    }
                    else -> {
                        log.error("Not handled action type ${it.userActionType}")
                    }
                }
            }
        }


        val result = WebResult(a.getLocalId(), blocking)

        val start = pageIdentifier.registerShape(HtmlUtils.computeIdentifyingShape(pageBeforeExecutingAction))
        result.setIdentifyingPageIdStart(start)
        result.setUrlPageStart(urlBeforeExecutingAction)
        result.setPossibleActionIds(possibilities.map { it.getIdentifier() })

        if(!blocking) {
            //TODO all needed info
            val endPageSource = browserController.getCurrentPageSource()
            val end   = pageIdentifier.registerShape(HtmlUtils.computeIdentifyingShape(endPageSource))
            result.setIdentifyingPageIdEnd(end)
            val endUrl = browserController.getCurrentUrl()
            result.setUrlPageEnd(endUrl)

            if(start != end){
                val valid = checkHtmlGlobalOracle(endPageSource, endUrl, fv)
                result.setValidHtml(valid)
            }
        }

        actionResults.add(result)
        return !blocking
    }

    private fun checkHtmlGlobalOracle(html: String, urlOfHtmlPage: String, fv: FitnessValue) : Boolean{

        var issues = false

        /*
        if(HtmlUtils.checkErrorsInHtml(html) != null){
            //TODO save error message, and output in generated tests
            //   TODO actually, this does not work, as Chrome fixes the issues in HTML when displaying it.
            //   An option could be to make direct call to SUT, to fetch original HTML
            //NOTE code is commented out because parsing HTML is not cheap

            webGlobalState.addBrokenPage(urlOfHtmlPage)
            //return false

            val malformedHtmlId = idMapper.handleLocalTarget(idMapper.getFaultDescriptiveIdForMalformedHtml(r.getIdentifyingPageIdEnd()!!))
            fv.updateTarget(malformedHtmlId, 1.0, i)
        }
         */

        HtmlUtils.getUrlInALinks(html).forEach {
            try{
                URI(it)
            } catch (e: URISyntaxException){
                webGlobalState.addMalformedUri(it, urlOfHtmlPage)
                issues = true

                val id = idMapper.handleLocalTarget(idMapper.getFaultDescriptiveIdForMalformedURI(it))
                fv.updateTarget(id, 1.0)

                return@forEach
            }

            //external links should be valid URL
            val url = try{
                URL(it)
            } catch (e: MalformedURLException){
                return@forEach
            }

            val external = !url.host.isNullOrBlank()
            if(external){
                if(! webGlobalState.hasAlreadySeenExternalLink(url)){
                    val found = HtmlUtils.checkLink(url)
                    if(!found){
                        issues = true

                        val id = idMapper.handleLocalTarget(idMapper.getFaultDescriptiveIdForBrokenLink(it))
                        fv.updateTarget(id, 1.0)
                    }
                    webGlobalState.addExternalLink(url, found, urlOfHtmlPage)
                }  else {
                    webGlobalState.updateExternalLink(url, urlOfHtmlPage)
                    if(webGlobalState.isBrokenLink(url)){
                        issues = true
                    }
                }
            }
        }

        return !issues
    }



    private fun handleResponseTargets(
        fv: FitnessValue,
        actions: List<WebAction>,
        actionResults: List<WebResult>,
        additionalInfoList: List<AdditionalInfoDto>,
    ) {

        fv.updateTarget(idMapper.handleLocalTarget("WEB_HOME_PAGE"), 1.0)

        for(i in actions.indices){
            val a = actions[i]
            val r = actionResults[i]

            if(r.stopping){
                return
            }

            //target for reaching page E
            val pageId = idMapper.handleLocalTarget("WEB_PAGE:${r.getIdentifyingPageIdEnd()}")
            fv.updateTarget(pageId, 1.0, i)

            //target for transaction S->E
            val transactionId = idMapper.handleLocalTarget("WEB_TRANSACTION:${r.getIdentifyingPageIdStart()}->${r.getIdentifyingPageIdEnd()}")
            fv.updateTarget(transactionId, 1.0, i)

            val executedActionId = a.getIdentifier()
            r.getPossibleActionIds().forEach {
                val actionInPageId = idMapper.handleLocalTarget("WEB_ACTION:${r.getIdentifyingPageIdStart()}@$it")
                val h = if(it == executedActionId) 1.0 else 0.5
                fv.updateTarget(actionInPageId, h, i)
            }
            Lazy.assert { r.getPossibleActionIds().contains(executedActionId) }



            //TODO possibly check error logs in Chrome Tool
            //TODO targets for HTTP calls through reverse-proxy
        }

    }
}