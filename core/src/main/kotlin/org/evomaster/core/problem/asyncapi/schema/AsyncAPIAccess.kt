package org.evomaster.core.problem.asyncapi.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.gene.builder.SchemaRefUtils
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Response

/**
 * Loads and parses an AsyncAPI 3.0 schema document into the internal
 * [AsyncAPISchema] model.
 *
 * External `$ref` documents (cross-file, relative paths, remote http(s)) are
 * resolved eagerly during [parse] by [inlineExternalRefs], which loads each
 * referenced source, namespaces its `components.schemas` and `components.messages`
 * under synthetic keys in the primary document, and rewrites the in-place
 * `$ref` strings to point at those synthetic keys. After the pre-pass the rest
 * of the parser only sees intra-document refs and downstream code (the gene
 * builder's [AsyncAPISchemaRefResolver]) stays oblivious to external loading.
 *
 * Out of scope for the starter slice: AsyncAPI 2.x (rejected explicitly with a
 * clear error so users understand what to do), authenticated schema retrieval.
 */
object AsyncAPIAccess {

    private val log = LoggerFactory.getLogger(AsyncAPIAccess::class.java)

    private val yamlMapper = ObjectMapper(YAMLFactory())
    private val jsonMapper = ObjectMapper()

    private const val LOCAL_MESSAGE_REF_PREFIX = "#/components/messages/"
    private const val LOCAL_SCHEMA_REF_PREFIX = "#/components/schemas/"
    private const val LOCAL_CHANNEL_REF_PREFIX = "#/channels/"

    /**
     * Retrieve and parse an AsyncAPI schema. The location can be a remote URL
     * in http(s), a `file://` URL, or a local OS file path.
     */
    fun getAsyncAPIFromLocation(asyncApiLocation: String): AsyncAPISchema {
        val data: String
        val location: SchemaLocation
        if (asyncApiLocation.startsWith("http", true)) {
            data = readFromRemoteServer(asyncApiLocation)
            location = SchemaLocation(asyncApiLocation, SchemaLocationType.REMOTE)
        } else {
            data = readFromDisk(asyncApiLocation)
            location = SchemaLocation(asyncApiLocation, SchemaLocationType.LOCAL)
        }
        return parse(data, location)
    }

    /** Test-only helper: load the schema from a classpath resource. */
    fun getAsyncAPIFromResource(resourceLocation: String): AsyncAPISchema {
        val data = this.javaClass.getResource(resourceLocation)?.readText()
            ?: throw SutProblemException("AsyncAPI resource not found on classpath: $resourceLocation")
        return parse(data, SchemaLocation(resourceLocation, SchemaLocationType.RESOURCE))
    }

    fun parse(schemaText: String, location: SchemaLocation): AsyncAPISchema {

        val root = readTree(schemaText)

        val versionNode = root.get("asyncapi")
            ?: throw SutProblemException("AsyncAPI schema missing top-level 'asyncapi' version field")
        val version = versionNode.asText()
        if (!version.startsWith("3.")) {
            throw SutProblemException(
                "AsyncAPI version $version is not supported in this build; only 3.x is parsed." +
                        " (AsyncAPI 2.x support is planned as a follow-up.)"
            )
        }

        if (root is ObjectNode) {
            inlineExternalRefs(root, location)
        }

        val defaultContentType = root.get("defaultContentType")?.asText() ?: "application/json"
        val servers = parseServers(root.get("servers"))
        val componentMessages = parseComponentMessages(root, defaultContentType)
        val componentSchemas = parseComponentSchemas(root)
        val securitySchemes = parseSecuritySchemes(root)
        val channels = parseChannels(root.get("channels"))
        val operations = parseOperations(root.get("operations"))

        return AsyncAPISchema(
            rawText = schemaText,
            location = location,
            version = version,
            channels = channels,
            operations = operations,
            messages = componentMessages,
            componentSchemas = componentSchemas,
            defaultContentType = defaultContentType,
            servers = servers,
            securitySchemes = securitySchemes
        )
    }

