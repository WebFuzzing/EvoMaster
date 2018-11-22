package org.evomaster.core.problem.rest.serviceII

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.serviceII.resources.RestResourceCalls
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import java.lang.IllegalStateException

class RestIndividualII(val resourceCalls: MutableList<RestResourceCalls>,
                       sampleType: SampleType,
                       dbInitialization: MutableList<DbAction> = mutableListOf()
) : RestIndividual(resourceCalls.flatMap { it.actions }.toMutableList(), sampleType, dbInitialization) {

    override fun copy(): Individual {
        val calls = resourceCalls.map { it.copy() }.toMutableList()
        return RestIndividualII(
                calls,
                sampleType,
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>
        )
    }

    override fun seeActions(): List<out Action> {
        if(resourceCalls.map { it.actions.size }.sum() != actions.size)
            throw  IllegalStateException("Mismatched between RestResourceCalls and actions")
        return actions
    }

}