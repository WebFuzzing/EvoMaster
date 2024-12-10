package org.evomaster.core.search

import org.evomaster.core.Lazy
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.service.monitor.ProcessMonitorExcludeField
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/**
 * an element which has a structure, i.e., 0..1 [parent] and 0..* children
 * the children can be initialized with constructor, and further added with [addChild] and [addChildren]
 * @param children its children
 * @param groups optional grouping for the children.
 * @property parent its parent
 */
abstract class StructuralElement (
   /*
        Unfortunately, Kotlin does not allow to define generics like this

        StructuralElement<T : StructuralElement<*>>

        as it violates  Finite Bound Restriction constraints.
        https://stackoverflow.com/questions/46682455/how-to-solve-violation-of-finite-bound-restriction-in-kotlin

        So, we end up using an "out" bound, which forces us to do a runtime upper cast to StructuralElement
        every time we need to insert new elements in children. It is not ideal, as right insertions of proper
        types for the children would not be checked at compilation time.
        A partial workaround is to introduce a type verifier, checked every time a new element is added.

        An extra benefit here is that such verifier can be used to exclude only some specific subtypes.
    */

    protected open val children : MutableList<out StructuralElement> = mutableListOf(),
   @ProcessMonitorExcludeField
    protected val childTypeVerifier: (Class<*>) -> Boolean = {_ -> true},
   @ProcessMonitorExcludeField
    private var groups : GroupsOfChildren<StructuralElement>? = null
) {

    companion object{
        private val log = LoggerFactory.getLogger(StructuralElement::class.java)

        const val NONE_LOCAL_ID = "NONE_LOCAL_ID"
    }

    /**
     * a unique id is used to identify this structural element in the context of an individual
     */
    private var localId : String = NONE_LOCAL_ID

    /**
     * set a local id of the action
     * note that the id can be assigned only if the current id is NONE_ACTION_ID
     */
    fun setLocalId(id: String) {
        if (!hasLocalId())
            this.localId = id
        else
            throw IllegalStateException("Cannot re-assign the id of the element with $id. The current id is ${this.localId}")
    }

    /**
     * return if the action has been assigned with a local id
     */
    fun hasLocalId() = localId != NONE_LOCAL_ID

    /**
     * reset local id of this structural element
     */
    fun resetLocalId() {
        localId = NONE_LOCAL_ID
    }

    /**
     * reset local if of this structural element and its children
     */
    fun resetLocalIdRecursively(){
        resetLocalId()
        children.forEach(StructuralElement::resetLocalIdRecursively)
    }

    fun getLocalId() = localId

    /**
     * parent of the element, which contains current the element
     * Note that [parent] can be null when the element is root
     */
    @ProcessMonitorExcludeField
    var parent : StructuralElement? = null
        private set

    fun groupsView() = groups


    init {
        verifyChildrenToInsert(children)
        children.forEach {
            it.parent = this;
        }
        groups?.verifyGroups()
    }


    /**
     * a pre-setup for the children if needed
     * the setup will be performed before the children to add
     */
    private fun preChildrenSetup(c : Collection<StructuralElement>){
        // handle local id for new children to add into individual
        if (this is Individual)
            this.handleLocalIdsForAddition(c)
        /*
           handle local id for new children to add into composite structure of individual,
           ie ActionTree (eg, add external service to group), Action (eg, add param to action), ArrayGene (eg, add element to gene)
         */
        if (this.getRoot() is Individual && (this is ActionComponent || this is CompositeGene))
            (this.getRoot() as Individual).handleLocalIdsForAddition(c)
    }


    open fun getViewOfChildren() : List<StructuralElement> = children

    /**
     * Return a map from index in the children array to child value.
     * Only the children of type [klass] are included
     */
     fun <T> getIndexedChildren(klass: Class<T>): Map<Int, T>{
        val m  = mutableMapOf<Int, T>()
        for(i in children.indices){
            val child = children[i]
            if(!klass.isAssignableFrom(child.javaClass)){
                continue
            }
            m[i] = child as T
        }
        return m
    }

    private fun verifyChildrenToInsert(e : StructuralElement){
        if(! childTypeVerifier.invoke(e.javaClass)){
            throw IllegalArgumentException("Cannot add child due to its class ${e.javaClass}")
        }
    }

    private fun verifyChildrenToInsert(c : Collection<StructuralElement>){
        c.forEach { verifyChildrenToInsert(it) }
    }


    fun addChildToGroup(child: StructuralElement, groupId: String){
        verifyChildrenToInsert(child)
        if(groups == null){
            throw IllegalArgumentException("No groups are defined")
        }
        preChildrenSetup(listOf(child))

        val end = groups!!.endIndexForGroupInsertionInclusive(groupId)
        addChild(end, child) //appending at the end of the group
        groups!!.addedToGroup(groupId, child)
    }

    fun addChildToGroup(position: Int, child: StructuralElement, groupId: String){
        verifyChildrenToInsert(child)
        if(groups == null){
            throw IllegalArgumentException("No groups are defined")
        }
        val start = groups!!.startIndexForGroupInsertionInclusive(groupId)
        val end = groups!!.endIndexForGroupInsertionInclusive(groupId)
        if(position < start || position > end){
            throw IllegalArgumentException("Invalid position $position out of [$start,$end] for group $groupId")
        }
        preChildrenSetup(listOf(child))
        addChild(position,child)
        groups!!.addedToGroup(groupId, child)
    }

    fun addChildrenToGroup(children : List<StructuralElement>, groupId: String){
        children.forEach { addChildToGroup(it, groupId) }
    }

    fun addChildrenToGroup(position: Int, children : List<StructuralElement>, groupId: String){
        var i = position
        children.forEach { addChildToGroup(i++, it, groupId) }
    }


    /**
     * add a child of the element
     */
    open fun addChild(child: StructuralElement){
        verifyChildrenToInsert(child)
        if(children.contains(child)){
            throw IllegalArgumentException("Child already present")
        }
        preChildrenSetup(listOf(child))
        child.parent = this
        (children as MutableList<StructuralElement>).add(child)
    }

    open fun addChild(position: Int, child: StructuralElement){
        verifyChildrenToInsert(child)
        if(children.contains(child)) throw IllegalArgumentException("Child already present")
        preChildrenSetup(listOf(child))
        child.parent = this
        (children as MutableList<StructuralElement>).add(position, child)
    }

    /**
     * add children of the element
     */
    fun addChildren(children : List<StructuralElement>){
        children.forEach { addChild(it) }
    }

    open fun addChildren(position: Int, list : List<StructuralElement>){
        verifyChildrenToInsert(list)
        for(child in list){
            if(children.contains(child)) throw IllegalArgumentException("Child already present")
        }
        preChildrenSetup(list)
        list.forEach { it.parent = this }
        (children as MutableList<StructuralElement>).addAll(position, list)
    }


    /**
     * Subclasses overriding this will need to call super method.
     *
     * Make sure that after we kill children, we do not leave a mess (eg dangling cross-tree dependencies)
     */
    open fun callWinstonWolfe(){
        children.forEach{it.callWinstonWolfe()}
    }

    //https://preview.redd.it/hg27vjl7x0241.jpg?auto=webp&s=d3c8b5d2cfbf12a05715271e0cf7f1c26e962827
    open fun killAllChildren(){

        //we are prepared... we call Winston Wolfe before the "mess"...
        callWinstonWolfe()

        children.forEach {
            it.parent = null; //let's avoid memory leaks
        }
        children.clear()
        groups?.clear()
    }

    open fun killChildren(predicate: (StructuralElement) -> Boolean){
        val toRemove = children.filter(predicate)
        for(child in toRemove){
            killChild(child)
        }
    }

    open fun killChildren(toKill: List<out StructuralElement>){
        for(child in toKill){
            killChild(child)
        }
    }

    open fun killChild(child: StructuralElement){
        val index = children.indexOf(child)
        killChildByIndex(index)
    }

    open fun killChildByIndex(index: Int) : StructuralElement{
        val groupId = groups?.groupForChild(index)?.id
        val child = children.removeAt(index)

        child.callWinstonWolfe()

        child.parent = null
        groups?.removedFromGroup(groupId!!)
        return  child
    }



    fun swapChildren(position1: Int, position2: Int){
        if(position1 > children.size || position2 > children.size)
            throw IllegalArgumentException("position is out of range of list")
        if(position1 == position2)
            throw IllegalArgumentException("It is not necessary to swap two same positions")

        if(groups != null && ! groups!!.areChildrenInSameGroup(position1,position2)){
            throw IllegalArgumentException("Cannot swap children in different groups")
        }

        val first = children[position1]
        (children as MutableList<StructuralElement>)[position1] = children[position2]
        (children as MutableList<StructuralElement>)[position2] = first
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
    protected abstract fun copyContent(): StructuralElement

    /**
     * post-handling on the copy based on its [original] version
     */
    protected open fun postCopy(original : StructuralElement){
        if (children.size != original.children.size)
            throw IllegalStateException("copy has different size of children compared to original, e.g., copy (${children.size}) vs. original (${original.children.size})")
        children.indices.forEach {
            children[it].postCopy(original.children[it])
        }
        if(original.groups != null && this.groups == null){
            //the copy content didn't setup group, let's do it here
            groups = original.groups!!.copy(children)
        }
    }

    /**
     * make a deep copy
     * @return a new Copyable based on [this]
     */
    open fun copy() : StructuralElement {
        val copy = copyContent()
        if (hasLocalId())
            copy.setLocalId(localId)
        Lazy.assert {
            copy.getLocalId() == localId
        }
        copy.postCopy(this)
        return copy
    }

    /**
     * @return root of the element which can not be null.
     * If [this] is the root, return [this]
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
     * @return whether there exist any parent satisfies the specified predicate
     */
    fun existAnyParent(predicate: (StructuralElement) -> Boolean): Boolean{
        if (parent!= null){
            if (predicate(parent!!)) return true
            return parent!!.existAnyParent(predicate)
        }
        return false
    }

    /**
     * @return the first parent satisfies the specified predicate
     */
    fun getFirstParent(predicate: (StructuralElement) -> Boolean) : StructuralElement?{
        if (parent == null) return null
        if (predicate(parent!!))
            return parent
        return parent!!.getFirstParent(predicate)
    }

    /**
     * @return the first parent of the given type, or null if none found
     */
    fun <T:StructuralElement> getFirstParent(klass: Class<T>) : T? {
        return getFirstParent { klass.isAssignableFrom(it.javaClass) } as T?
    }
}
