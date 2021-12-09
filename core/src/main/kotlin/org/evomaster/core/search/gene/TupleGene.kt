package org.evomaster.core.search.gene

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
        var elements: List<Gene> = listOf(),
        /**
         * In some cases, we want to treat an element differently from the other (the last in particular).
         * This is for example the case of function calls in GQL when the return type is an object, on
         * which we need to select what to retrieve.
         * In this cases, such return object will be part of the tuple, as the last element.
         */
        val lastElementTreatedSpecially : Boolean = false

) : CollectionGene, Gene(name, elements) {

    init {
        if(elements.isEmpty()){
            throw IllegalArgumentException("Empty tuple")
        }
    }

    fun getSpecialGene() : Gene? {
        if(lastElementTreatedSpecially){
            return elements.last()
        }
    }

}