    private fun readTree(schemaText: String): JsonNode {
        val trimmed = schemaText.trimStart()
        val mapper = if (trimmed.startsWith("{")) jsonMapper else yamlMapper
        return try {
            mapper.readTree(schemaText)
        } catch (e: Exception) {
            throw SutProblemException("Failed to parse AsyncAPI schema: ${e.message}")
        }
    }

    private fun parseServers(node: JsonNode?): Map<String, AsyncAPIServer> {
        if (node == null || !node.isObject) return emptyMap()
        val out = LinkedHashMap<String, AsyncAPIServer>()
        node.fields().forEach { (name, server) ->
            val host = server.get("host")?.asText() ?: ""
            val protocol = server.get("protocol")?.asText() ?: ""
            out[name] = AsyncAPIServer(name = name, host = host, protocol = protocol)
        }
        return out
    }

    private fun parseComponentMessages(
        root: JsonNode,
        defaultContentType: String
    ): Map<String, AsyncAPIMessage> {
        val components = root.get("components") ?: return emptyMap()
        val messagesNode = components.get("messages") ?: return emptyMap()
        val out = LinkedHashMap<String, AsyncAPIMessage>()
        messagesNode.fields().forEach { (id, message) ->
            val payload = message.get("payload")
            val payloadRefRaw = payload?.get("\$ref")?.asText()
            val payloadRef = payloadRefRaw?.removePrefix(LOCAL_SCHEMA_REF_PREFIX)
                ?.takeIf { it != payloadRefRaw }
            val payloadInline = if (payload != null && payloadRef == null) payload else null

            val correlationLocation = message.get("correlationId")?.get("location")?.asText()

            val headers = message.get("headers")
            val headersRefRaw = headers?.get("\$ref")?.asText()
            val headersRef = headersRefRaw?.removePrefix(LOCAL_SCHEMA_REF_PREFIX)
                ?.takeIf { it != headersRefRaw }
            val headersInline = if (headers != null && headersRef == null) headers else null

            // AsyncAPI 3.0 protocol bindings: per the asyncapi/bindings/kafka
            // spec, message-level kafka.key is the routing-key schema we want
            // the EA to mutate independently of payload + headers.
            val kafkaKey = message.get("bindings")
                ?.get("kafka")
                ?.get("key")

            out[id] = AsyncAPIMessage(
                id = id,
                name = message.get("name")?.asText() ?: id,
                contentType = message.get("contentType")?.asText() ?: defaultContentType,
                correlationLocation = correlationLocation,
                payloadSchemaRef = payloadRef,
                payloadInline = payloadInline,
                headersSchemaRef = headersRef,
                headersInline = headersInline,
                kafkaKeyInline = kafkaKey
            )
        }
        return out
    }

    private fun parseComponentSchemas(root: JsonNode): Map<String, JsonNode> {
        val components = root.get("components") ?: return emptyMap()
        val schemasNode = components.get("schemas") ?: return emptyMap()
        val out = LinkedHashMap<String, JsonNode>()
        schemasNode.fields().forEach { (id, schema) -> out[id] = schema }
        return out
    }

    private fun parseSecuritySchemes(root: JsonNode): Map<String, AsyncAPISecurityScheme> {
        val components = root.get("components") ?: return emptyMap()
        val node = components.get("securitySchemes") ?: return emptyMap()
        if (!node.isObject) return emptyMap()
        val out = LinkedHashMap<String, AsyncAPISecurityScheme>()
        node.fields().forEach { (name, schemeNode) ->
            val type = schemeNode.get("type")?.asText()?.lowercase()
                ?: return@forEach // skip malformed entries; the parser is lenient on auth
            out[name] = AsyncAPISecurityScheme(
                name = name,
                type = type,
                `in` = schemeNode.get("in")?.asText(),
                scheme = schemeNode.get("scheme")?.asText(),
                bearerFormat = schemeNode.get("bearerFormat")?.asText(),
                description = schemeNode.get("description")?.asText()
            )
        }
        return out
    }

