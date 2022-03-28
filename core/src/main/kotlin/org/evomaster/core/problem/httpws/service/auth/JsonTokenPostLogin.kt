package org.evomaster.core.problem.httpws.service.auth

import org.evomaster.client.java.controller.api.dto.JsonTokenPostLoginDto

class JsonTokenPostLogin(

        /**
         * The id representing this user that is going to login
         */
        val userId: String,

        /**
         * The endpoint where to execute the login
         */
        val endpoint: String,

        /**
         * The payload to send, as stringified JSON object
         */
        val jsonPayload: String,


        /**
         * How to extract the token from a JSON response, as such
         * JSON could have few fields, possibly nested.
         * It is expressed as a JSON Pointer
         */
        val extractTokenField: String,

        /**
         * When sending out the obtained token in the Authorization header,
         * specify if there should be any prefix (e.g., "Bearer " or "JWT ")
         */
        val headerPrefix: String
) {

    companion object{

        fun fromDto(dto: JsonTokenPostLoginDto) = JsonTokenPostLogin(
                dto.userId, dto.endpoint, dto.jsonPayload, dto.extractTokenField, dto.headerPrefix)

    }

}