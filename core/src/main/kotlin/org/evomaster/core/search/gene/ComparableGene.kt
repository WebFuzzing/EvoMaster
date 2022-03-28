package org.evomaster.core.search.gene

import org.evomaster.core.search.StructuralElement

abstract class ComparableGene(name: String, children: List<StructuralElement>) :
    Comparable<ComparableGene>, Gene(name, children) {

}