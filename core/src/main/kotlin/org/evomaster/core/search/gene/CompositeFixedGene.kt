package org.evomaster.core.search.gene

import org.evomaster.core.search.StructuralElement

abstract class CompositeFixedGene(
        name: String,
        children: List<out Gene>
) : CompositeGene(name, children.toMutableList()) {

    constructor(name: String, child: Gene) : this(name, mutableListOf(child))

    init {
        if(children.isEmpty() && !canBeChildless()){
            throw IllegalStateException("A fixed composite gene must have at least 1 internal gene")
        }
    }

    open fun canBeChildless() = false


    private val errorChildMsg = "BUG in EvoMaster: cannot modify children of fixed ${this.javaClass}"

    override fun addChild(child: StructuralElement) {
        throw IllegalStateException(errorChildMsg)
    }

    override fun killAllChildren(){
        throw IllegalStateException(errorChildMsg)
    }

    override fun killChild(child: StructuralElement){
        throw IllegalStateException(errorChildMsg)
    }

    override fun killChildByIndex(index: Int) : StructuralElement{
        throw IllegalStateException(errorChildMsg)
    }

    override fun killChildren(predicate: (StructuralElement) -> Boolean){
        throw IllegalStateException(errorChildMsg)
    }

    override fun killChildren(toKill: List<out StructuralElement>){
        throw IllegalStateException(errorChildMsg)
    }
}