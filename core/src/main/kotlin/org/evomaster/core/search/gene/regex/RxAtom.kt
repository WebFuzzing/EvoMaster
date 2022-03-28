package org.evomaster.core.search.gene.regex

import org.evomaster.core.search.StructuralElement


abstract class RxAtom(name: String, children: List<out StructuralElement>) : RxTerm(name, children)