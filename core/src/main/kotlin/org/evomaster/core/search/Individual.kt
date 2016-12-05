package org.evomaster.core.search

import org.evomaster.core.search.gene.Gene


abstract class Individual{

    abstract fun copy() : Individual

    abstract fun genes() : List<out Gene>
}

