package org.evomaster.core.problem.webfrontend

import org.evomaster.core.problem.gui.GuiAction
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene

class WebAction(
    /**
     * Different interactions could be squizzed into a single action, like filling all text inputs
     * of a form and then submit it
     */
    val userInteractions: List<WebUserInteraction> = listOf(),
    /**
     * Map from cssLocator (coming from [userInteractions]) for text input to StringGene representing its value
     */
    val textData : Map<String, StringGene> = mapOf()
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

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return true
    }

    override fun copyContent(): StructuralElement {
        return WebAction(
            userInteractions.map { it.copy() },
            textData.entries.associate { it.key to it.value.copy() as StringGene }
        )
    }

    fun copyValueFrom(other: WebAction){
        userInteractions
    }

}