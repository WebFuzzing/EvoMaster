package org.evomaster.core.problem.external.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.evomaster.core.problem.external.service.param.ResponseParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils

/**
 * Action to execute the external service related need
 * to handle the external service calls.
 *
 * Typically, handle WireMock responses
 */
class ExternalServiceAction(

    /**
     * Received request to the respective WireMock instance
     *
     * TODO: Need to expand the properties further in future
     * depending on the need
     */
    val request: ExternalServiceRequest,

    /**
     * currently, we support response with json format
     * then use ObjectGene now,
     * might extend it later
     */
    val response: ResponseParam = ResponseParam(),

    /**
     * WireMock server which received the request
     */
    val wireMockServer: WireMockServer,
    private val id: Long,

    val representExistingRequest: Boolean = false
) : Action(listOf(response)) {

    companion object {
        private fun buildResponse(template: String): ResponseParam {
            // TODO: refactor later
            return ResponseParam()
        }
    }

    constructor(request: ExternalServiceRequest, template: String, wireMockServer: WireMockServer, id: Long) :
            this(request, buildResponse(template), wireMockServer, id)


    override fun getName(): String {
        // TODO: Need to change in future
        return request.getID()
    }

    override fun seeGenes(): List<out Gene> {
        return response.genes
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }

    override fun copyContent(): StructuralElement {
        return ExternalServiceAction(request, response.copy() as ResponseParam, wireMockServer, id, representExistingRequest)
    }

    /**
     * Experimental implementation of WireMock stub generation
     *
     * Method should randomize the response code
     */
    fun buildResponse() {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlMatching(request.getURL()))
                .atPriority(1)
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(viewStatus())
                        .withBody(viewResponse())
                )
        )
    }

    private fun viewStatus(): Int {
        return response.status.values[response.status.index]
    }

    private fun viewResponse(): String {
        if (response.selected == -1) return "{}"
        return response.responses[response.selected].getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON)
    }

}