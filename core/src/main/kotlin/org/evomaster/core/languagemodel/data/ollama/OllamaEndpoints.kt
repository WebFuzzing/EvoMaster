package org.evomaster.core.languagemodel.data.ollama

class OllamaEndpoints {

    companion object {
        /**
         * API URL to generate a response for a given prompt with a provided model.
         */
        const val GENERATE_ENDPOINT = "/api/generate"

        /**
         * API URL to list models that are available locally.
         */
        const val TAGS_ENDPOINT = "/api/tags"

        fun getGenerateEndpoint(serverURL: String): String {
            return cleanURL(serverURL) + GENERATE_ENDPOINT
        }

        fun getTagEndpoint(serverURL: String): String {
            return cleanURL(serverURL) + TAGS_ENDPOINT
        }

        private fun cleanURL(serverURL: String): String {
            return if (serverURL.endsWith("/")) serverURL.dropLast(1) else serverURL
        }
    }


}
