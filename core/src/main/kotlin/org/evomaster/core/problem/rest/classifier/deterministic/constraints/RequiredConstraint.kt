package org.evomaster.core.problem.rest.classifier.deterministic.constraints

import org.evomaster.core.problem.rest.classifier.InputField
import org.evomaster.core.problem.rest.classifier.InputFieldType
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.wrapper.OptionalGene

/**
 * Represent parameters that are "required", ie, must be there in the call.
 * Note that such a constraint can be expressed in OpenAPI schemas.
 * Still, the schema might be underspecified
 */
class RequiredConstraint(
    initialCall: RestCallAction
) : ConstraintFor400 {

    private val requiredQueries = mutableSetOf<String>()

    //TODO body

    init {

        val params = initialCall.parameters

        params.filterIsInstance<QueryParam>()
            .forEach {
                /*
                    if marked already as "required" in schema, then it is not going to be optional here
                 */
                val option = it.primaryGene().getWrappedGene(OptionalGene::class.java)
                if(option != null) {
                    requiredQueries.add(it.name)
                }
            }
        //TODO body

    }

    override fun update2xx(input: RestCallAction) {

        val params = input.parameters

        params.filterIsInstance<QueryParam>()
            .filter { requiredQueries.contains(it.name) }
            .forEach {
                val option = it.primaryGene().getWrappedGene(OptionalGene::class.java)
                if(option != null && !option.isActive) {
                    requiredQueries.remove(it.name)
                }
            }

        //TODO body
    }

    override fun checkUnsatisfiedConstraints(input: RestCallAction): Set<InputField> {

        return requiredQueries
            .filter { required ->
                val param = input.parameters.find { p -> p is QueryParam && p.name == required }
                if (param == null) {
                    false
                } else {
                    val option = param.primaryGene().getWrappedGene(OptionalGene::class.java)
                    option != null && !option.isActive
                }
            }.map { InputField(it, InputFieldType.QUERY) }
            .toSet()

        //TODO body
    }


}