package org.evomaster.core.problem.asyncapi.schema

/**
 * AsyncAPI 3.0 operation — what an application does on a channel.
 *
 * `SEND` operations are publish-from-the-application's-perspective. From
 * EvoMaster's perspective these are also publish operations: EvoMaster sends
 * a message that the SUT, declared as the application, would consume.
 *
 * The asymmetry matters when [reply] is set — the operation describes a
 * request/reply pair, and EvoMaster will subscribe to the reply channel after
 * publishing.
 */
data class AsyncAPIOperation(
    /** Map key in `operations:` (e.g. `sendNcsRequest`). */
    val name: String,
    val action: Action,
    /** Channel key (resolved from `$ref: '#/channels/<key>'`). */
    val channelName: String,
    /** Message ids the operation can carry on its main channel. */
    val messageIds: List<String>,
    /** Reply binding when the operation declares `reply:`. */
    val reply: ReplyBinding?,
    /**
     * Security-scheme component names referenced from this operation's
     * `security:` array. Each entry maps to an [AsyncAPISecurityScheme] in
     * [AsyncAPISchema.securitySchemes]. Empty when the operation declares no
     * security or when global-only security applies.
     */
    val security: List<String> = emptyList()
) {
    enum class Action { SEND, RECEIVE }
}
