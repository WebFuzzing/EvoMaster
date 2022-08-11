package org.evomaster.core.problem.api.service

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.enterprise.EnterpriseIndividual
import org.evomaster.core.problem.external.service.ExternalServiceAction
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionComponent
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.TrackOperator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max

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
    children: List<out ActionComponent>
): EnterpriseIndividual(trackOperator, index, children){


}