package org.evomaster.core.search


/**
 * Group definitions for children.
 * Every time children is modified, we need to make sure to call methods here to keep the groups in sync
 */
class GroupsOfChildren(
    /**
     * Read-only view of current children
     */
    private val children : List<StructuralElement> = listOf(),
    /**
     * These groups are in order. Each one with a unique id.
     * Once groups are defined here in the constructor, cannot be modified.
     */
    private val groups : List<ChildGroup> = listOf()
) {

    companion object{

        //list of main group names

        const val MAIN = "MAIN"

        const val INITIALIZATION_SQL = "INITIALIZATION_SQL"

        const val INITIALIZATION_EXTERNAL_SERVICES = "INITIALIZATION_EXTERNAL_SERVICES"

        const val RESOURCE_SQL = "RESOURCE_SQL"
    }

    private val groupMap = groups.associateBy { it.id }

    init {
        verifyGroups()
    }

    fun copy() = GroupsOfChildren(children, groups.map { it.copy() })

    fun copy(children: List<StructuralElement>) = GroupsOfChildren(children, groups.map { it.copy() })


    fun verifyGroups() {
        if(groups.isEmpty()){
           throw IllegalArgumentException("No group definitions")
        }
        val size = groups.sumOf { sizeOfGroup(it.id) }
        if(size != children.size){
            throw IllegalStateException("There are ${children.size} children, but $size in groups")
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
        groups.forEach { it.startIndex = -1 }
    }

    fun numberOfGroupsInUse() = groups.filter { it.isInUse() }.size

    fun startIndexForGroupInsertionInclusive(id: String) : Int {
        val index = getGroupIndex(id)
        val g = groups[index]
        if(g.isInUse()){
            return g.startIndex
        }
        // the group is currently empty. need to check if other groups are in use
        val previous = groups.indices.indexOfLast { it < index && groups[it].isInUse() }
        if(previous < 0){
            //no previous non-empty group. can start at beginning
            return 0
        }
        //there is a previous. need to determine where it ends
        val p = groups[previous]
        return  p.startIndex + sizeOfGroup(p.id)
    }

    fun endIndexForGroupInsertionInclusive(id: String) : Int {
        val index = getGroupIndex(id)
        val g = groups[index]
        if(!g.isInUse()){
            //if empty, start and end would be the same
            return startIndexForGroupInsertionInclusive(id)
        }
        return g.startIndex + sizeOfGroup(id)
    }

    private fun getGroupIndex(id: String) : Int{
        val index = groups.indexOfFirst { it.id == id }
        if(index < 0){
            throw IllegalArgumentException("Group with id $id does not exist")
        }
        return index
    }

    fun addToGroup(id: String, element: StructuralElement){
        val index = getGroupIndex(id)
        if(! groups[index].canBeInGroup(element)){
            throw IllegalArgumentException("Element $element cannot be added to group $id")
        }

        //"adding" to a group does not impact is starting index.
        groups.withIndex()
            .filter { it.index > index && it.value.isInUse()}
                // all following groups in use increase their start by 1, as shift to right
            .forEach { it.value.startIndex++ }
    }

    fun sizeOfGroup(id: String) : Int {
        val index = groups.indexOfFirst { it.id == id }
        if(index < 0){
            throw IllegalArgumentException("Group with id $id does not exist")
        }
        val g = groups[index]
        if(!g.isInUse()){
            return 0
        }

        val next = groups.withIndex().indexOfFirst { it.index > index && it.value.isInUse() }
        if(next < 0){
            // g is the last group
            assert(index == groups.lastIndex)
            return groups.size - g.startIndex
        }

        return  groups[next].startIndex - g.startIndex
    }

    private fun groupForChild(index: Int) : ChildGroup{
        if(index < 0 || index >= children.size){
            throw IllegalArgumentException("Index $index$ of child is out of bound for child list of size ${children.size}")
        }
        if(groups.isEmpty()){
            throw IllegalStateException("There is no group defined")
        }
        var k = -1
        for(g in groups){
            k += sizeOfGroup(g.id)
            if(index <= k){
                return g
            }
        }
        throw IllegalStateException("Cannot find group for index $index")
    }


}