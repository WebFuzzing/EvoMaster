package org.evomaster.core.search


/**
 * Group definitions for children.
 * Every time children is modified, we need to make sure to call methods here to keep the groups in sync
 */
class  GroupsOfChildren<T>(
    /**
     * Read-only view of current children
     */
    private val children : List<T> = listOf(),
    /**
     * These groups are in order. Each one with a unique id.
     * Once groups are defined here in the constructor, cannot be modified.
     */
    private val groups : List<ChildGroup<T>> = listOf()
) {

    companion object{

        //list of main group names

        const val MAIN = "MAIN"

        const val INITIALIZATION_SQL = "INITIALIZATION_SQL"

        const val INITIALIZATION_MONGO = "INITIALIZATION_MONGO"

        const val INITIALIZATION_DNS = "INITIALIZATION_DNS"

        const val EXTERNAL_SERVICES = "EXTERNAL_SERVICES"

        const val RESOURCE_SQL = "RESOURCE_SQL"
    }

    private val groupMap = groups.associateBy { it.id }

    init {
        verifyGroups()
    }

    fun copy(children: List<T>) = GroupsOfChildren(children, groups.map { it.copy() })


    fun verifyGroups() {
        if(groups.size < 2){
            throw IllegalArgumentException("There should be at least 2 groups")
        }
        if(groups.map { it.id }.toSet().size != groups.size){
            throw IllegalArgumentException("Group ids must be unique")
        }
        groups.forEach {
            if(!it.isValid()){
                throw IllegalArgumentException("Invalid group range for $it")
            }
        }
        val size = groups.sumOf {it.size() }
        if(size != children.size){
            throw IllegalStateException("There are ${children.size} children, but $size in groups")
        }
        val inUse = groups.filter { it.isNotEmpty() }
        for(i in 0..inUse.size-2){
            val current = inUse[i]
            val next = inUse[i+1]
            if(current.endIndex+1  != next.startIndex){
                throw IllegalArgumentException("There is a gap between $current and $next")
            }
        }

        children.forEachIndexed { index, element ->
            val g = groupForChild(index)
            if(! g.canBeInGroup(element)){
                throw IllegalStateException("Element at position $index is not valid for group ${g.id}")
            }
        }
    }

    fun clear(){
        if(children.isNotEmpty()){
           throw IllegalStateException("Children is not empty")
        }
        groups.forEach { it.reset() }
    }

    fun numberOfGroupsInUse() = groups.filter { it.isNotEmpty() }.size

    /**
     * Inserting at this position is fine, and will make the element the first
     * in the group.
     * Inserting before could end up in a different group.
     */
    fun startIndexForGroupInsertionInclusive(id: String) : Int {
        val index = getGroupIndex(id)
        val g = groups[index]
        if(g.isNotEmpty()){
            return g.startIndex
        }
        // the group is currently empty. need to check if other groups are in use
        val previous = groups.indices.indexOfLast { it < index && groups[it].isNotEmpty() }
        if(previous < 0){
            //no previous non-empty group. can start at beginning
            return 0
        }
        //there is a previous. need to determine where it ends
        val p = groups[previous]
        return  p.endIndex + 1
    }

    /**
     * Inserting here is fine, making new element the last in the group.
     */
    fun endIndexForGroupInsertionInclusive(id: String) : Int {
        val index = getGroupIndex(id)
        val g = groups[index]
        if(!g.isNotEmpty()){
            //if empty, start and end would be the same
            return startIndexForGroupInsertionInclusive(id)
        }
        return g.endIndex + 1
    }

    fun getGroup(id: String) : ChildGroup<T>{
        return groups[getGroupIndex(id)]
    }

    private fun getGroupIndex(id: String) : Int{
        val index = groups.indexOfFirst { it.id == id }
        if(index < 0){
            throw IllegalArgumentException("Group with id $id does not exist")
        }
        return index
    }

    fun sizeOfGroup(id: String) = groupMap[id]?.size() ?: throw IllegalArgumentException("No group $id")
    fun getAllInGroup(id: String) : List<T>{
        val g = groupMap[id] ?: throw IllegalArgumentException("Invalid group id $id")
        if(!g.isNotEmpty()){
            return listOf()
        }
        return children.subList(g.startIndex, g.endIndex+1)
    }

    fun areChildrenInSameGroup(i: Int, j: Int) : Boolean{
        val a = groupForChild(i)
        val b = groupForChild(j)
        return a.id == b.id
    }

    fun addedToGroup(id: String, element: T){
        val index = getGroupIndex(id)
        val g = groups[index]
        if(! g.canBeInGroup(element)){
            throw IllegalArgumentException("Element $element cannot be added to group $id")
        }

        val size = g.size()
        if(size == g.maxSize){
            throw IllegalArgumentException("Group has already reached its max size of ${g.maxSize}")
        }

        if(g.isEmpty()){
            g.startIndex = startIndexForGroupInsertionInclusive(id)
            g.endIndex = g.startIndex
        } else {
            g.endIndex++
        }

        //"adding" to a group does not impact its starting index.
        groups.withIndex()
            .filter { it.index > index && it.value.isNotEmpty()}
                // all following groups in use will be shift by 1 to the right
            .forEach {
                it.value.startIndex++
                it.value.endIndex++
            }
    }

    fun removedFromGroup(id: String){
        val index = getGroupIndex(id)
        val g = groups[index]
        val size = g.size()
        if(size == 0){
            throw IllegalArgumentException("Cannot remove from an empty group")
        }
        if(size == 1){
            g.reset()
        } else {
            g.endIndex--
        }

        groups.withIndex()
            .filter { it.index > index && it.value.isNotEmpty()}
            // all following groups in use are shift to left by 1
            .forEach {
                it.value.startIndex--
                it.value.endIndex--
            }

    }

    fun groupForChild(index: Int) : ChildGroup<T>{
        if(index < 0 || index >= children.size){
            throw IllegalArgumentException("Index $index$ of child is out of bound for child list of size ${children.size}")
        }
        if(groups.isEmpty()){
            throw IllegalStateException("There is no group defined")
        }
        var k = -1
        for(g in groups){
            k += g.size()
            if(index <= k){
                return g
            }
        }
        throw IllegalStateException("Cannot find group for index $index")
    }


}