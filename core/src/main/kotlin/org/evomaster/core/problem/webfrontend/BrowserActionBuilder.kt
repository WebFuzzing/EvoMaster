package org.evomaster.core.problem.webfrontend

import org.jsoup.Jsoup
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

object BrowserActionBuilder {


    fun computePossibleUserInteractions(html: String) : List<WebUserInteraction>{


        val document = try{
            Jsoup.parse(html)
        }catch (e: Exception){
            //TODO double-check
            return listOf()
        }

        val list = mutableListOf<WebUserInteraction>()

        //TODO all cases

        document.getElementsByTag("a")
            .forEach {
                val href = it.attr("href")
                val canClick = if(!href.isNullOrBlank()) {
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
                if(canClick){
                    list.add(WebUserInteraction(it.cssSelector(), UserActionType.CLICK))
                }
            }

        return list
    }


    fun createPossibleActions(html: String) : List<WebAction>{

        val interactions = computePossibleUserInteractions(html)

        val inputs = interactions.filter { it.userActionType == UserActionType.FILL_TEXT }
        val others = interactions.filter { it.userActionType != UserActionType.FILL_TEXT }

        //TODO genes for inputs

        return others.map { WebAction(mutableListOf(it)) }
    }

}