package org.evomaster.core.problem.webfrontend

import org.evomaster.core.problem.gui.GuiAction
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene
import org.jsoup.Jsoup

class WebAction(
    /**
     * Different interactions could be squizzed into a single action, like filling all text inputs
     * of a form and then submit it
     */
    val userInteractions: MutableList<WebUserInteraction> = mutableListOf(),
    /**
     * Map from cssLocator (coming from [userInteractions]) for text input to StringGene representing its value
     */
    val textData : MutableMap<String, StringGene> = mutableMapOf()
) : GuiAction(textData.values.map { it }) {

    init {
        val nFillText = userInteractions.count { it.userActionType == UserActionType.FILL_TEXT }
        if(nFillText != textData.size){
            throw IllegalArgumentException("Mismatch between $nFillText fill text actions and ${textData.size} genes for it")
        }
        for(key in textData.keys){
            if(! userInteractions.any { it.cssSelector == key }){
                throw IllegalArgumentException("Missing info for input: $key")
            }
        }
    }

    override fun isDefined() : Boolean {
        return userInteractions.isNotEmpty()
    }

    override fun isApplicableInCurrentContext(): Boolean {
        TODO("Not yet implemented")
    }

    fun isApplicableInGivenPage(page: String) : Boolean{

        val document = try{
            Jsoup.parse(page)
        }catch (e: Exception){
            return false
        }

        return userInteractions.all { document.select(it.cssSelector).size > 0   }
    }

    override fun getName(): String {
        if(userInteractions.isEmpty()){
            return "Undefined"
        }
        val x = userInteractions.last()
        return "${x.userActionType}:${x.cssSelector}"
    }

    override fun seeTopGenes(): List<out Gene> {
        return  children as List<out Gene>
    }


    override fun copyContent(): StructuralElement {
        return WebAction(
            userInteractions.map { it.copy() }.toMutableList(),
            textData.entries.associate { it.key to it.value.copy() as StringGene }.toMutableMap()
        )
    }

    fun copyValueFrom(other: WebAction){
        userInteractions.clear()
        userInteractions.addAll(other.userInteractions) //immutable elements
        textData.clear()
        textData.putAll(other.textData.entries.associate { it.key to it.value.copy() as StringGene })
    }

    fun getIdentifier() : String {
        return "A:" + userInteractions.joinToString(","){"${it.userActionType}:${it.cssSelector}"}
    }
}