package org.evomaster.core.search

import org.evomaster.core.logging.LoggingUtil
import org.slf4j.LoggerFactory

/**
 * an element which has a structure, i.e., 0..1 [parent] and 0..* children
 * the children can be initialized with constructor, and further added with [addChild] and [addChildren]
 * @param children its children
 * @property parent its parent
 */
abstract class StructuralElement (
    children : List<out StructuralElement> = mutableListOf()
) {

    companion object{
        private val log = LoggerFactory.getLogger(StructuralElement::class.java)
    }

    /**
     * parent of the element, which contains current the element
     * Note that [parent] can be null when the element is root
     */
    var parent : StructuralElement? = null
        private set

    init {
        initChildren(children)
    }

    private fun initChildren(children : List<StructuralElement>){
        children.forEach { it.parent = this }
    }

    /**
     * @return children of [this]
     */
    abstract fun getChildren(): List<out StructuralElement>

    /**
     * add a child of the element
     * Note that the default method is only to build the parent/children relationship
     */
    open fun addChild(child: StructuralElement){
        child.parent = this
    }

    /**
     * add children of the element
     * Note that the default method is only to build the parent/children relationship
     */
    open fun addChildren(children : List<StructuralElement>){
        initChildren(children)
    }

    /**
     * make a deep copy on the content
     *
     * Noet that here we only copy the content the element,
     * do not further build relationship (e.g., binding) among the elements
     */
    abstract fun copyContent(): StructuralElement

    /**
     * post-handling on the copy based on its [template]
     */
    open fun postCopy(template : StructuralElement){
        if (getChildren().size != template.getChildren().size)
            throw IllegalStateException("copy and its template have different size of children, e.g., copy (${getChildren().size}) vs. template (${template.getChildren().size})")
        getChildren().indices.forEach {
            getChildren()[it].postCopy(template.getChildren()[it])
        }
    }

    /**
     * make a deep copy
     * @return a new Copyable based on [this]
     */
    open fun copy() : StructuralElement {
        if (parent != null)
            LoggingUtil.uniqueWarn(log, "${this::class.java} should have a parent but currently it is null")
        val copy = copyContent()
        copy.postCopy(this)
        return copy
    }

    /**
     * @return root of the element which can not be null
     * if [this] is the root, return [this]
     */
    fun getRoot() : StructuralElement {
        if (parent!=null) return parent!!.getRoot()
        return this
    }

    /**
     * @return a copy in [this] based on the [template] in [parent]
     */
    fun find(template: StructuralElement): StructuralElement {
        // check if the root has same type
        val targetRoot = getRoot()
        val templateRoot = template.getRoot()
        if (targetRoot::class.java.simpleName != templateRoot::class.java.simpleName){
            throw IllegalStateException("mismatched root type: target (${targetRoot::class.java.simpleName}) vs. template (${templateRoot::class.java.simpleName})")
        }

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
    fun targetWithIndex(path: List<Int>): StructuralElement {
        var target = this
        path.forEach {
            if (it >= target.getChildren().size)
                throw IllegalStateException("cannot get the children at index $it for $target which has ${target.getChildren().size} children")
            target = target.getChildren()[it]
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
            val index = parent!!.getChildren().indexOf(this)
            if (index == -1)
                throw IllegalStateException("cannot find this in its parent")
            back.add(0, index)
            parent!!.traverseBackIndex(back)
        }
    }

}