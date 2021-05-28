package org.evomaster.core.search

import org.evomaster.core.logging.LoggingUtil
import org.slf4j.LoggerFactory

/**
 * an element which has a structure, i.e., 0..1 [parent] and 0..* [children]
 * @property children its children
 * @property parent its parent
 */
abstract class StructuralElement(
    val children : MutableList<StructuralElement> = mutableListOf()
) {

    companion object{
        private val log = LoggerFactory.getLogger(StructuralElement::class.java)
    }

    var parent : StructuralElement? = null
        private set

    init {
        children.forEach { it.parent = this }
    }

    /**
     * make a deep copy on the content
     */
    abstract fun copyContent(): StructuralElement

    /**
     * post-handling on the copy based on its [template]
     */
    open fun postCopy(template : StructuralElement){
        if (children.size != template.children.size)
            throw IllegalStateException("copy and its template have different size of children, e.g., copy (${children.size}) vs. template (${template.children.size})")
        children.indices.forEach {
            children[it].postCopy(template.children[it])
        }
    }

    /**
     * make a deep copy
     * @return a new Copyable based on [this]
     */
    fun copy() : StructuralElement{
        if (parent != null)
            LoggingUtil.uniqueWarn(log, "${this::class.java} has a parent, the return copy might lose some info, e.g., parent")
        val copy = copyContent()
        copy.postCopy(this)
        return copy
    }

    fun getRoot() : StructuralElement{
        if (parent!=null) return parent!!.getRoot()
        return this
    }

    /**
     * @return a copy in [this] based on the [template] in [parent]
     */
    fun find(template: StructuralElement): StructuralElement{
        val traverseBack = mutableListOf<Int>()
        traverseBackIndex(traverseBack)
        val start = traverseBack.size

        val ttraverseBack = mutableListOf<Int>()
        template.traverseBackIndex(ttraverseBack)

        if (ttraverseBack.size < start)
            throw IllegalArgumentException("cannot find ancestor element (levels, current: $start vs. target: ${ttraverseBack.size})")

        if (start > 0 && !ttraverseBack.subList(0, start).containsAll(traverseBack))
            throw IllegalArgumentException("this does not contain requested target")

        if (ttraverseBack.size == start) return this

        return targetWithIndex(ttraverseBack.subList(start, ttraverseBack.size))
    }

    /**
     * @return an object based on [parent]
     */
    fun targetWithIndex(path: List<Int>): StructuralElement{
        var target = this
        path.forEach {
            if (it >= target.children.size)
                throw IllegalStateException("cannot get the children at index $it for $target which has ${target.children.size} children")
            target = target.children[it]
        }
        return target
    }

    /**
     * @return a visiting path from [this] to root
     * e.g., Root->A->B->C (-> indicates owns), the return should be a sequence of C, B, A, Root
     */
    fun traverseBack(back : MutableList<StructuralElement>) {
        back.add(0,this)
        if (parent!=null) parent!!.traverseBack(back)
    }

    /**
     * @return a visiting path from [this] to root
     * e.g., Root->A->B->C (-> indicates owns), the return should be a sequence of C, B, A, Root
     */
    fun traverseBackIndex(back : MutableList<Int>) {
        if (parent!=null) {
            val index = parent!!.children.indexOf(this)
            if (index == -1)
                throw IllegalStateException("cannot find this in its parent")
            back.add(0, index)
            parent!!.traverseBackIndex(back)
        }
    }
}