    private fun parseChannels(node: JsonNode?): Map<String, AsyncAPIChannel> {
        if (node == null || !node.isObject) return emptyMap()
        val out = LinkedHashMap<String, AsyncAPIChannel>()
        node.fields().forEach { (key, channel) ->
            val address = channel.get("address")?.asText()
            val messages = channel.get("messages")
            val messageIds = mutableListOf<String>()
            messages?.fields()?.forEach { (_, ref) ->
                resolveLocalRef(ref, LOCAL_MESSAGE_REF_PREFIX)?.let { messageIds.add(it) }
            }
            val parameters = LinkedHashMap<String, JsonNode>()
            channel.get("parameters")?.takeIf { it.isObject }?.fields()?.forEach { (paramName, paramNode) ->
                parameters[paramName] = paramNode
            }
            out[key] = AsyncAPIChannel(
                name = key,
                address = address,
                messageIds = messageIds,
                parameters = parameters
            )
        }
        return out
    }

    private fun parseOperations(node: JsonNode?): Map<String, AsyncAPIOperation> {
        if (node == null || !node.isObject) return emptyMap()
        val out = LinkedHashMap<String, AsyncAPIOperation>()
        node.fields().forEach { (key, op) ->
            val actionRaw = op.get("action")?.asText()
                ?: throw SutProblemException("AsyncAPI operation '$key' missing 'action'")
            val action = when (actionRaw.lowercase()) {
                "send" -> AsyncAPIOperation.Action.SEND
                "receive" -> AsyncAPIOperation.Action.RECEIVE
                else -> throw SutProblemException(
                    "Unsupported AsyncAPI operation action '$actionRaw' on '$key' " +
                            "(expected 'send' or 'receive')"
                )
            }
            val channelRef = op.get("channel")
                ?: throw SutProblemException("AsyncAPI operation '$key' missing 'channel'")
            val channelName = resolveLocalRef(channelRef, LOCAL_CHANNEL_REF_PREFIX)
                ?: throw SutProblemException("AsyncAPI operation '$key' has unresolved channel reference")

            val messageIds = collectOperationMessageIds(op.get("messages"))

            val replyNode = op.get("reply")
            val reply = if (replyNode != null) {
                val replyChannelRef = replyNode.get("channel")
                val replyChannel = replyChannelRef?.let { resolveLocalRef(it, LOCAL_CHANNEL_REF_PREFIX) }
                    ?: throw SutProblemException("AsyncAPI operation '$key' has reply with no/unresolved channel")
                ReplyBinding(
                    channelNames = listOf(replyChannel),
                    messageIds = collectOperationMessageIds(replyNode.get("messages"))
                )
            } else null

            // AsyncAPI 3.0 `security` on an Operation is an array of Security
            // Requirement Objects. Each entry references a scheme defined under
            // components.securitySchemes (typically by $ref). We capture only
            // the referenced scheme names; the AND/OR semantics within an
            // entry are not modelled here (the broker layer applies one auth
            // configuration anyway).
            val security = parseOperationSecurity(op.get("security"))

            out[key] = AsyncAPIOperation(
                name = key,
                action = action,
                channelName = channelName,
                messageIds = messageIds,
                reply = reply,
                security = security
            )
        }
        return out
    }

    private fun parseOperationSecurity(node: JsonNode?): List<String> {
        if (node == null || !node.isArray) return emptyList()
        val out = mutableListOf<String>()
        node.forEach { entry ->
            val ref = entry?.get("\$ref")?.asText() ?: return@forEach
            // Expected ref shape: #/components/securitySchemes/<name>
            val name = ref.substringAfterLast('/')
            if (name.isNotBlank()) out.add(name)
        }
        return out
    }

