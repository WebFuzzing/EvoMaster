package org.evomaster.core.problem.httpws.auth

import com.webfuzzing.commons.auth.CreateUsers
import org.evomaster.core.problem.rest.data.ContentType
import org.evomaster.core.problem.rest.data.HttpVerb
import java.net.MalformedURLException
import java.net.URL

class CreateUsers(

    val endpoint: String?,

    val externalEndpointURL: String?,

    val payload: String,

    val verb: HttpVerb,

    val contentType: ContentType,

    val generators: List<Generator>
){
    init {
        if (endpoint == null && externalEndpointURL == null) {
            throw IllegalArgumentException("Either 'endpoint' or 'externalEndpointURL' should be specified")
        }
        if (endpoint != null && externalEndpointURL != null) {
            throw IllegalArgumentException("Cannot have both 'endpoint' and 'externalEndpointURL' specified. It is ambiguous.")
        }
        if (endpoint != null && !endpoint.startsWith("/")) {
            throw IllegalArgumentException(
                "Create endpoint definition must start with a /. It is not a full URL." +
                        " For example: '/users'"
            )
        }
        if (externalEndpointURL != null) {
            try {
                URL(externalEndpointURL)
            } catch (e: MalformedURLException) {
                throw IllegalArgumentException("'externalEndpointURL' is not a valid URL: ${e.message}")
            }
        }
    }

    companion object {
        fun fromDto(dto: CreateUsers) : org.evomaster.core.problem.httpws.auth.CreateUsers{
            return CreateUsers(
                endpoint = dto.endpoint,
                externalEndpointURL = dto.externalEndpointURL,
                payload = dto.payloadRaw,
                verb = HttpVerb.valueOf(dto.verb.toString()),
                contentType = dto.contentType.let { ContentType.from(it)},
                generators = dto.generators.map {Generator.fromDto(it)}
            )
        }
    }
}