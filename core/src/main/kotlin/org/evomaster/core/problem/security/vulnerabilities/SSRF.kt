package org.evomaster.core.problem.security.vulnerabilities

class SSRF {

    companion object {
        const val SSRF_PROMPT_ANSWER_FOR_POSSIBILITY = "TRUE"

        /**
         * This prompt is for SSRF.
         * TODO: In the future, this can be extended to for other classes by using a
         *  custom large language model.
         */
        fun getPromptWithNameAndDescription(name: String, description: String): String {
            return " Consider the word \"${name}\" and the description as \"${description}\", used as a " +
                    "name identifier for a parameter inside a OpenAPI/Swagger schema. Would it likely represent " +
                    "a URL value ? give me answer as boolean with TRUE or FALSE"
        }

        /**
         * This prompt is for SSRF.
         * TODO: In the future, this can be extended to for other classes by using a
         *  custom large language model.
         */
        fun getPromptWithNameOnly(name: String): String {
            return "Consider the word \"${name}\", used as a name identifier for a parameter inside a " +
                    "OpenAPI/Swagger schema. Would it likely represent a URL value ? give me answer as " +
                    "boolean with TRUE or FALSE"
        }
    }
}
