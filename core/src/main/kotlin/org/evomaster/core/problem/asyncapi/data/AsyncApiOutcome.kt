package org.evomaster.core.problem.asyncapi.data

/**
 * What came of publishing one message, as the driver reported it.
 */
enum class AsyncApiOutcome {

    /**
     * Published, and no reply was expected.
     */
    PUBLISHED,

    /**
     * Published, and a reply arrived that answers it.
     */
    REPLIED,

    /**
     * Published, a reply was expected, and none arrived within the time waited.
     */
    NO_REPLY,

    /**
     * The driver could not put the message on the wire at all.
     */
    PUBLISH_FAILED
}
