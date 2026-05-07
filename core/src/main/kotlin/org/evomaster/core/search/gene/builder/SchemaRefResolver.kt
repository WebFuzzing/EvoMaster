package org.evomaster.core.search.gene.builder

import io.swagger.v3.oas.models.media.Schema

/**
 * Resolves a JSON-Schema `$ref` to a Swagger media [Schema] tree.
 *
 * Decoupling `$ref` lookup behind this interface is what lets
 * [JsonSchemaToGeneConverter] stay protocol-agnostic.  REST wraps an
 * OpenAPI `RestSchema`/`SchemaOpenAPI` pair behind an implementation
 * (which delegates to swagger-parser's resolver); AsyncAPI wraps its
 * own component-schema map.
 *
 * Implementations are expected to be cheap to call repeatedly — the
 * converter does not memoise resolution itself, but it does keep a
 * higher-level `Gene` cache keyed by `$ref` string (because root-mounted
 * gene trees are reusable).
 */
interface SchemaRefResolver {

    /**
     * Resolve [ref] (e.g. `"#/components/schemas/Order"`) to a Swagger
     * [Schema], or `null` if the reference cannot be resolved.  Implementations
     * append diagnostic strings to [messages] when a reference can't be found
     * so the caller can surface them in the run summary.
     */
    fun resolve(ref: String, messages: MutableList<String>): Schema<*>?
}
