package org.evomaster.core.search.structuralelement.simple.model

import org.evomaster.core.search.StructuralElement

class Leaf(val data: String) : StructuralElement() {

    val binding = mutableListOf<Leaf>()

    override fun getChildren(): MutableList<StructuralElement> = mutableListOf()


    override fun copyContent(): Leaf {
        return Leaf(data)
    }

    override fun postCopy(template: StructuralElement) {
        val root = getRoot()
        val postBinding = (template as Leaf).binding.map {b->
            root.find(b) as? Leaf?:throw IllegalStateException("mismatched type between template (${b::class.java.simpleName}) and found (Leaf)")
        }
        binding.clear()
        binding.addAll(postBinding)

        super.postCopy(template)
    }
}