    private fun collectOperationMessageIds(messagesNode: JsonNode?): List<String> {
        if (messagesNode == null || !messagesNode.isArray) return emptyList()
        val out = mutableListOf<String>()
        messagesNode.forEach { entry ->
            // Operation-level messages reference channel-level message keys, e.g.
            //   #/channels/<channelKey>/messages/<messageKey>
            // We resolve the trailing path segment and, where possible, map it to
            // the component-level message id by walking back through the parser.
            val ref = entry.get("\$ref")?.asText() ?: return@forEach
            val tail = ref.substringAfterLast('/')
            if (tail.isNotEmpty()) {
                out.add(tail)
            }
        }
        return out
    }

    private fun resolveLocalRef(node: JsonNode?, expectedPrefix: String): String? {
        if (node == null) return null
        val ref = node.get("\$ref")?.asText() ?: return null
        if (!ref.startsWith(expectedPrefix)) return null
        return ref.removePrefix(expectedPrefix)
    }

    private fun readFromRemoteServer(url: String): String {
        val response = connectToServer(url, attempts = 10)
        val body = response.readEntity(String::class.java)
        if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {
            throw SutProblemException(
                "Cannot retrieve AsyncAPI schema from $url , status=${response.status} , body: $body"
            )
        }
        return body
    }

    private fun readFromDisk(asyncApiLocation: String): String {
        val path = try {
            if (asyncApiLocation.startsWith("file:", true)) {
                Paths.get(URI.create(asyncApiLocation))
            } else {
                Paths.get(asyncApiLocation)
            }
        } catch (e: Exception) {
            throw SutProblemException(
                "The path provided for the AsyncAPI schema $asyncApiLocation is invalid: ${e.message}"
            )
        }
        if (!Files.exists(path)) {
            throw SutProblemException("The provided AsyncAPI file does not exist: $asyncApiLocation")
        }
        return path.toFile().readText()
    }

    /**
     * Resolve every external `$ref` reachable from [root]: load the referenced
     * source document, namespace its `components.schemas` and `components.messages`
     * under synthetic keys in [root]'s components, rewrite every external ref
     * (including chained refs in the loaded sources) to those synthetic keys.
     *
     * Mutates [root] in place. After this method returns, every `$ref` in the
     * document is intra-document (`#/...`).
     */
    private fun inlineExternalRefs(root: ObjectNode, primaryLocation: SchemaLocation) {

        // absoluteLocation → parsed document root
        val externalDocs = LinkedHashMap<String, ObjectNode>()
        // absoluteLocation → synthetic-key prefix (e.g. "_ext_a1b2c3d4_")
        val keyPrefixByDoc = LinkedHashMap<String, String>()

        // 1. Discover and load every external document, transitively. Mutates
        //    `externalDocs` and `keyPrefixByDoc`; does NOT yet rewrite anything.
        loadExternalDocsTransitively(root, primaryLocation, externalDocs, keyPrefixByDoc)

        if (externalDocs.isEmpty()) return

        // 2. Inline each external document's component entries under namespaced
        //    keys in the primary document, with their own internal $refs already
        //    rewritten to those namespaced keys.
        val primaryComponents = ensureObject(root, "components")
        val primarySchemas = ensureObject(primaryComponents, "schemas")
        val primaryMessages = ensureObject(primaryComponents, "messages")

        externalDocs.forEach { (loc, doc) ->
            val prefix = keyPrefixByDoc[loc]
                ?: throw IllegalStateException("Missing key prefix for $loc")
            inlineDocComponents(
                doc, prefix, primarySchemas, primaryMessages,
                loc, externalDocs, keyPrefixByDoc, primaryLocation
            )
        }

        // 3. Rewrite every external $ref in the primary document so it points
        //    at a namespaced key. (External refs *within* external docs were
        //    handled in step 2 while inlining their copies.)
        rewriteExternalRefsInPrimary(root, primaryLocation, keyPrefixByDoc)
    }

