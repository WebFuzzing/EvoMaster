package org.evomaster.core.search.gene.regex

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene


abstract class RxTerm(name: String, children: List<out StructuralElement>) : Gene(name, children)