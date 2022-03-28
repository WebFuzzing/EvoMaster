package org.evomaster.core.problem.graphql


/**
 * In GraphQL there are (at least currently) two types of operations:
 * fetch data, and modify data.
 *
 * But, still GraphQL has to go through HTTP.
 * This means that a [QUERY] can be done with a GET, whereas a [MUTATION] must need
 * a non-idempotent verb, typically a POST (and for sure not a GET).
 *
 * However, a [QUERY] can be done with a POST. The reason to do that is if there are issues
 * with URL length (a body payload of a POST has no constraints, but query parameters do have, because
 * there are limitations on URL lengths).
 * Also, to be consistent, could just make sense to use a POST for both anyway
 */
enum class GQMethodType {

    /**
     * Fetch data
     */
    QUERY,

    /**
     * Modify data
     */
    MUTATION
}