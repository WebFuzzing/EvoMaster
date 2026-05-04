package org.evomaster.core.problem.asyncapi.schema

/**
 * Reply description on an AsyncAPI 3.0 `request`/`reply` operation.
 *
 * AsyncAPI declares correlation through the `correlationId` field on a Message:
 *   `correlationId: { location: '$message.header#/<headerName>' }`
 * Only the header location is supported here; payload-located correlation
 * is parsed but flagged unsupported by the action builder (M4) for now.
 */
data class ReplyBinding(
    /** One or more reply channels declared by the operation. */
    val channelNames: List<String>,
    /** Reply message ids (component-level) the operation can produce. */
    val messageIds: List<String>
)