    private fun loadExternalDocsTransitively(
        root: ObjectNode,
        primaryLocation: SchemaLocation,
        externalDocs: MutableMap<String, ObjectNode>,
        keyPrefixByDoc: MutableMap<String, String>
    ) {
        // Worklist of (refValue, sourceLocation) pairs. The sourceLocation is
        // the doc the ref was found in (primary or some already-loaded external).
        data class Pending(val refValue: String, val source: SchemaLocation)
        val queue = ArrayDeque<Pending>()

        collectRefs(root).forEach { queue.add(Pending(it, primaryLocation)) }

        while (queue.isNotEmpty()) {
            val (refValue, source) = queue.removeFirst()
            if (SchemaRefUtils.isLocalRef(refValue)) continue

            val (rawLoc, _) = SchemaRefUtils.splitRef(refValue)
            if (rawLoc.isBlank()) continue

            val absoluteLoc = SchemaRefUtils.resolveRawLocation(rawLoc, source)
            // Back-reference to the primary doc (e.g. external `a.yaml` has
            // `$ref: './primary.yaml#/components/schemas/X'`). The target
            // already lives in the primary's components — no need to re-load
            // and inline a namespaced duplicate of the primary's own content.
            // The rewrite phase below detects the same condition and turns
            // such refs into local `#/components/...` strings.
            if (absoluteLoc == primaryLocation.location) continue
            if (absoluteLoc in externalDocs) continue

            val docText = try {
                fetchExternalDocText(absoluteLoc)
            } catch (e: Exception) {
                throw SutProblemException(
                    "Failed to load external AsyncAPI \$ref source '$absoluteLoc' " +
                            "(referenced as '$refValue' from '${source.location}'): ${e.message}"
                )
            }
            val docRoot = readTree(docText)
            if (docRoot !is ObjectNode) {
                throw SutProblemException(
                    "External AsyncAPI \$ref source '$absoluteLoc' did not parse as an object"
                )
            }
            externalDocs[absoluteLoc] = docRoot
            keyPrefixByDoc[absoluteLoc] = AsyncAPIRefLocation.externalKeyPrefix(absoluteLoc)

            val docLocation = SchemaLocation(absoluteLoc, locationTypeFor(absoluteLoc))
            collectRefs(docRoot).forEach { queue.add(Pending(it, docLocation)) }
        }
    }

    private fun inlineDocComponents(
        doc: ObjectNode,
        prefix: String,
        primarySchemas: ObjectNode,
        primaryMessages: ObjectNode,
        sourceLoc: String,
        externalDocs: Map<String, ObjectNode>,
        keyPrefixByDoc: Map<String, String>,
        primaryLocation: SchemaLocation
    ) {
        val components = doc.get("components") as? ObjectNode ?: return
        val sourceLocation = SchemaLocation(sourceLoc, locationTypeFor(sourceLoc))

        (components.get("schemas") as? ObjectNode)?.let { schemas ->
            schemas.fields().forEach { (name, schemaNode) ->
                val copy = schemaNode.deepCopy<JsonNode>()
                rewriteRefsInInlinedNode(copy, sourceLocation, prefix, externalDocs, keyPrefixByDoc, primaryLocation)
                primarySchemas.set<JsonNode>("$prefix$name", copy)
            }
        }
        (components.get("messages") as? ObjectNode)?.let { messages ->
            messages.fields().forEach { (name, msgNode) ->
                val copy = msgNode.deepCopy<JsonNode>()
                rewriteRefsInInlinedNode(copy, sourceLocation, prefix, externalDocs, keyPrefixByDoc, primaryLocation)
                primaryMessages.set<JsonNode>("$prefix$name", copy)
            }
        }
    }

