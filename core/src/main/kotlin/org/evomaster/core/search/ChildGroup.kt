package org.evomaster.core.search

/**
 * Structural elements can have children.
 * These children can be divided in groups, to simplify the coding.
 * Still, there is only one data structure "children". this grouping is just extra info
 * to work on such data structure
 */
class ChildGroup(
    /**
     * Unique id representing the group
     */
    val id : String,
    /**
     * Function to check if element can belong to this group, typically based on its type
     */
    val canBeInGroup: (StructuralElement) -> Boolean,
    /**
     *  index in the children structure where this group begins.
     *  note: groups are in order, so one ends when the next starts.
     *  so, here we do not store info about the group length, nor its end, as it would be yet another
     *  value that would need updating after each insert/remove of children
     *
     *  A negative index means the group is empty
     */
    var startIndex : Int = -1
) {

    fun copy() = ChildGroup(id,canBeInGroup,startIndex)

    fun isInUse() = startIndex >= 0
}