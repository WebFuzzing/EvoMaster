package org.evomaster.core.problem.webfrontend

import org.evomaster.core.problem.gui.GuiAction
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.jsoup.Jsoup

class WebAction(
    /**
     * Different interactions could be squeezed into a single action, like filling all text inputs
     * of a form and then submit it -
     * ie. filling a form and submitting - would be considered one action
     */
    val userInteractions: MutableList<WebUserInteraction> = mutableListOf(),
    /**
     * Map from cssLocator (coming from [userInteractions]) for text input to StringGene representing its value
     * MutableMap<csslocator, gene>
     */
    val textData: MutableMap<String, StringGene> = mutableMapOf(),
    /**
     * For a dropdown menu, where only one selection is possible, specify which one to choose.
     * This is based on the "value" attribute, or the visible text if not available.
     */
    val singleSelection: MutableMap<String, EnumGene<String>> = mutableMapOf(),
    /**
     * TODO explanation
     * TODO might change ArrayGene<BooleanGene>
     */
    val multiSelection: MutableMap<String, ArrayGene<BooleanGene>> = mutableMapOf(),
) : GuiAction(
    textData.values.map { it }
        .plus(singleSelection.values.map { it })
        .plus(multiSelection.values.map { it })
) {

    init {
        val nFillText = userInteractions.count { it.userActionType == UserActionType.FILL_TEXT }
        if (nFillText != textData.size) {
            throw IllegalArgumentException("Mismatch between $nFillText fill text actions and ${textData.size} genes for it")
        }
        for (key in textData.keys) {
            if (!userInteractions.any { it.cssSelector == key }) {
                throw IllegalArgumentException("Missing info for input: $key")
            }
        }

        //TODO constraint checks on singleSelection and multiSelection
        //SingleSelection
        if (singleSelection.isNotEmpty()) {
            for ((key, value) in singleSelection) {
                val selectedValue = value.getValueAsRawString()
                val allowedValues = value.values

                //check for duplicated option values?
                val duplicates = allowedValues
                    .groupingBy { it }
                    .eachCount()
                    .filter { it.value > 1 }
                    .keys

                //Constraint: Empty option list
                if (allowedValues.isNullOrEmpty()) {
                    throw Exception("List is Empty")
                }
                //Constraint: duplicated values
                if (duplicates.isNotEmpty()) {
                    throw Exception("Selection contains duplicate values")
                }

                // Constraint: Selection Shouldnt be empty?  the first option can be- to be removed
                /*if (selectedValue.isNullOrEmpty()) {
                    throw Exception("Selection for '$selectedValue' is missing.")
                }*/

                // Constraint: Selection must be in the allowed values -> for autocomplete cases when the user can type the option
                if (!allowedValues.contains(selectedValue.toString())) {
                    throw Exception("Invalid selection for '$selectedValue': '$selectedValue' is not among allowed values $allowedValues.")
                }
            }
        }
    }

    override fun isDefined(): Boolean {
        return userInteractions.isNotEmpty()
    }

    override fun isApplicableInCurrentContext(): Boolean {
        TODO("Not yet implemented")
    }

    fun isApplicableInGivenPage(page: String): Boolean {

        val document = try {
            Jsoup.parse(page)
        } catch (e: Exception) {
            return false
        }

        return userInteractions.all { document.select(it.cssSelector).size > 0 }
    }

    override fun getName(): String {
        if (userInteractions.isEmpty()) {
            return "Undefined"
        }
        val x = userInteractions.last()
        return "${x.userActionType}:${x.cssSelector}"
    }

    override fun seeTopGenes(): List<out Gene> {
        return children as List<out Gene>
    }


    override fun copyContent(): StructuralElement {
        return WebAction(
            userInteractions.map { it.copy() }.toMutableList(),
            textData.entries.associate { it.key to it.value.copy() as StringGene }.toMutableMap(),
            singleSelection.entries.associate { it.key to it.value.copy() as EnumGene<String> }.toMutableMap(),
            multiSelection.entries.associate { it.key to it.value.copy() as ArrayGene<BooleanGene> }.toMutableMap(),
        )
    }

    /**
     * Given another WebAction, copy its entire genotype into this.
     */
    fun copyValueFrom(other: WebAction) {

        killAllChildren()

        userInteractions.clear()
        userInteractions.addAll(other.userInteractions) //immutable elements
        textData.clear()
        textData.putAll(other.textData.entries.associate { it.key to it.value.copy() as StringGene })
        singleSelection.clear()
        singleSelection.putAll(other.singleSelection.entries.associate { it.key to it.value.copy() as EnumGene<String> })
        //TODO  multiSelection

        addChildren(textData.values.toList())
        addChildren(singleSelection.values.toList())
        addChildren(multiSelection.values.toList())
    }

    fun getIdentifier(): String {
        return "A:" + userInteractions.joinToString(",") { "${it.userActionType}:${it.cssSelector}" }
    }
}