    /**
     * Walk [node] (already copied into the primary document) and rewrite every
     * `$ref` it contains:
     *  - Intra-document refs (`#/components/schemas/X`) within the source doc:
     *    rewrite to `#/components/schemas/<sourcePrefix>X`.
     *  - External refs to other docs we've loaded: rewrite to that doc's prefix.
     */
    private fun rewriteRefsInInlinedNode(
        node: JsonNode,
        sourceLocation: SchemaLocation,
        ownPrefix: String,
        externalDocs: Map<String, ObjectNode>,
        keyPrefixByDoc: Map<String, String>,
        primaryLocation: SchemaLocation
    ) {
        if (node is ObjectNode) {
            val refText = node.get("\$ref")?.asText()
            if (refText != null) {
                val rewritten = rewriteRefValue(
                    refText, sourceLocation, ownPrefix, externalDocs, keyPrefixByDoc, primaryLocation
                )
                if (rewritten != null) {
                    node.put("\$ref", rewritten)
                }
            }
            node.fields().forEach { (_, child) ->
                rewriteRefsInInlinedNode(child, sourceLocation, ownPrefix, externalDocs, keyPrefixByDoc, primaryLocation)
            }
        } else if (node is ArrayNode) {
            node.forEach { rewriteRefsInInlinedNode(it, sourceLocation, ownPrefix, externalDocs, keyPrefixByDoc, primaryLocation) }
        }
    }

    private fun rewriteExternalRefsInPrimary(
        root: ObjectNode,
        primaryLocation: SchemaLocation,
        keyPrefixByDoc: Map<String, String>
    ) {
        rewriteRefsInInlinedNode(
            node = root,
            sourceLocation = primaryLocation,
            ownPrefix = "", // primary's intra-doc refs need no prefixing
            externalDocs = emptyMap(), // unused on this path
            keyPrefixByDoc = keyPrefixByDoc,
            primaryLocation = primaryLocation
        )
    }

    /**
     * Returns the rewritten `$ref` value, or null if the ref should be left
     * alone (intra-document ref in the *primary* doc).
     */
    private fun rewriteRefValue(
        refText: String,
        sourceLocation: SchemaLocation,
        ownPrefix: String,
        @Suppress("UNUSED_PARAMETER") externalDocs: Map<String, ObjectNode>,
        keyPrefixByDoc: Map<String, String>,
        primaryLocation: SchemaLocation
    ): String? {
        if (SchemaRefUtils.isLocalRef(refText)) {
            // Intra-document ref. Inside the *primary* doc (ownPrefix empty),
            // leave it alone. Inside an inlined external-doc copy, namespace it.
            if (ownPrefix.isEmpty()) return null
            return rewriteLocalFragment(refText, ownPrefix)
        }

        // External ref: split, resolve.
        val (rawLoc, fragment) = SchemaRefUtils.splitRef(refText)
        if (rawLoc.isBlank()) {
            return rewriteLocalFragment("#$fragment", ownPrefix)
        }
        val absoluteLoc = SchemaRefUtils.resolveRawLocation(rawLoc, sourceLocation)
        // Back-reference to the primary doc: the target lives in the primary's
        // own components, so rewrite to a plain local ref (no prefix). This
        // pairs with the matching skip in loadExternalDocsTransitively so the
        // primary is never inlined as a namespaced duplicate of itself.
        if (absoluteLoc == primaryLocation.location) {
            return externalRefToLocalKey(fragment, "")
        }
        val prefix = keyPrefixByDoc[absoluteLoc]
            ?: throw SutProblemException(
                "External AsyncAPI \$ref '$refText' from '${sourceLocation.location}' " +
                        "resolved to '$absoluteLoc' which was never loaded"
            )
        // Determine target component category from the fragment and rewrite.
        return externalRefToLocalKey(fragment, prefix)
    }

