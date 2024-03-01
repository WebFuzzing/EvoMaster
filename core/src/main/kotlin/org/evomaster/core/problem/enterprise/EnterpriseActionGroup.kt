package org.evomaster.core.problem.enterprise

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionDependentGroup
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.search.*


/**
 * This is used to represent a single MAIN action (eg call to API), which is linked to a group
 * of dependent actions (eg setting up wiremock).
 * The dependent actions are related ONLY to the MAIN action.
 * Deleting the MAIN action would imply it is 100% safe to remove the dependent ones.
 */
class EnterpriseActionGroup<T: Action>(
    children: MutableList<out Action>,
    private val mainClass : Class<T>,
    //Kotlin does not like a reference of input val in the lambda, resulting in null when call super constructor
    //childTypeVerifier: (Class<*>) -> Boolean = {k -> ExternalServiceAction::class.java.isAssignableFrom(k) || mainClass.isAssignableFrom(k.javaClass)},
    groups: GroupsOfChildren<out Action> = GroupsOfChildren(
        children,
        listOf(
            ChildGroup(GroupsOfChildren.EXTERNAL_SERVICES
                , { e -> e is ApiExternalServiceAction }),
            ChildGroup(GroupsOfChildren.MAIN, { k -> mainClass.isAssignableFrom(k.javaClass) }, 0, 0, 1)
        )
    )
) : ActionDependentGroup(
    children,
    groups = groups
) {

     constructor(action: T): this(mutableListOf(action), action.javaClass)

    /**
     * @return the main action toward the SUT. the can be only 1 in this group
     */
    fun getMainAction() = children[groupsView()!!.startIndexForGroupInsertionInclusive(GroupsOfChildren.MAIN)]
        as Action


    /**
     * @return all the actions related to setup the main actions.
     * This might be empty.
     */
    fun getExternalServiceActions() : List<ApiExternalServiceAction>{
        return groupsView()!!.getAllInGroup(GroupsOfChildren.EXTERNAL_SERVICES) as List<ApiExternalServiceAction>
    }


    override fun copyContent(): EnterpriseActionGroup<T> {

        val k = children.map { it.copy() } as MutableList<out Action>

        return EnterpriseActionGroup(
            k,
            mainClass,
            groupsView()!!.copy(k) as GroupsOfChildren<Action>
        )
    }
}
