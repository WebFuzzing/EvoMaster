package org.evomaster.core.problem.api

import org.evomaster.core.problem.enterprise.EnterpriseIndividual
import org.evomaster.core.search.ActionComponent
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.tracer.TrackOperator

/**
 * the abstract individual for API based SUT, such as REST, GraphQL, RPC
 */
abstract class ApiWsIndividual (

    /**
     * a tracked operator to manipulate the individual (nullable)
     */
    trackOperator: TrackOperator? = null,
    /**
     * an index of individual indicating when the individual is initialized during the search
     * negative number means that such info is not collected
     */
    index : Int = -1,
    /**
     * a list of children of the individual
     */
    children: MutableList<out ActionComponent>,
    childTypeVerifier: (Class<*>) -> Boolean,
    groups : GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(children, children.size, 0)
): EnterpriseIndividual(trackOperator, index, children, childTypeVerifier, groups){


}