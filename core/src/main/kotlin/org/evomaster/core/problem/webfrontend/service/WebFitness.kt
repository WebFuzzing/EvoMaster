package org.evomaster.core.problem.webfrontend.service

import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.enterprise.service.EnterpriseFitness
import org.evomaster.core.problem.webfrontend.*
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.taint.TaintAnalysis
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URISyntaxException
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
        targets: Set<Int>
    ): EvaluatedIndividual<WebIndividual>? {

        rc.resetSUT()
        browserController.cleanBrowser() //TODO these 2 calls could be made in parallel

        val actionResults: MutableList<ActionResult> = mutableListOf()

        doDbCalls(individual.seeInitializingActions().filterIsInstance<DbAction>(), actionResults = actionResults)

        val fv = FitnessValue(individual.size().toDouble())

        val actions = individual.seeMainExecutableActions()

        browserController.goToStartingPage()
        checkHtmlGlobalOracle(browserController.getCurrentPageSource(), browserController.getCurrentUrl())
        //if starting page is invalid, not much we can do at all...
        //TODO maybe should have explicit check at the beginning of the search

        //run the test, one action at a time
        for (i in actions.indices) {

            val a = actions[i]

            registerNewAction(a, i)

            val ok = handleWebAction(a, actionResults)
            actionResults.filterIsInstance<WebResult>()[i].stopping = !ok

            if (!ok) {
                break
            }
        }

        val dto = updateFitnessAfterEvaluation(targets, individual, fv)
            ?: return null

        handleExtra(dto, fv)

        val webResults = actionResults.filterIsInstance<WebResult>()
        handleResponseTargets(fv, actions, webResults, dto.additionalInfoList)


        if (config.baseTaintAnalysisProbability > 0) {
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

    private fun handleWebAction(a: WebAction, actionResults: MutableList<ActionResult>): Boolean {

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


        val result = WebResult(blocking)

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
                val valid = checkHtmlGlobalOracle(endPageSource, endUrl)
                result.setValidHtml(valid)
            }
        }

        actionResults.add(result)
        return !blocking
    }

    private fun checkHtmlGlobalOracle(html: String, urlOfHtmlPage: String) : Boolean{

        val ahrefs = HtmlUtils.getUrlInALinks(html)
        if(ahrefs == null){
            webGlobalState.addBrokenPage(urlOfHtmlPage)
            return false
        }

        ahrefs.forEach {
            val uri = try{
                URI(it)
            } catch (e: URISyntaxException){
                webGlobalState.addMalformedUri(it, urlOfHtmlPage)
                return@forEach
            }
            val external = !uri.host.isNullOrBlank()
            if(external){
                webGlobalState.addExternalLink(uri, urlOfHtmlPage)
            }
        }

        return true
    }

    private fun handleResponseTargets(
        fv: FitnessValue,
        actions: List<WebAction>,
        actionResults: List<WebResult>,
        additionalInfoList: List<AdditionalInfoDto>,
    ) {

        fv.updateTarget(idMapper.handleLocalTarget("HOME_PAGE"), 1.0)

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

            val validHtml = r.getValidHtml()!! //must be set
            if(!validHtml){
                val malformedHtmlId = idMapper.handleLocalTarget(idMapper.getFaultDescriptiveIdForMalformedHtml(r.getIdentifyingPageIdStart()!!))
                fv.updateTarget(malformedHtmlId, 1.0, i)
            }

            //TODO possibly check error logs in Chrome Tool
            //TODO targets for HTTP calls through reverse-proxy
        }

    }
}