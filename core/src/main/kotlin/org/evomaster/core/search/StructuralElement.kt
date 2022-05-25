package org.evomaster.core.search

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

    //FIXME this workaround does not seem to work, see ProcessMonitorTest
    //constructor() : this(mutableListOf()) //issues with Kotlin compiler

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


    open fun getViewOfChildren() : List<StructuralElement> = children

    private fun initChildren(children : List<StructuralElement>){
        children.forEach { it.parent = this; }
    }


    /**
     * add a child of the element
     */
    open fun addChild(child: StructuralElement){  //TODO check usage
        child.parent = this
        //TODO re-check proper use of in/out in Kotlin
        (children as MutableList<StructuralElement>).add(child)
    }

    //https://preview.redd.it/hg27vjl7x0241.jpg?auto=webp&s=d3c8b5d2cfbf12a05715271e0cf7f1c26e962827
    open fun killAllChildren(){
        children.forEach {
            it.parent = null; //let's avoid memory leaks
        }
        children.clear()
    }

    open fun killChild(child: StructuralElement){
        child.parent = null
        children.remove(child)
    }

    open fun killChildByIndex(index: Int) : StructuralElement{
        val child = children.removeAt(index)
        child.parent = null
        return  child
    }

    /**
     * add children of the element
     */
    fun addChildren(children : List<StructuralElement>){
        children.forEach { addChild(it) }
    }


    /**
     * make a deep copy on the content
     *
     * Note that here we only copy the content the element,
     * do not further build relationship (e.g., binding) among the elements.
     *
     * After this method is called, need to call [postCopy] to setup to
     * relationship. This will be handled in [copy]
     */
    //TODO protected?
    protected abstract fun copyContent(): StructuralElement

    /**
     * post-handling on the copy based on its [original] version
     */
    //TODO protected?
    protected open fun postCopy(original : StructuralElement){
        if (children.size != original.children.size)
            throw IllegalStateException("copy has different size of children compared to original, e.g., copy (${children.size}) vs. original (${original.children.size})")
        children.indices.forEach {
            children[it].postCopy(original.children[it])
        }
    }

    /**
     * make a deep copy
     * @return a new Copyable based on [this]
     */
    open fun copy() : StructuralElement {
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
}