    private fun rewriteLocalFragment(refText: String, prefix: String): String {
        // refText is "#/<...>". Apply prefix to the last path segment for the
        // recognised component categories; leave others alone.
        val fragment = refText.removePrefix("#")
        return externalRefToLocalKey(fragment, prefix) ?: refText
    }

    private fun externalRefToLocalKey(fragment: String, prefix: String): String? {
        val trimmed = fragment.trimStart('/')
        return when {
            trimmed.startsWith("components/schemas/") -> {
                val name = trimmed.removePrefix("components/schemas/")
                "#/components/schemas/$prefix$name"
            }
            trimmed.startsWith("components/messages/") -> {
                val name = trimmed.removePrefix("components/messages/")
                "#/components/messages/$prefix$name"
            }
            trimmed.isEmpty() -> {
                // Whole-file external ref. Uncommon and ambiguous (the target
                // doc may have many components); not supported in the starter.
                throw SutProblemException(
                    "AsyncAPI whole-file external \$ref (no fragment) is not supported " +
                            "yet; rewrite the ref to point at a specific component " +
                            "(e.g. '<file>#/components/schemas/<Name>')"
                )
            }
            trimmed.startsWith("channels/") || trimmed.startsWith("operations/") -> {
                // The parser inlines only schemas and messages today; cross-doc
                // channel / operation references would require lifting the
                // structural sections too — out of scope. Fail loudly so the
                // user knows what to do.
                throw SutProblemException(
                    "External AsyncAPI \$ref pointing at '/$trimmed' is not supported yet " +
                            "(only #/components/schemas/* and #/components/messages/* may " +
                            "be referenced across files). Inline the target into the primary " +
                            "document, or reference it via a component schema."
                )
            }
            else -> {
                // Unknown fragment category — pass through (most likely the
                // downstream local-ref resolver will surface a clearer error
                // than we can here).
                null
            }
        }
    }

    /** Recursively collect every `$ref` string value reachable from [node]. */
    private fun collectRefs(node: JsonNode): List<String> {
        val out = mutableListOf<String>()
        collectRefsInto(node, out)
        return out
    }

    private fun collectRefsInto(node: JsonNode, out: MutableList<String>) {
        when (node) {
            is ObjectNode -> {
                node.get("\$ref")?.asText()?.let { out.add(it) }
                node.fields().forEach { (_, child) -> collectRefsInto(child, out) }
            }
            is ArrayNode -> node.forEach { collectRefsInto(it, out) }
            else -> {}
        }
    }

    private fun ensureObject(parent: ObjectNode, fieldName: String): ObjectNode {
        val existing = parent.get(fieldName)
        if (existing is ObjectNode) return existing
        val created = parent.objectNode()
        parent.set<JsonNode>(fieldName, created)
        return created
    }

    private fun locationTypeFor(absoluteLocation: String): SchemaLocationType {
        return if (absoluteLocation.startsWith("http:", ignoreCase = true) ||
            absoluteLocation.startsWith("https:", ignoreCase = true)
        ) {
            SchemaLocationType.REMOTE
        } else {
            SchemaLocationType.LOCAL
        }
    }

    private fun fetchExternalDocText(absoluteLocation: String): String {
        return if (absoluteLocation.startsWith("http:", ignoreCase = true) ||
            absoluteLocation.startsWith("https:", ignoreCase = true)
        ) {
            readFromRemoteServer(absoluteLocation)
        } else {
            readFromDisk(absoluteLocation)
        }
    }

    private fun connectToServer(url: String, attempts: Int): Response {
        for (i in 0 until attempts) {
            try {
                return ClientBuilder.newClient()
                    .target(url)
                    .request("*/*")
                    .get()
            } catch (e: Exception) {
                if (e.cause is ConnectException) {
                    Thread.sleep(1_000)
                } else {
                    throw SutProblemException("Failed to connect to $url: ${e.message}")
                }
            }
        }
        throw SutProblemException("Check the schema URL. Failed to connect to $url")
    }
}
