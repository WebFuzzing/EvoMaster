package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *  A tuple is a fixed-size, ordered list of elements, of possible different types.
 *  This is needed for example when representing the inputs of function calls in
 *  GraphQL.
 *
 *  TODO all needed methods to make it compile
 *
 *  TODO double-check with Man regarding hypermutation for this gene
 */
class TupleGene(
    /**
     * The name of this gene
     */
    name: String,
    /**
     * The actual elements in the array, based on the template. Ie, usually those elements will be clones
     * of the templated, and then mutated/randomized
     */
    var elements: MutableList<Gene> = mutableListOf(),
    /**
     * In some cases, we want to treat an element differently from the other (the last in particular).
     * This is for example the case of function calls in GQL when the return type is an object, on
     * which we need to select what to retrieve.
     * In these cases, such return object will be part of the tuple, as the last element.
     */
    val lastElementTreatedSpecially: Boolean = false

) : Gene(name, elements) {

    init {
        if (elements.isEmpty()) {
            throw IllegalArgumentException("Empty tuple")
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(TupleGene::class.java)
    }

    fun getSpecialGene(): Gene? {
        TODO("Not yet implemented")
        if (lastElementTreatedSpecially) {
            return elements.last()
        }
        return StringGene("Not implemented yet!!!")

    }


    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        //for now, it is specific to graphql with functions in returned types
        TODO("Not yet implemented")
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        if (elements.isEmpty()) return
        //double check
        elements.dropLast(1).forEach {
            it.randomize(randomness, false)
        }
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is TupleGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.elements = other.elements.map { e -> e.copyContent() }.toMutableList()
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is TupleGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.elements.zip(other.elements) { thisElem, otherElem ->
            thisElem.containsSameValueAs(otherElem)
        }.all { it }
    }

    override fun innerGene(): List<Gene> = elements


    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is TupleGene) {
            elements = gene.elements.map { it.copyContent() }.toMutableList()
            return true
        }
        LoggingUtil.uniqueWarn(log, "cannot bind TupleGene with ${gene::class.java.simpleName}")
        return false

    }

    override fun getChildren(): MutableList<Gene> = elements


}