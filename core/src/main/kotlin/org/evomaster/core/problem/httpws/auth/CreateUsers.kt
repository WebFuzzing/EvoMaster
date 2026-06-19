package org.evomaster.core.problem.httpws.auth

import com.webfuzzing.commons.auth.CreateUsers
import org.evomaster.core.problem.rest.data.ContentType
import org.evomaster.core.problem.rest.data.HttpVerb


class CreateUsers(

    val name: String,

    val call: CallToEndpoint,

    val generators: List<Generator>
){
    init {

        val payload = call.payload
            //TODO could handle payload in headers in future, and so this could be null
            ?: throw IllegalArgumentException("No payload provided")

        for (generator in generators) {
            val placeholder = generator.placeHolder
            if(!payload.contains(placeholder)) {
                throw IllegalArgumentException("Payload does not contain the placeholder '$placeholder': $payload")
            }
        }
    }

    companion object {
        fun fromDto(name: String, dto: CreateUsers) : org.evomaster.core.problem.httpws.auth.CreateUsers{
            return CreateUsers(
                name = name,
                call = CallToEndpoint(
                    endpoint = dto.endpoint,
                    externalEndpointURL = dto.externalEndpointURL,
                    payload = dto.payloadRaw,
                    verb = HttpVerb.valueOf(dto.verb.toString()),
                    contentType = dto.contentType.let { ContentType.from(it)},
                    headers = listOf() // TODO
                ),
                generators = dto.generators.map {Generator.fromDto(it)}
            )
        }
    }
}