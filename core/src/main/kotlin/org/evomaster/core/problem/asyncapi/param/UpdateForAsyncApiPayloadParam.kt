package org.evomaster.core.problem.asyncapi.param

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.api.param.UpdateForParam
import org.evomaster.core.search.gene.Gene

/**
 * The payload an opaque one is to be replaced with, at the next mutation of the action: the
 * shape of the class the service was seen reading the message into.
 *
 * The AsyncAPI counterpart of [org.evomaster.core.problem.rest.param.UpdateForBodyParam]: an
 * evaluated individual must not change what it sends, so the new payload waits here until
 * [org.evomaster.core.search.service.mutator.StandardMutator] swaps it in on a copy.
 *
 * Named apart from [AsyncApiParam.PAYLOAD] so that nothing looking a payload up by name, such as
 * the fitness building what the driver publishes, picks this one.
 */
class UpdateForAsyncApiPayloadParam(gene: Gene) : Param(NAME, gene), UpdateForParam {

    companion object {
        const val NAME = "updateForAsyncApiPayloadParam"
    }

    override fun copyContent(): Param = UpdateForAsyncApiPayloadParam(gene.copy())

    /**
     * Only the payload is replaced. The headers of the same message are left as they are.
     */
    override fun isSameTypeWithUpdatedParam(param: Param): Boolean =
        param is AsyncApiParam && param.name == AsyncApiParam.PAYLOAD

    override fun getUpdatedParam(): Param = AsyncApiParam(AsyncApiParam.PAYLOAD, gene.copy())
}
