package org.evomaster.core.problem.rest.arazzo.resolver

import org.evomaster.core.problem.rest.arazzo.models.FailureAction
import org.evomaster.core.problem.rest.arazzo.models.FailureReusable
import org.evomaster.core.problem.rest.arazzo.models.Parameter
import org.evomaster.core.problem.rest.arazzo.models.ParameterReusable
import org.evomaster.core.problem.rest.arazzo.models.SuccessAction
import org.evomaster.core.problem.rest.arazzo.models.SuccessReusable

object ArazzoReferenceResolver {

    fun resolveSuccessActions(items: List<SuccessReusable>?): List<SuccessAction> {
        if (items == null) return emptyList()

        return items.map { item ->
            when (item) {
                is SuccessReusable.Success -> item.action
                //TODO: Implementar referencia del reusable a paremetro
                is SuccessReusable.ReusableObj -> null
            } as SuccessAction
        }
    }

    fun resolveFailureActions(items: List<FailureReusable>?): List<FailureAction> {
        if (items == null) return emptyList()

        return items.map { item ->
            when (item) {
                is FailureReusable.Failure -> item.action
                //TODO: Implementar referencia del reusable a paremetro
                is FailureReusable.ReusableObj -> null
            } as FailureAction
        }
    }

    fun resolveParameters(items: List<ParameterReusable>?): List<Parameter> {
        if (items == null) return emptyList()

        return items.map { item ->
            when (item) {
                is ParameterReusable.Param -> item.parameter
                //TODO: Implementar referencia del reusable a paremetro
                is ParameterReusable.ReusableObj -> null
            } as Parameter
        }
    }

}