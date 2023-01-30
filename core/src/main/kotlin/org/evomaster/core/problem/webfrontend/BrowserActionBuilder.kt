package org.evomaster.core.problem.webfrontend

import org.jsoup.Jsoup

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
                list.add(WebUserInteraction(it.cssSelector(), UserActionType.CLICK))
            }

        return list
    }


    fun createPossibleActions(html: String) : List<WebAction>{

        val interactions = computePossibleUserInteractions(html)

        val inputs = interactions.filter { it.userActionType == UserActionType.FILL_TEXT }
        val others = interactions.filter { it.userActionType != UserActionType.FILL_TEXT }

        //TODO genes for inputs

        return others.map { WebAction(listOf(it)) }
    }

}