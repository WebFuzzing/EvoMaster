package com.foo.rest.examples.bb.httppatch

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

/**
 * Black-box SUT for the JSON Patch (RFC 6902) e2e test.
 *
 * This is a vendored, self-contained slice of the open-source project
 * https://github.com/cassiomolin/http-patch-spring (the same API whose OpenAPI schema is
 * already committed in core/src/test/resources/swagger/sut/http-patch-spring.json).
 *
 * Differences from the original, on purpose:
 *  - in-memory store instead of a real database, so state can be reset between runs;
 *  - JSON Patch applied manually with Jackson (no extra dependency);
 *  - the patch is applied TRANSACTIONALLY: it is computed on a copy, the result is validated,
 *    and only persisted if the resulting Contact is still valid. Any malformed/inapplicable
 *    patch, or a patch that would leave the resource invalid, is rejected with a 4xx and the
 *    stored object is left untouched. This is what guarantees the API "does not break" while
 *    EvoMaster fuzzes it with potentially destructive patches.
 */
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping("/contacts")
open class HttpPatchApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpPatchApplication::class.java, *args)
        }

        private val store = LinkedHashMap<Long, Contact>()
        private var counter = 0L

        private fun initialContacts(): List<Contact> = listOf(
            Contact(
                name = "Ada Lovelace",
                birthday = "1815-12-10",
                favorite = true,
                notes = "mathematician",
                groups = mutableListOf("friends", "science"),
                work = Work("Analyst", "Analytical Engine Co"),
                phones = mutableListOf(Phone("555-0001", "home")),
                emails = mutableListOf(Email("ada@example.com", "work"))
            ),
            // Note: 'work' is intentionally null here, so patches targeting /work/title
            // exercise the "navigate into a missing parent" path (handled as 409, not 500).
            Contact(
                name = "Alan Turing",
                birthday = "1912-06-23",
                favorite = false,
                notes = null,
                groups = mutableListOf("science"),
                work = null,
                phones = mutableListOf(),
                emails = mutableListOf()
            )
        )

        /**
         * Re-seeds the in-memory store to its initial state. Invoked by the EmbeddedSutController
         * (HttpPatchController.resetStateOfSUT) so that destructive patches from one test do not
         * leak into the next one.
         */
        @JvmStatic
        fun reset() {
            synchronized(store) {
                store.clear()
                counter = 0
                for (c in initialContacts()) {
                    val id = ++counter
                    store[id] = c.copy(id = id)
                }
            }
        }
    }

    init {
        reset()
    }

    private val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    /**
     * All non-2xx responses use a small JSON body. Returning a plain string under
     * Content-Type application/json would be served as invalid JSON, which (a) is itself a
     * response/schema-mismatch fault and (b) makes the generated black-box tests crash on
     * JSON.parse (e.g. JS/Jest with superagent). A JSON object keeps every response valid JSON.
     */
    private fun problem(status: Int, message: String): ResponseEntity<Any> =
        ResponseEntity.status(status).body(mapOf("message" to message))

    @GetMapping
    fun findContacts(): ResponseEntity<List<Contact>> =
        ResponseEntity.ok(synchronized(store) { store.values.toList() })

    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    fun createContact(@RequestBody contact: Contact): ResponseEntity<Any> {
        if (contact.name.isNullOrBlank())
            return problem(422, "name is required")
        val created = synchronized(store) {
            val id = ++counter
            contact.copy(id = id).also { store[id] = it }
        }
        // 201 + Location header so EvoMaster can bind subsequent calls (incl. its cleanup DELETE)
        // to the created resource. Without this, cleanup hits a non-existent id and EvoMaster's
        // BlackBoxRestFitness.handleCleanUpActions fails with "Wrong status: 404".
        return ResponseEntity.created(URI.create("/contacts/${created.id}")).body(created)
    }

    @GetMapping("/{id}", produces = ["application/json"])
    fun findContact(@PathVariable id: Long): ResponseEntity<Contact> {
        val contact = synchronized(store) { store[id] } ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(contact)
    }

    @PutMapping("/{id}", consumes = ["application/json"])
    fun updateContact(@PathVariable id: Long, @RequestBody contact: Contact): ResponseEntity<Any> {
        synchronized(store) { store[id] } ?: return ResponseEntity.notFound().build()
        if (contact.name.isNullOrBlank())
            return problem(422, "name is required")
        synchronized(store) { store[id] = contact.copy(id = id) }
        return ResponseEntity.ok().build()
    }

    // Idempotent on purpose: returns 204 whether or not the id existed. EvoMaster's black-box
    // cleanup phase (BlackBoxRestFitness.handleCleanUpActions) issues a DELETE bound to each
    // created resource and asserts a 2xx/403 status. Returning 404 for a missing id there would
    // crash the whole search with "Wrong status: 404", so we keep DELETE idempotent (also valid
    // per RFC 7231: a 2xx with no body is acceptable for an already-absent resource).
    @DeleteMapping("/{id}")
    fun deleteContact(@PathVariable id: Long): ResponseEntity<Any> {
        synchronized(store) { store.remove(id) }
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}", consumes = ["application/json-patch+json"], produces = ["application/json"])
    fun patchContact(@PathVariable id: Long, @RequestBody body: String): ResponseEntity<Any> {
        val current = synchronized(store) { store[id] } ?: return ResponseEntity.notFound().build()

        val patchNode = try {
            mapper.readTree(body)
        } catch (e: Exception) {
            return problem(400, "Malformed JSON document")
        }

        // Apply on a tree copy of the resource. Any structural problem -> 4xx, never 500.
        val patched: JsonNode = try {
            ContactJsonPatch.apply(mapper.valueToTree(current), patchNode)
        } catch (e: PatchException) {
            if (e.status == 409) CoveredTargets.cover("JSONPATCH_CONFLICT")
            return problem(e.status, e.message ?: "Patch operation failed")
        } catch (e: Exception) {
            return problem(400, "Could not apply patch document")
        }

        // Validate the result before persisting it.
        val updated = try {
            mapper.treeToValue(patched, Contact::class.java)
        } catch (e: Exception) {
            CoveredTargets.cover("JSONPATCH_INVALID_RESOURCE")
            return problem(422, "Patched resource is not a valid Contact")
        }
        if (updated.name.isNullOrBlank()) {
            CoveredTargets.cover("JSONPATCH_INVALID_RESOURCE")
            return problem(422, "name is required")
        }

        synchronized(store) { store[id] = updated.copy(id = id) }
        CoveredTargets.cover("JSONPATCH_APPLIED_OK")
        return ResponseEntity.ok(updated.copy(id = id))
    }
}

