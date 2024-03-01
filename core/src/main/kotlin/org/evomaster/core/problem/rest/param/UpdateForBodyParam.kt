package org.evomaster.core.problem.rest.param

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.api.param.UpdateForParam
import org.evomaster.core.search.gene.Gene

/**
 * After a fitness evaluation, we could extract info from the SUT to expand the genotype
 * of the individual.
 * For example, we could learn how a body payload was parsed for a specific DTO, where this
 * info was missing from the OpenAPI schema.
 * However, we CANNOT modify the phenotype of the individual after evaluation :(
 * So we add info to modify the individual at its next mutation
 *
 */
class UpdateForBodyParam(val body: BodyParam) : Param("updateForBodyParam", body.seeGenes().toMutableList()),
    UpdateForParam {

    /*
        TODO check if issue after refactoring. Previously it was:
        "Note that the children of UpdateForBodyParam is [body] (BodyParam) not [gene] as other types of Param"
     */

    override fun copyContent(): Param {
        return UpdateForBodyParam(body.copy() as BodyParam)
    }


    override fun seeGenes(): List<Gene> {
        return body.seeGenes()
    }

    override fun isSameTypeWithUpdatedParam(param: Param) : Boolean {
        return param is BodyParam
    }

    override fun getUpdatedParam(): Param {
        return body
    }
}

