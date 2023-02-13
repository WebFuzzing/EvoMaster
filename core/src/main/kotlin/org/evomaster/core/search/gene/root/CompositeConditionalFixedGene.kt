package org.evomaster.core.search.gene.root

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

/**
 * represent a composite gene and whether its children is fixed depends on [isFixed]
 * note that if [isFixed] is true, it means that any addition or removal of its children is not allowed.
 */
abstract class CompositeConditionalFixedGene(
        name: String,
        val isFixed: Boolean,
        children: List<out Gene>
) : CompositeGene(name, children.toMutableList()) {

    constructor(name: String, isFixed: Boolean, child: Gene) : this(name, isFixed, mutableListOf(child))

    init {
        if(children.isEmpty() && !canBeChildless()){
            throw IllegalStateException("A fixed composite gene must have at least 1 internal gene")
        }
    }

    abstract fun canBeChildless() : Boolean


    private val errorChildMsg = "BUG in EvoMaster: cannot modify children of fixed ${this.javaClass}"

    override fun addChild(child: StructuralElement) {
        if (isFixed) throw IllegalStateException(errorChildMsg)
        super.addChild(child)
    }

    override fun addChild(position: Int, child: StructuralElement){
        if (isFixed) throw IllegalStateException(errorChildMsg)
        super.addChild(position, child)
    }

    override fun addChildren(position: Int, list : List<StructuralElement>){
        if (isFixed) throw IllegalStateException(errorChildMsg)
        super.addChildren(position, list)
    }

    override fun killAllChildren(){
        if (isFixed) throw IllegalStateException(errorChildMsg)
        super.killAllChildren()
    }

    override fun killChild(child: StructuralElement){
        if (isFixed) throw IllegalStateException(errorChildMsg)
        super.killChild(child)
    }

    override fun killChildByIndex(index: Int) : StructuralElement {
        if (isFixed) throw IllegalStateException(errorChildMsg)
        return super.killChildByIndex(index)
    }

    override fun killChildren(predicate: (StructuralElement) -> Boolean){
        if (isFixed) throw IllegalStateException(errorChildMsg)
        super.killChildren(predicate)
    }

    override fun killChildren(toKill: List<out StructuralElement>){
        if (isFixed) throw IllegalStateException(errorChildMsg)
        super.killChildren(toKill)
    }

}