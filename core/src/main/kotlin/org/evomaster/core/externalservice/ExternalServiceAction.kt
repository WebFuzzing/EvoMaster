package org.evomaster.core.externalservice

import com.github.tomakehurst.wiremock.WireMockServer
import org.evomaster.core.problem.external.service.ExternalServiceInfo
import org.evomaster.core.search.Action
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness

/**
 * Ignore this class added for future implementation
 */
class ExternalServiceAction (
    val externalServiceInfo: ExternalServiceInfo
        ) : Action(listOf()) {

    private lateinit var wireMockServer: WireMockServer

    fun getWireMockServer() : WireMockServer {
        return wireMockServer
    }

    override fun getName(): String {
        return "${externalServiceInfo.remoteHostname}:${externalServiceInfo.remotePort}"
    }

    override fun seeGenes(): List<out Gene> {
        TODO("Not yet implemented")
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, all: List<Action>) {
        TODO("Not yet implemented")
    }

    override fun getChildren(): List<out StructuralElement> {
        TODO("Not yet implemented")
    }

    override fun copyContent(): StructuralElement {
        return ExternalServiceAction(externalServiceInfo)
    }
}