/**
 * Minimal manual JSON Patch (RFC 6902) applier over a Jackson tree.
 * Mutates a deep copy of the resource in place. Throws [PatchException] (a 4xx) for any
 * malformed or inapplicable operation, so the controller never returns a 500 for bad input.
 */
object ContactJsonPatch {

    fun apply(resource: JsonNode, patch: JsonNode): JsonNode {
        if (patch !is ArrayNode) throw PatchException(400, "Patch document must be a JSON array")

        val root: JsonNode = resource.deepCopy()
        for (op in patch) {
            if (op !is ObjectNode) throw PatchException(400, "Each operation must be a JSON object")
            when (val opName = op.path("op").asText(null)
                ?: throw PatchException(400, "Operation is missing 'op'")) {
                "add" -> {
                    CoveredTargets.cover("JSONPATCH_OP_ADD")
                    add(root, pointer(op, "path"), requireValue(op))
                }
                "remove" -> {
                    CoveredTargets.cover("JSONPATCH_OP_REMOVE")
                    remove(root, pointer(op, "path"))
                }
                "replace" -> {
                    CoveredTargets.cover("JSONPATCH_OP_REPLACE")
                    replace(root, pointer(op, "path"), requireValue(op))
                }
                "move" -> {
                    CoveredTargets.cover("JSONPATCH_OP_MOVE")
                    val from = pointer(op, "from")
                    val value = read(root, from)
                    remove(root, from)
                    add(root, pointer(op, "path"), value)
                }
                "copy" -> {
                    CoveredTargets.cover("JSONPATCH_OP_COPY")
                    val value = read(root, pointer(op, "from")).deepCopy<JsonNode>()
                    add(root, pointer(op, "path"), value)
                }
                "test" -> {
                    CoveredTargets.cover("JSONPATCH_OP_TEST")
                    if (read(root, pointer(op, "path")) != requireValue(op))
                        throw PatchException(409, "Test operation failed")
                }
                else -> throw PatchException(400, "Unsupported operation: $opName")
            }
        }
        return root
    }

