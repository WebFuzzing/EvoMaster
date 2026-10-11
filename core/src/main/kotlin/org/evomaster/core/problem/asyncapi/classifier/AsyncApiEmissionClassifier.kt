package org.evomaster.core.problem.asyncapi.classifier

import com.webfuzzing.asyncapi.models.AsyncApiChannel
import com.webfuzzing.asyncapi.models.AsyncApiDocument
import com.webfuzzing.asyncapi.models.AsyncApiMessage
import com.webfuzzing.asyncapi.models.AsyncApiOperation

/**
 * Tells what a contract says about an address the service was seen emitting a message to: that
 * the service sends there, that the address is declared for something else, or nothing at all.
 *
 * The service sends to the channels of `send` operations. Their addresses may have
 * `{placeholders}`, e.g. `user.{userId}.signedup`, which a concrete `user.42.signedup` matches.
 * Any other channel, consumed by the service or carrying a reply, still counts as declared.
 *
 * A channel that names no address, neither its own nor in a binding, gets one at run time, which
 * could be any. If the document has one, every address counts as declared, and that channel is
 * never what [sendChannelOf] gives.
 */
class AsyncApiEmissionClassifier(private val document: AsyncApiDocument) {

    companion object {

        /**
         * What a placeholder in an address looks like, e.g. `{userId}`.
         */
        private val PARAMETER = Regex("\\{[^{}]+}")

        /**
         * What a placeholder matches in a concrete address: any non-empty text. Constraints the
         * document puts on a parameter are not checked, so an address breaking only those is
         * still recognised.
         */
        private const val PARAMETER_VALUE = ".+"

        /**
         * Where a message on [channel] goes on the wire: its address, unless the binding of its
         * server's protocol overrides it, or the Kafka topic of the binding when the channel
         * declares no address at all. Null when the channel names neither, so the address is
         * decided at run time and there is nothing to match against.
         *
         * Placeholders are left in place.
         */
        fun wireAddressOf(document: AsyncApiDocument, channel: AsyncApiChannel): String? {

            val protocol = document.serversOf(channel).firstOrNull()?.protocol

            /*
                With Kafka the topic often lives only in the binding. The protocol is only known
                when the document declares a server, so with no address the binding's topic is
                taken regardless: it is the one destination the document names.
             */
            return channel.effectiveAddress(protocol) ?: channel.bindings?.kafkaTopic
        }

        /**
         * A pattern matching the concrete addresses [address] stands for: every placeholder is
         * some non-empty text, and the rest is taken literally.
         */
        private fun patternOf(address: String): Regex =
            Regex(address.split(PARAMETER).joinToString(PARAMETER_VALUE) { if (it.isEmpty()) it else Regex.escape(it) })
    }

    /**
     * A channel the service sends to, as some `send` operation declares.
     *
     * @param address  the address as the document writes it, placeholders included
     * @param messages what the `send` operations on the channel say may be published there, in
     *                 declaration order and distinct
     */
    class SendChannel(
        val channelName: String,
        val address: String,
        val messages: List<AsyncApiMessage>
    ) {
        internal val pattern: Regex = patternOf(address)
    }

    /**
     * Every channel some `send` operation acts on that has a wire address.
     */
    private val sendChannels: List<SendChannel> = buildSendChannels()

    /**
     * The wire addresses of every channel in the document, whatever the operations on it, sends
     * included, placeholders left in place.
     */
    private val addresses: List<String> = document.channels.values
        .mapNotNull { wireAddressOf(document, it) }
        .distinct()

    /**
     * Patterns of [addresses], in the same order.
     */
    private val declared: List<Regex> = addresses.map { patternOf(it) }

    /**
     * Whether some channel of the document has no wire address, so that its address is decided
     * at run time and may be any.
     */
    private val hasRuntimeAddress: Boolean = document.channels.values.any { wireAddressOf(document, it) == null }

    /**
     * The channel a `send` operation declares for [address], or null when no `send` operation
     * acts on a channel with that address.
     *
     * A channel naming [address] exactly wins over one whose placeholders would match it, even
     * when only the latter is sent to: with `orders.{orderId}` declared for sending and
     * `orders.placed` for receiving, a message to `orders.placed` went to the channel the
     * service consumes, not to an order called "placed".
     */
    fun sendChannelOf(address: String): SendChannel? {

        if (addresses.contains(address)) {
            return sendChannels.firstOrNull { it.address == address }
        }

        return sendChannels.firstOrNull { it.pattern.matches(address) }
    }

    /**
     * Whether any channel of the document has [address], whatever the operations on it. Always
     * true when some channel has its address decided at run time, as [address] may be that one.
     */
    fun isDeclared(address: String): Boolean =
        hasRuntimeAddress || declared.any { it.matches(address) }

    private fun buildSendChannels(): List<SendChannel> {

        /*
            Several operations may send on one channel, each selecting some of its messages: what
            may be published there is all of them together.
            Key is the channel name, value is the messages declared for it so far.
         */
        val messages = LinkedHashMap<String, LinkedHashSet<AsyncApiMessage>>()

        document.operations.values
            .filter { it.action == AsyncApiOperation.Action.SEND }
            .forEach { op ->
                messages.getOrPut(op.channelName) { LinkedHashSet() }.addAll(document.messagesOf(op))
            }

        return messages.mapNotNull { (name, declared) ->
            val channel = document.channels[name] ?: return@mapNotNull null
            val address = wireAddressOf(document, channel) ?: return@mapNotNull null
            SendChannel(name, address, declared.toList())
        }
    }
}

