package org.evomaster.core.problem.webfrontend

import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.openqa.selenium.By
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.Select
import java.net.URI
import java.net.URISyntaxException
import kotlin.math.min

object BrowserActionBuilder {


    /**
     * Given the current page we need to see what can we do in the page, what kind of interactions we can have.
     */
    fun computePossibleUserInteractions(driver: RemoteWebDriver) : List<WebUserInteraction>{


        val document = try{
            Jsoup.parse(driver.pageSource)
        }catch (e: Exception){
            //TODO double-check
            return listOf()
        }

        val list = mutableListOf<WebUserInteraction>()

        //TODO all cases

        handleALinks(document, driver, list)
        handleDropDowns(document, driver, list)

        return list
    }

    private fun handleDropDowns(
        document: Document,
        driver: RemoteWebDriver,
        list: MutableList<WebUserInteraction>
    ) {

        document.getElementsByTag("select")
            .forEach { jsoup ->
                val dropdown = Select(driver.findElement(By.cssSelector(jsoup.cssSelector())))
                val type = if(dropdown.isMultiple) {
                    UserActionType.SELECT_MULTI
                }  else {
                    UserActionType.SELECT_SINGLE
                }
                val options = dropdown.options
                    .filter{it.isEnabled}
                    .map{
                        /*
                            Look at "value" attribute. unfortunately, it is not mandatory.
                            in such case, we rather use the visible text.
                            note: prefer avoiding indices, as they are not reliable, eg, if changes, it would not
                            break the test, unless out of bounds
                         */
                        it.getAttribute("value") ?: it.text
                    }
                list.add(WebUserInteraction(jsoup.cssSelector(), type, options))
            }
    }


/* Analyze anchor tags to identify clickable elements*/
    private fun handleALinks(
        document: Document,
        driver: RemoteWebDriver,
        list: MutableList<WebUserInteraction>
    ) {
        document.getElementsByTag("a")
            .forEach {
                val href = it.attr("href")
                val canClick = if (!href.isNullOrBlank()) {
                    val uri = try {
                        URI(href)
                    } catch (e: URISyntaxException) {
                        //errors are handled elsewhere in fitness function
                        return@forEach
                    }
                    val external = !uri.host.isNullOrBlank()
                    !external
                } else {
                    val onclick = it.attr("onclick")
                    !onclick.isNullOrBlank()
                }
                if (canClick) {
                    list.add(WebUserInteraction(it.cssSelector(), UserActionType.CLICK))
                }
            }
    }


    fun createPossibleActions(driver: RemoteWebDriver) : List<WebAction>{

        val interactions = computePossibleUserInteractions(driver)

        val inputs = interactions.filter { it.userActionType == UserActionType.FILL_TEXT }
        val others = interactions.filter { it.userActionType != UserActionType.FILL_TEXT }

        //TODO genes for inputs

        return others.map {
            when(it.userActionType) {
                UserActionType.CLICK -> WebAction(mutableListOf(it))
                UserActionType.SELECT_SINGLE -> {
                    val selection = EnumGene<String>(it.cssSelector, it.inputs)
                    WebAction(mutableListOf(it), singleSelection = mutableMapOf(it.cssSelector to selection))
                }
                //TODO multi
                else -> {
                    //TODO log warn
                    WebAction(mutableListOf(it))
                }
            }
        }
    }

}