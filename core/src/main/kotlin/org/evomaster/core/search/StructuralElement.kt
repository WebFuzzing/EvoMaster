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
        /*
            TODO check if needed a function to return copy
         */
    protected open val children : MutableList<out StructuralElement> = mutableListOf()
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

    /**
     * present whether the element is defined root
     */
    private var isDefinedRoot : Boolean = false

    init {
        initChildren(children)
    }


    open fun getViewOfChildren() : List<StructuralElement> = children

    private fun initChildren(children : List<StructuralElement>){
        children.forEach { it.parent = this }
    }

    /**
     * @return children of [this]
     *
     * FIXME: this is not related to children in input. confusing, might need to change name
     */
//    abstract fun getChildren(): List<out StructuralElement>

    /**
     * add a child of the element
     * Note that the default method is only to build the parent/children relationship
     *
     * FIXME: this is setting up the parent-child relationship, not adding to children
     */
    open fun addChild(child: StructuralElement){  //TODO check usage
        child.parent = this
        child.isDefinedRoot = false
        //TODO re-check proper use of in/out in Kotlin
        (children as MutableList<StructuralElement>).add(child)
    }

    /**
     * add children of the element
     * Note that the default method is only to build the parent/children relationship
     *
     * FIXME see previous comment
     */
    fun addChildren(children : List<StructuralElement>){
        children.forEach { addChild(it) }
    }

    //TODO add method to kill child, ie remove, otherwise memory leak.
    //TODO possibly called in CollectionGene?


    /**
     * make a deep copy on the content
     *
     * Note that here we only copy the content the element,
     * do not further build relationship (e.g., binding) among the elements.
     *
     * After this method is called, need to call [postCopy] to setup to
     * relationship. This will be handled in [copy]
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
    open fun copy() : StructuralElement {
        // except individual, all elements should have a parent
        if (parent == null && !isDefinedRoot()) {
            val msg = "${this::class.java} should have a parent but currently it is null"
            //LoggingUtil.uniqueWarn(log, msg)
            throw IllegalStateException(msg)
        }
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

    /**
     * clarify if the element is root as defined
     */
    fun isDefinedRoot() = isDefinedRoot

    /**
     * identify the element as root
     */
    fun identifyAsRoot(){
        isDefinedRoot = true
    }

}