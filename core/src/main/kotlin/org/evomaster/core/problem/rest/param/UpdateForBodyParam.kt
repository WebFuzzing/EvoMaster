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
class UpdateForBodyParam(
    genes: MutableList<Gene>,
    /**
     * Non-gene state of the [BodyParam] being replaced.
     *
     * [getUpdatedParam] rebuilds a BodyParam from genes alone, so anything that is not a gene has to
     * be carried explicitly or it is silently dropped. Today this is always empty, because the two
     * places that create an UpdateForBodyParam are guarded by `none { it is BodyParam }` and build a
     * fresh JSON body; carrying it is what keeps that from breaking if the mechanism is ever extended
     * to bodies that already exist.
     */
    val parts: Map<String, PartMeta> = mapOf(),
    private val sourceDescription: String? = null
) : Param("updateForBodyParam", genes), UpdateForParam {

    constructor(body: BodyParam) : this(body.seeGenes().toMutableList(), body.parts, body.description)

    override fun copyContent(): Param {
        return UpdateForBodyParam(seeGenes().map { it.copy() }.toMutableList(), parts, sourceDescription)
    }

    override fun isSameTypeWithUpdatedParam(param: Param) : Boolean {
        return param is BodyParam
    }

    override fun getUpdatedParam(): Param {
        return BodyParam(children.map { it.copy() as Gene}, parts)
            .also { it.description = sourceDescription }
    }
}

