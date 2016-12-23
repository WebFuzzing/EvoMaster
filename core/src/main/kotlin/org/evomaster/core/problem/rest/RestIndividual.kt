package org.evomaster.core.problem.rest

import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene


class RestIndividual : Individual(){

    private val actions : List<RestAction> = mutableListOf()


    override fun copy(): Individual {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun genes(): List<out Gene> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun size(): Int {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}