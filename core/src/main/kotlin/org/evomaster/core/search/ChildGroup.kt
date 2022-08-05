package org.evomaster.core.search

/**
 * Structural elements can have children.
 * These children can be divided in groups, to simplify the coding.
 * Still, there is only one data structure "children". this grouping is just extra info
 * to work on such data structure
 */
class ChildGroup<T>(
    /**
     * Unique id representing the group
     */
    val id : String,
    /**
     * Function to check if element can belong to this group, typically based on its type
     */
    val canBeInGroup: (T) -> Boolean = {_ -> true},
    /**
     *  index in the children structure where this group begins.
     *  note: groups are in order, so one ends when the next starts. there is no empty gap
     *
     *  A negative index means the group is empty
     */
    var startIndex : Int = -1,
    /**
     * This is inclusive. Negative value means the group is empty
     */
    var endIndex : Int = -1,
    /**
     * How many elements at most there can be in this group
     */
    val maxSize: Int = Int.MAX_VALUE
) {

    override fun toString(): String {
        return "$id [$startIndex,$endIndex]"
    }

    fun copy() = ChildGroup(id,canBeInGroup,startIndex,endIndex,maxSize)

    fun isNotEmpty() = !isEmpty()

    fun isEmpty() = startIndex < 0

    fun isValid() = ((startIndex < 0 && endIndex < 0) || (startIndex >=0 && startIndex <= endIndex))
            && size() <= maxSize

    fun size() = if(isEmpty()) 0 else (endIndex - startIndex + 1)

    fun reset() {
        startIndex = -1
        endIndex = -1
    }
}