    private fun pointer(op: ObjectNode, field: String): String =
        op.path(field).asText(null) ?: throw PatchException(400, "Operation is missing '$field'")

    private fun requireValue(op: ObjectNode): JsonNode =
        if (op.has("value")) op.get("value") else throw PatchException(400, "Operation is missing 'value'")

    private fun tokens(p: String): List<String> {
        if (p.isEmpty()) return emptyList()
        if (!p.startsWith("/")) throw PatchException(400, "Invalid JSON Pointer: $p")
        return p.substring(1).split("/").map { it.replace("~1", "/").replace("~0", "~") }
    }

    private fun child(node: JsonNode, token: String): JsonNode? = when (node) {
        is ObjectNode -> if (node.has(token)) node.get(token) else null
        is ArrayNode -> token.toIntOrNull()?.let { if (it in 0 until node.size()) node.get(it) else null }
        else -> null
    }

    private fun parentAndKey(root: JsonNode, p: String): Pair<JsonNode, String> {
        val tk = tokens(p)
        if (tk.isEmpty()) throw PatchException(400, "Root path '' is not supported")
        var node = root
        for (i in 0 until tk.size - 1) {
            node = child(node, tk[i]) ?: throw PatchException(409, "Path not found: $p")
        }
        return node to tk.last()
    }

    private fun read(root: JsonNode, p: String): JsonNode {
        var node = root
        for (t in tokens(p)) node = child(node, t) ?: throw PatchException(409, "Path not found: $p")
        return node
    }

    private fun add(root: JsonNode, p: String, value: JsonNode) {
        val (parent, key) = parentAndKey(root, p)
        when (parent) {
            is ObjectNode -> parent.replace(key, value)
            is ArrayNode -> {
                if (key == "-") parent.add(value)
                else {
                    val idx = key.toIntOrNull() ?: throw PatchException(400, "Invalid array index: $key")
                    if (idx < 0 || idx > parent.size()) throw PatchException(409, "Array index out of bounds: $idx")
                    parent.insert(idx, value)
                }
            }
            else -> throw PatchException(409, "Cannot add into a non-container at $p")
        }
    }

    private fun remove(root: JsonNode, p: String) {
        val (parent, key) = parentAndKey(root, p)
        when (parent) {
            is ObjectNode -> if (parent.has(key)) parent.remove(key) else throw PatchException(409, "Path not found: $p")
            is ArrayNode -> {
                val idx = key.toIntOrNull() ?: throw PatchException(400, "Invalid array index: $key")
                if (idx < 0 || idx >= parent.size()) throw PatchException(409, "Array index out of bounds: $idx")
                parent.remove(idx)
            }
            else -> throw PatchException(409, "Cannot remove from a non-container at $p")
        }
    }

    private fun replace(root: JsonNode, p: String, value: JsonNode) {
        val (parent, key) = parentAndKey(root, p)
        when (parent) {
            is ObjectNode -> if (parent.has(key)) parent.replace(key, value) else throw PatchException(409, "Path not found: $p")
            is ArrayNode -> {
                val idx = key.toIntOrNull() ?: throw PatchException(400, "Invalid array index: $key")
                if (idx < 0 || idx >= parent.size()) throw PatchException(409, "Array index out of bounds: $idx")
                parent.set(idx, value)
            }
            else -> throw PatchException(409, "Cannot replace in a non-container at $p")
        }
    }
}

class PatchException(val status: Int, message: String) : RuntimeException(message)

data class Contact(
    var id: Long? = null,
    var name: String? = null,
    var birthday: String? = null,
    var favorite: Boolean? = null,
    var notes: String? = null,
    var groups: MutableList<String>? = null,
    var work: Work? = null,
    var phones: MutableList<Phone>? = null,
    var emails: MutableList<Email>? = null
)

data class Work(var title: String? = null, var company: String? = null)

data class Phone(var phone: String? = null, var type: String? = null)

data class Email(var email: String? = null, var type: String? = null)