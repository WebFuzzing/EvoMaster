package org.evomaster.core.problem.webfrontend

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.Select
import java.net.URI
import java.net.URISyntaxException

object BrowserActionBuilder {


    /**
     * FIXME this might require refactoring
     * checking on HTML as string might (or might not???) lose dynamic info added to the page.
     * TODO need to verify this.
     * Might be better to pass in a Selenium Document.
     * We need to verify before making the refactoring
     */
    fun computePossibleUserInteractions(html: String) : List<WebUserInteraction>{


        val document = try{
            Jsoup.parse(html)
        }catch (e: Exception){
            //TODO double-check
            return listOf()
        }

        val list = mutableListOf<WebUserInteraction>()

        //TODO all cases

        handleALinks(document, list)
        handleDropDowns(document, list)

        return list
    }

    private fun handleDropDowns(document: Document, list: MutableList<WebUserInteraction>) {

        document.getElementsByTag("select")
            .forEach {
                //TODO
               // val type =
               // list.add(WebUserInteraction(it.cssSelector(), UserActionType.SELECT))
            }
    }

    private fun handleALinks(
        document: Document,
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


    fun createPossibleActions(html: String) : List<WebAction>{

        val interactions = computePossibleUserInteractions(html)

        val inputs = interactions.filter { it.userActionType == UserActionType.FILL_TEXT }
        val others = interactions.filter { it.userActionType != UserActionType.FILL_TEXT }

        //TODO genes for inputs

        return others.map { WebAction(mutableListOf(it)) }
    }

}