package org.evomaster.core.problem.webfrontend

import org.jsoup.Jsoup

object BrowserActionBuilder {

    fun computePossibleUserInteractions(html: String) : List<WebUserInteraction>{

        val document = Jsoup.parse(html)

        val list = mutableListOf<WebUserInteraction>()

        //TODO all cases

        document.getElementsByTag("a")
            .forEach {
                list.add(WebUserInteraction(it.cssSelector(), UserActionType.CLICK))
            }

        return list
    }
}