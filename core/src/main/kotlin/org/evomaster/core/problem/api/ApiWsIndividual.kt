package org.evomaster.core.problem.api

import org.evomaster.core.problem.enterprise.EnterpriseChildTypeVerifier
import org.evomaster.core.problem.enterprise.EnterpriseIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.tracer.TrackOperator

/**
 * the abstract individual for API based SUT, such as REST, GraphQL, RPC
 */
abstract class ApiWsIndividual (
    sampleType: SampleType,

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
    childTypeVerifier: EnterpriseChildTypeVerifier,
    groups : GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(children, children.size, 0, 0, 0)
): EnterpriseIndividual(sampleType, trackOperator, index, children, childTypeVerifier, groups){


}
