package org.evomaster.core.search

import org.evomaster.core.search.gene.Gene


abstract class Individual{

    abstract fun copy() : Individual

    abstract fun seeGenes() : List<out Gene>

    abstract fun size() : Int
}

