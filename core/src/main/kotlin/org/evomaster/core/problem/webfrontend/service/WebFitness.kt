package org.evomaster.core.problem.webfrontend.service

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.core.Lazy
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.problem.enterprise.service.EnterpriseFitness
import org.evomaster.core.problem.webfrontend.*
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.taint.TaintAnalysis
import org.openqa.selenium.WebDriver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

/*  ftiness fuction - given an input text case- tell me how good it is. 0- not covered, 1 - covered.
*
* */

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
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean,
    ): EvaluatedIndividual<WebIndividual>? {

        rc.resetSUT()
        browserController.cleanBrowser() //TODO these 2 calls could be made in parallel

        val actionResults: MutableList<ActionResult> =
            mutableListOf() // keep track of results - for each action we execute we need to know the result of the action. ex: the response of an http call

        //initialization
        doDbCalls(
            individual.seeInitializingActions().filterIsInstance<SqlAction>(),
            actionResults = actionResults
        )// to think about it later, next year - it is setting up the environment

        //data structure representing the fitness of the individual
        val fv = FitnessValue(individual.size().toDouble())

        val actions = individual.seeMainExecutableActions()

        browserController.goToStartingPage()
        //read what this functions does - checks if URLs are malformed
        checkHtmlGlobalOracle(browserController.getCurrentPageSource(), browserController.getCurrentUrl(), fv)
        //if starting page is invalid, not much we can do at all...
        //TODO maybe should have explicit check at the beginning of the search

        //run the test, one action at a time
        for (i in actions.indices) {

            val a = actions[i]

            registerNewAction(a, i) // needed to tell the driver about the action to be taken

            val ok = handleWebAction(a, actionResults, fv)
            actionResults.filterIsInstance<WebResult>()[i].stopping = !ok

            if (!ok) {
                break
            }
        }

        val dto = updateFitnessAfterEvaluation(targets, allTargets, fullyCovered, descriptiveIds, individual, fv)
            ?: return null

        handleExtra(dto, fv)

        val webResults = actionResults.filterIsInstance<WebResult>()
        handleResponseTargets(fv, actions, webResults, dto.additionalInfoList)// handles black box interaction


        if (config.isEnabledTaintAnalysis()) {
            Lazy.assert { webResults.size == dto.additionalInfoList.size }
            TaintAnalysis.doTaintAnalysis(individual, dto.additionalInfoList, randomness, config)
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

    //the actual execution
    private fun handleWebAction(wa: WebAction, actionResults: MutableList<ActionResult>, fv: FitnessValue): Boolean {

        //TODO should check if current "page" is not html, eg an image

        val pageBeforeExecutingAction = browserController.getCurrentPageSource()
        val urlBeforeExecutingAction = browserController.getCurrentUrl()
        val possibilities = BrowserActionBuilder.createPossibleActions(browserController.getDriver())

        if (isNotHtmlPage(browserController.getDriver(), pageBeforeExecutingAction, urlBeforeExecutingAction)) {
            log.error("Not an HTML page"); // to be amended
        }

        var blocking = false

        if (!wa.isDefined() || !wa.isApplicableInGivenPage(pageBeforeExecutingAction)) {
            //not applicable might happen if mutation in previous action led to different page

            //TODO possibly add "back" and "refresh" actions, but with a probability

            //TODO if page is invalid, should always return with "back"

            if (possibilities.isEmpty()) {
                blocking = true
            } else {
                //TODO check archive for missing targets when choosing possibilities
                val chosen = randomness.choose(possibilities)
                assert(chosen.isDefined())
                chosen.doInitialize(randomness)
                wa.copyValueFrom(chosen)
            }
        }
        assert(blocking || (wa.isDefined() && wa.isApplicableInGivenPage(pageBeforeExecutingAction)))

        if (!blocking) {
            val inputs = wa.userInteractions.filter { it.userActionType == UserActionType.FILL_TEXT }
            //TODO first fill all inputs

            val interactions = wa.userInteractions.filter { it.userActionType != UserActionType.FILL_TEXT }
            interactions.forEach {

                when (it.userActionType) {
                    UserActionType.CLICK -> {
                        //TODO in try/catch... what to do in catch?
                        browserController.clickAndWaitPageLoad(it.cssSelector)
                        //TODO better wait
                    }

                    UserActionType.SELECT_SINGLE ->{
                        for(select in wa.singleSelection){
                            val css = select.key
                            val valueAttributeOrText = select.value.getValueAsRawString()
                            browserController.selectAndWaitPageLoad(css, listOf(valueAttributeOrText))
                        }
                    }
                    UserActionType.SELECT_MULTI -> {
                        //TODO
//                        /*
//                            not just clicking, but deciding which options to select.
//                            this is based on values in the genes
//                         */
//                        wa.seeTopGenes().size // size 0 ? No genes in webaction
//
//
//                       // val selectedValues = listOf("")  // select options coming from genes
//
//                        // from webaction, check if it is a single selector or multi,
//                        // then extract the gene, then from the browser controller extract the value
//
//
//                            browserController.selectAndWaitPageLoad(it.cssSelector, selectedValues)
//
                    }

                    else -> {
                        log.error("Not handled action type ${it.userActionType}")
                    }
                }
            }
        }


        val result = WebResult(wa.getLocalId(), blocking)

        val start = pageIdentifier.registerShape(HtmlUtils.computeIdentifyingShape(pageBeforeExecutingAction))
        result.setIdentifyingPageIdStart(start)
        result.setUrlPageStart(urlBeforeExecutingAction)
        result.setPossibleActionIds(possibilities.map { it.getIdentifier() })

        if (!blocking) {
            //TODO all needed info
            val endPageSource = browserController.getCurrentPageSource()
            val end = pageIdentifier.registerShape(HtmlUtils.computeIdentifyingShape(endPageSource))
            result.setIdentifyingPageIdEnd(end)
            val endUrl = browserController.getCurrentUrl()
            result.setUrlPageEnd(endUrl)

            if (start != end) { // navigation occurred
                val valid = checkHtmlGlobalOracle(endPageSource, endUrl, fv)
                result.setValidHtml(valid)
            }
        }

        actionResults.add(result)
        return !blocking
    }

    private fun checkHtmlGlobalOracle(html: String, urlOfHtmlPage: String, fv: FitnessValue): Boolean {

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
            try {
                URI(it)
            } catch (e: URISyntaxException) {
                //FIXME URI should not be used in Java, as implementing deprecated specs
                webGlobalState.addMalformedUri(it, urlOfHtmlPage)
                issues = true

                //TODO this will need thinking, eg, rather check that not getting 404 if following it
//                val id = idMapper.handleLocalTarget(idMapper.getFaultDescriptiveIdForMalformedURI(it))
//                fv.updateTarget(id, 1.0)

                return@forEach
            }

            //external links should be valid URL
            val url = try {
                URL(it)
            } catch (e: MalformedURLException) {
                return@forEach
            }

            val external = !url.host.isNullOrBlank()
            if (external) {
                if (!webGlobalState.hasAlreadySeenExternalLink(url)) {
                    val found = HtmlUtils.checkLink(url)
                    if (!found) {
                        issues = true

                        val id = idMapper.handleLocalTarget(idMapper.getFaultDescriptiveId(ExperimentalFaultCategory.WEB_BROKEN_LINK,it))
                        fv.updateTarget(id, 1.0)
                    }
                    webGlobalState.addExternalLink(url, found, urlOfHtmlPage)
                } else {
                    webGlobalState.updateExternalLink(url, urlOfHtmlPage)
                    if (webGlobalState.isBrokenLink(url)) {
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

        for (i in actions.indices) {
            val a = actions[i]
            val r = actionResults[i]

            if (r.stopping) {
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
                val prefix = "WEB_ACTION:${r.getIdentifyingPageIdStart()}@$it"
                val actionInPageId = idMapper.handleLocalTarget(
                    if(a.singleSelection.isEmpty()){
                        prefix
                    } else {
                        prefix + a.singleSelection.values.joinToString(",") { g -> g.getValueAsRawString() }
                    }
                )
                /*
                    on a page, there could be several interesting actions to do... we don't want to lose info on them.
                    we give as such a non-zero score.
                    of course, for action we actually made, it is covered (ie score 1)
                 */
                val h = if(it == executedActionId) 1.0 else 0.5
                fv.updateTarget(actionInPageId, h, i)
            }
            Lazy.assert { r.getPossibleActionIds().contains(executedActionId) }



            //TODO possibly check error logs in Chrome Tool
            //TODO targets for HTTP calls through reverse-proxy
        }

    }

    private fun isNotHtmlPage(driver: WebDriver, pageSource: String, pageURL: String): Boolean {
        return when {
            //check if it contains html or body tags
            !pageSource.contains("<html", ignoreCase = true) || !pageSource.contains("<body", ignoreCase = true) -> true
            //check headers
            else -> {
                try {
                    val url = URL(pageURL)
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.requestMethod = "HEAD"
                    connection.connect()
                    val contentType = connection.contentType?.lowercase() ?: ""
                    connection.disconnect()

                    contentType.isNotEmpty() && contentType.startsWith("application/") && !contentType.contains("html") //to be revised
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

}
/* Common content-type values
HTML - text/html
JPEG - image/jpeg
PNG  - image/png
PDF  - application/pdf
JSON - application/json
* */