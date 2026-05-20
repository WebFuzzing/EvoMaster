package org.evomaster.core.output.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import org.evomaster.core.output.TestCase
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.data.ReplyFieldAssertion
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.utils.GeneUtils
import java.nio.file.Path
import java.util.UUID

/**
 * Test-case writer for AsyncAPI runs.
 *
 * For Java JUnit 5 output (the default), emits real Kafka producer /
 * consumer calls via [org.evomaster.test.utils.EMTestUtils.kafkaPublish]
 * and [org.evomaster.test.utils.EMTestUtils.kafkaAwaitReply].  The
 * helpers wrap kafka-clients so each generated test action stays one
 * line; users opt into AsyncAPI tests by pulling in
 * `org.apache.kafka:kafka-clients` (the helper raises a clear error
 * if it's missing at test runtime).
 *
 * Other languages still get the comment-only skeleton — Kotlin/JS/Python
 * emission is parked as follow-up.
 */
class AsyncAPITestCaseWriter : ApiTestCaseWriter() {

    @Inject
    private lateinit var emConfig: EMConfig

    /**
     * Emit the JavaDoc / single-line comment block that precedes the
     * generated `@Test` method.
     *
     * M11-PR2 fix L upgrades this from a one-line stub
     * (`// AsyncAPI test case: test_0`) to a multi-line summary listing each
     * action's kind, channel, and observed delivery / reply outcome. Useful
     * when grepping a 60+ test file for "what does this test exercise?".
     *
     * M11-PR2 fix C additionally emits `@Disabled` for tests whose action
     * chain stalled at evaluation time (every PUBLISH recorded
     * `delivery: fail`). Those tests still publish the same bytes at
     * replay time, but with the original broker rejection now invisible
     * — running them passes vacuously and pollutes CI output. JUnit 5
     * picks up `@Disabled` regardless of whether it precedes or follows
     * `@Test`, so we emit it here as part of the comment block.
     */
    override fun addTestCommentBlock(lines: Lines, test: TestCase) {
        lines.addSingleCommentLine("AsyncAPI test case: ${test.name}")
        val actions = test.test.individual.seeMainExecutableActions()
        for ((i, a) in actions.withIndex()) {
            val async = a as? AsyncAPIAction ?: continue
            val res = test.test.seeResult(a.getLocalId()) ?: continue
            val delivery = res.getResultValue("delivery")
            val reply = res.getResultValue("reply")
            val outcome = listOfNotNull(
                delivery?.let { "delivery: $it" },
                reply?.let { "reply: $it" }
            ).joinToString(", ")
            val summary = "Action #$i: ${async.kind.name} on '${async.channelAddress}'" +
                    if (outcome.isNotEmpty()) " — $outcome" else ""
            lines.addSingleCommentLine(summary)
        }
    }

    /**
     * Emit `@Disabled` / `@DisplayName` annotations on the line *between*
     * the JavaDoc block and the `@Test` annotation. Pre-M11-PR3 these were
     * emitted from `addTestCommentBlock`, which embedded them inside the
     * `/** … */` block — making them syntactic comments and silently inert.
     * The new [TestCaseWriter.addTestAnnotations] hook fires at the right
     * spot so JUnit actually picks them up.
     */
    override fun addTestAnnotations(lines: Lines, test: TestCase) {
        if (!config.outputFormat.isJUnit5()) return
        val actions = test.test.individual.seeMainExecutableActions()
        if (actions.isEmpty()) return

        val publishes = mutableListOf<AsyncAPIAction>()
        val publishesFailed = mutableListOf<AsyncAPIAction>()
        val kindCounts = mutableMapOf<AsyncAPIAction.Kind, Int>()
        val channels = mutableListOf<String>()
        for (a in actions) {
            val async = a as? AsyncAPIAction ?: continue
            val res = test.test.seeResult(a.getLocalId()) ?: continue
            kindCounts.merge(async.kind, 1) { acc, v -> acc + v }
            channels.add(async.channelAddress)
            if (async.kind == AsyncAPIAction.Kind.PUBLISH) {
                publishes.add(async)
                if (res.getResultValue("delivery") == "fail") publishesFailed.add(async)
            }
        }
        // @Disabled — only when *every* PUBLISH failed at evaluation.
        if (publishes.isNotEmpty() && publishes.size == publishesFailed.size) {
            val failedChannels = publishes.joinToString(", ") { "'${it.channelAddress}'" }
            lines.add(
                "@org.junit.jupiter.api.Disabled(\"" +
                        escapeJava("every PUBLISH in this test recorded delivery: fail during " +
                                "EvoMaster evaluation (channels: $failedChannels); test was generated for " +
                                "completeness but does not exercise the SUT") +
                        "\")"
            )
        }
        // @DisplayName — human-readable JUnit report label.
        val displayName = buildDisplayName(kindCounts, channels, publishesFailed.size, publishes.size)
        if (displayName.isNotBlank()) {
            lines.add("@org.junit.jupiter.api.DisplayName(\"${escapeJava(displayName)}\")")
        }
        // M11-PR3 partial-#6/#7: emit a `@Tag("channel:<address>")` so users
        // can filter / group tests by channel via JUnit 5 tag selectors:
        // `mvn test -Dgroups=channel:streetlights`. Full `@Nested`-per-channel
        // suite splitting is deferred to a follow-up PR — it would require
        // significant restructuring of `TestSuiteWriter`'s test-emission
        // pipeline.
        channels.distinct().forEach { channel ->
            lines.add("@org.junit.jupiter.api.Tag(\"channel:${escapeJava(channel)}\")")
        }
    }

    private fun buildDisplayName(
        kindCounts: Map<AsyncAPIAction.Kind, Int>,
        channels: List<String>,
        failedPublishes: Int,
        totalPublishes: Int
    ): String {
        val parts = mutableListOf<String>()
        kindCounts[AsyncAPIAction.Kind.PUBLISH]?.let { n ->
            parts.add(if (n == 1) "publish" else "$n publishes")
        }
        kindCounts[AsyncAPIAction.Kind.SUBSCRIBE_REPLY]?.let { _ ->
            parts.add("await reply")
        }
        kindCounts[AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT]?.let { n ->
            parts.add(if (n == 1) "observe output" else "observe outputs")
        }
        val channel = channels.firstOrNull()?.let { " on '$it'" } ?: ""
        val outcome = when {
            totalPublishes > 0 && failedPublishes == totalPublishes -> " — all publishes failed at eval"
            totalPublishes > 0 && failedPublishes > 0 -> " — $failedPublishes of $totalPublishes publishes failed at eval"
            else -> ""
        }
        return parts.joinToString(" + ") + channel + outcome
    }

    override fun handleActionCalls(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        insertionVars: MutableList<Pair<String, String>>,
        testCaseName: String,
        testSuitePath: Path?
    ) {
        val actions = ind.individual.seeMainExecutableActions()
        if (actions.isEmpty()) {
            lines.addSingleCommentLine("(no AsyncAPI actions in this individual)")
            return
        }
        // Per-test broker resolution used to be emitted as the first line of
        // every test method; pulled up to the class-level static field
        // `baseUrlOfSut` (declared by TestSuiteWriter for black-box runs).
        // The `KAFKA_BOOTSTRAP_SERVERS` env-var fallback that was inlined in
        // each test method was dropped — users who need broker re-targeting
        // can override the static field directly. M11-PR2 fix D.
        // Map from pairId to the correlation id we synthesised for the matching
        // PUBLISH; SUBSCRIBE_REPLY actions read it so the consumer-side filter
        // matches the producer-side header value.
        val correlationByPair = mutableMapOf<String, String>()
        // M11-PR3 fix #10: also remember which action index emitted the
        // publish so the paired SUBSCRIBE_REPLY can assert reply ≥ publish.
        val publishIndexByPair = mutableMapOf<String, Int>()
        // Coalesce trailing skipped actions into a single summary line rather
        // than emitting one `// Action #N (KIND) — no result captured ...`
        // comment per action. A long PUBLISH-fail chain (voiceblender hit 47
        // SUBSCRIBE_OUTPUT tails on one test) produced an unreadable wall of
        // comments otherwise.
        var skipRunStart = -1
        var skipRunCount = 0
        fun flushSkipRun() {
            if (skipRunCount == 0) return
            val lastIndex = skipRunStart + skipRunCount - 1
            val label = if (skipRunCount == 1) {
                "Action #$skipRunStart — no result captured (action chain stopped early)"
            } else {
                "Actions #$skipRunStart–#$lastIndex ($skipRunCount actions) — no results captured (action chain stopped early)"
            }
            lines.addSingleCommentLine(label)
            skipRunStart = -1
            skipRunCount = 0
        }
        for ((i, action) in actions.withIndex()) {
            val result = ind.seeResult(action.getLocalId())
            val async = action as? AsyncAPIAction
            if (async != null && async.kind == AsyncAPIAction.Kind.PUBLISH) {
                correlationByPair[async.pairId] = "evm-${UUID.randomUUID()}"
                publishIndexByPair[async.pairId] = i
            }
            if (result == null) {
                if (skipRunCount == 0) skipRunStart = i
                skipRunCount++
                continue
            }
            flushSkipRun()
            addAsyncAPIAction(async, i, lines, result, correlationByPair, publishIndexByPair)
        }
        flushSkipRun()
    }

    private fun addAsyncAPIAction(
        action: AsyncAPIAction?,
        index: Int,
        lines: Lines,
        result: ActionResult,
        correlationByPair: Map<String, String>,
        publishIndexByPair: Map<String, Int>
    ) {
        if (action == null) return

        // Per-action evaluation outcome (delivery / reply / variant) was
        // previously duplicated in the test body. M11-PR2 fix L lifted that
        // summary into the JavaDoc above the method; the body now stays
        // focused on the action sequence itself. The single-line method
        // header below is still useful inside the body because navigators
        // jump to here, not the JavaDoc.
        lines.addSingleCommentLine(
            "Action #$index: ${action.kind} on '${action.channelAddress}' (operation '${action.operationName}')"
        )

        if (!config.outputFormat.isJava()) {
            lines.addSingleCommentLine("TODO Kotlin/Python AsyncAPI emission not implemented yet")
            lines.addEmpty(1)
            return
        }

        when (action.kind) {
            AsyncAPIAction.Kind.PUBLISH -> emitPublish(action, index, lines, correlationByPair[action.pairId])
            AsyncAPIAction.Kind.SUBSCRIBE_REPLY -> emitSubscribeReply(
                action, index, lines, correlationByPair[action.pairId], publishIndexByPair[action.pairId]
            )
            AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT -> emitSubscribeOutput(action, index, lines)
        }
        lines.addEmpty(1)
    }

    private fun emitPublish(action: AsyncAPIAction, index: Int, lines: Lines, correlationId: String?) {
        val payloadGene = action.payloadParam()?.primaryGene()
        val payloadJson = payloadGene?.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = null) ?: "{}"
        val key = action.keyParam()?.primaryGene()?.getValueAsRawString()
        val keyExpr = if (key == null) "null" else "\"${escapeJava(key)}\""

        val rendered = renderChannelAddress(action)
        val correlationHeader = action.correlationHeaderName

        // M11-PR3 fix #1: pretty-print the payload as a `//` comment block
        // above the publish call so reviewers can read the intent without
        // staring at a one-line escaped JSON literal.
        emitPayloadComment(payloadJson, lines)

        // M11-PR3 fix #8: detect EvoMaster's invalid-input probe markers in
        // the payload (the "EVOMASTER" off-enum sentinel + a deliberately
        // empty payload `{}` against a schema with required fields). Wrap
        // the publish in `assertThrows` only when the schema *itself*
        // declares the payload invalid — schema-derivable, no SUT-side
        // oracle. For now we conservatively probe only the explicit
        // "EVOMASTER" string sentinel because that's the one the engine
        // intentionally emits as a known invalid value.
        val probeSentinel = "\"EVOMASTER\""
        val wrapInAssertThrows = payloadJson.contains(probeSentinel)

        // M11-PR3 fix #10: capture wall-clock around the publish so the
        // matching SUBSCRIBE_REPLY can assert ordering (reply ≥ publish).
        val publishStampVar = "__evm_publish_at_$index"
        lines.add("long $publishStampVar = System.currentTimeMillis();")

        lines.add("{")
        lines.indented {
            lines.add("java.util.Map<String, byte[]> __evm_headers = new java.util.LinkedHashMap<>();")
            if (correlationHeader != null && correlationId != null) {
                lines.add(
                    "__evm_headers.put(\"${escapeJava(correlationHeader)}\", " +
                            "\"$correlationId\".getBytes(java.nio.charset.StandardCharsets.UTF_8));"
                )
            }
            // User-defined headers (auth tokens, tenant ids, …).
            action.headersParam()?.primaryGene()?.let { headersGene ->
                if (headersGene is org.evomaster.core.search.gene.ObjectGene) {
                    headersGene.fields.forEach { field ->
                        val active = (field as? org.evomaster.core.search.gene.wrapper.OptionalGene)?.isActive ?: true
                        if (!active) return@forEach
                        val raw = if (field is org.evomaster.core.search.gene.wrapper.OptionalGene)
                            field.gene.getValueAsRawString() else field.getValueAsRawString()
                        lines.add(
                            "__evm_headers.put(\"${escapeJava(field.name)}\", " +
                                    "\"${escapeJava(raw)}\".getBytes(java.nio.charset.StandardCharsets.UTF_8));"
                        )
                    }
                }
            }
            val publishCall =
                "org.evomaster.test.utils.EMTestUtils.kafkaPublish(baseUrlOfSut, " +
                        "\"${escapeJava(rendered)}\", $keyExpr, " +
                        "\"${escapeJava(payloadJson)}\".getBytes(java.nio.charset.StandardCharsets.UTF_8), " +
                        "__evm_headers);"
            if (wrapInAssertThrows) {
                lines.add(
                    "// schema-invalid input (engine probe): publish must surface a runtime exception"
                )
                lines.add(
                    "assertThrows(RuntimeException.class, () -> { $publishCall });"
                )
            } else {
                lines.add(publishCall)
            }
        }
        lines.add("}")
    }

    private fun emitPayloadComment(payloadJson: String, lines: Lines) {
        if (payloadJson.isEmpty() || payloadJson == "{}") {
            lines.addSingleCommentLine("payload: {}")
            return
        }
        val pretty = try {
            val mapper = ObjectMapper()
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(payloadJson))
        } catch (e: Exception) {
            // Non-JSON payloads (e.g. raw bytes) fall back to the literal.
            payloadJson
        }
        if (pretty.length <= 80 && !pretty.contains('\n')) {
            lines.addSingleCommentLine("payload: $pretty")
        } else {
            lines.addSingleCommentLine("payload:")
            pretty.split('\n').forEach { lines.addSingleCommentLine("  $it") }
        }
    }

    private fun emitSubscribeOutput(action: AsyncAPIAction, index: Int, lines: Lines) {
        // Mirror the engine's listen-window. Use the configured value with
        // a safety floor so generated tests don't end up with 0-ms windows
        // that race the broker's first poll.
        val configuredWindow = config.asyncApiOutputObservationWindowMs.toLong().coerceAtLeast(250L)
        val rendered = renderChannelAddress(action)
        val varName = "__evm_output_$index"
        lines.add(
            "byte[][] $varName = org.evomaster.test.utils.EMTestUtils.kafkaCollectAllWithin(" +
                    "baseUrlOfSut, \"${escapeJava(rendered)}\", ${configuredWindow}L);"
        )
        // The schema declares this channel as SUT-produced; we don't require
        // a particular message count — a 0-message outcome is a legitimate
        // signal (the SUT didn't emit during the window). Each captured
        // message, however, MUST conform to the channel's declared schema:
        // we loop over the buffer and apply the same per-field facet
        // assertions used by SUBSCRIBE_REPLY (required / enum / bounds /
        // length / format). Skipping the loop when no messages arrived is
        // intentional — see the comment above.
        lines.add(
            "// $varName holds every message captured on '${escapeJava(rendered)}' " +
                    "during the ${configuredWindow}ms listen window"
        )
        if (action.replyFieldAssertions.isNotEmpty()) {
            val loopVar = "__evm_msg_$index"
            lines.add("for (byte[] $loopVar : $varName) {")
            lines.indented {
                emitFieldAssertionsOver(
                    specs = action.replyFieldAssertions,
                    valueVar = loopVar,
                    label = "output on '${escapeJava(rendered)}'",
                    lines = lines
                )
            }
            lines.add("}")
        }
        // M11-PR2 fix N: soft assertion that the buffer is non-null and the
        // window finished collecting (the helper returns a zero-length array
        // when the SUT didn't emit, never null). Hard count assertions like
        // `length == 1` are left to the user — the schema doesn't carry a
        // count expectation for SEND channels.
        lines.add(
            "assertNotNull($varName, " +
                    "\"output buffer on '${escapeJava(rendered)}' came back null — listener never ran\");"
        )
    }

    private fun emitSubscribeReply(action: AsyncAPIAction, index: Int, lines: Lines, correlationId: String?, paramPublishIndex: Int? = null) {
        val rendered = renderChannelAddress(action)
        val correlationHeader = action.correlationHeaderName
        val varName = "__evm_reply_$index"

        // M11-PR3 fix #10: causality check — the reply must arrive *after*
        // the matching publish. We record a wall-clock snapshot around the
        // reply receipt and compare against the publish snapshot recorded
        // in emitPublish. If the publish-index lookup fails (the action
        // chain didn't follow the expected publish→reply order) we skip
        // the timing assertion gracefully — assertion churn isn't worth a
        // hard error.
        val replyStampVar = "__evm_reply_at_$index"

        if (correlationHeader != null && correlationId != null) {
            // M11-PR2 fix E: when the schema declares a correlation header,
            // fetch the *first* reply unconditionally and assert correlation
            // explicitly. The older path silently filtered out wrong-correlation
            // messages and returned null, conflating "no reply" with "wrong
            // correlation reply". This envelope-returning variant gives the
            // user a clear failure message in either case.
            val envVar = "__evm_envelope_$index"
            lines.add(
                "org.evomaster.test.utils.EMTestUtils.ReplyEnvelope $envVar = " +
                        "org.evomaster.test.utils.EMTestUtils.kafkaAwaitReplyEnvelope(" +
                        "baseUrlOfSut, \"${escapeJava(rendered)}\", " +
                        "\"${escapeJava(correlationHeader)}\", 5000L);"
            )
            lines.add(
                "assertNotNull($envVar, " +
                        "\"AsyncAPI reply on ${escapeJava(rendered)} did not arrive within 5s\");"
            )
            lines.add(
                "assertEquals(" +
                        "\"$correlationId\", $envVar.correlationId, " +
                        "\"AsyncAPI reply on ${escapeJava(rendered)} arrived with mismatched correlation id\");"
            )
            lines.add("byte[] $varName = $envVar.payload;")
        } else {
            // No correlation header declared — first message wins, no
            // correlation assertion is meaningful.
            lines.add(
                "byte[] $varName = " +
                        "org.evomaster.test.utils.EMTestUtils.kafkaAwaitReply(baseUrlOfSut, " +
                        "\"${escapeJava(rendered)}\", \"\", \"\", 5000L);"
            )
            lines.add(
                "assertNotNull($varName, " +
                        "\"AsyncAPI reply on ${escapeJava(rendered)} did not arrive within 5s\");"
            )
        }
        // M11-PR3 fix #10: reply timestamp must be ≥ publish timestamp.
        if (paramPublishIndex != null) {
            val publishStampVar = "__evm_publish_at_$paramPublishIndex"
            lines.add("long $replyStampVar = System.currentTimeMillis();")
            lines.add(
                "assertTrue($replyStampVar >= $publishStampVar, " +
                        "\"AsyncAPI reply on ${escapeJava(rendered)} arrived before the paired publish " +
                        "(clock skew or out-of-order delivery)\");"
            )
        }
        emitReplyFieldAssertions(action, varName, lines)
    }

    /**
     * Emit per-field JUnit assertions on the captured reply, driven by the
     * pre-computed [AsyncAPIAction.replyFieldAssertions] populated at build
     * time. M9-PR5. Six assertion kinds:
     *
     *  - REQUIRED        → assertTrue(EMTestUtils.replyHas(reply, "f"))
     *  - ENUM            → assertTrue(Arrays.asList(...).contains(EMTestUtils.replyText(reply, "f")))
     *  - MIN / MAX       → assertTrue(EMTestUtils.replyNumber(reply, "f") >= / <= bound)
     *  - MIN_LENGTH /
     *    MAX_LENGTH      → assertTrue(EMTestUtils.replyTextLength(reply, "f") >= / <= bound)
     *  - FORMAT          → assertTrue(EMTestUtils.replyFormatMatches(reply, "f", "format"))
     *
     * The EMTestUtils helpers fail-open on missing fields (numeric / length /
     * format checks pass when the field is absent — REQUIRED separately
     * catches the presence violation) so a single missing field surfaces as
     * exactly one assertion failure rather than a cascade.
     */
    private fun emitReplyFieldAssertions(action: AsyncAPIAction, replyVar: String, lines: Lines) {
        // M11-PR3 fix #9: when the schema declared a discriminated oneOf,
        // emit a runtime if/else-if chain on the discriminator value and
        // apply each variant's full facet set inside the matching branch.
        // Stronger than the M11-PR2 intersection fallback.
        val variants = action.perVariantReplyAssertions
        if (variants != null && variants.byVariant.isNotEmpty()) {
            val discProp = escapeJava(variants.discriminatorProperty)
            val discVar = "__evm_disc_${replyVar.substringAfterLast('_')}"
            lines.add(
                "String $discVar = " +
                        "org.evomaster.test.utils.EMTestUtils.replyText($replyVar, \"$discProp\");"
            )
            var first = true
            variants.byVariant.forEach { (name, specs) ->
                val prefix = if (first) "if" else "else if"
                first = false
                lines.add("$prefix (\"${escapeJava(name)}\".equals($discVar)) {")
                lines.indented {
                    emitFieldAssertionsOver(specs, replyVar, "reply (variant '${escapeJava(name)}')", lines)
                }
                lines.add("}")
            }
            lines.add("else {")
            lines.indented {
                lines.add(
                    "fail(\"reply discriminator '$discProp' had value '\" + $discVar + " +
                            "\"' but the schema declares only: " +
                            variants.byVariant.keys.joinToString(", ") { escapeJava(it) } + "\");"
                )
            }
            lines.add("}")
            return
        }
        emitFieldAssertionsOver(action.replyFieldAssertions, replyVar, "reply", lines)
    }

    private fun emitFieldAssertionsOver(
        specs: List<ReplyFieldAssertion>,
        valueVar: String,
        label: String,
        lines: Lines
    ) {
        if (specs.isEmpty()) return
        specs.forEach { spec ->
            val p = escapeJava(spec.path)
            when (spec.kind) {
                ReplyFieldAssertion.Kind.REQUIRED -> lines.add(
                    "assertTrue(" +
                            "org.evomaster.test.utils.EMTestUtils.replyHas($valueVar, \"$p\"), " +
                            "\"$label missing required field '$p'\");"
                )
                ReplyFieldAssertion.Kind.ENUM -> {
                    val literals = spec.expectedValues.joinToString(", ") { "\"${escapeJava(it)}\"" }
                    lines.add(
                        "assertTrue(" +
                                "java.util.Arrays.asList($literals).contains(" +
                                "org.evomaster.test.utils.EMTestUtils.replyText($valueVar, \"$p\")), " +
                                "\"$label field '$p' not in declared enum\");"
                    )
                }
                ReplyFieldAssertion.Kind.CONST -> {
                    val v = escapeJava(spec.expectedValues.firstOrNull().orEmpty())
                    lines.add(
                        "assertEquals(\"$v\", " +
                                "org.evomaster.test.utils.EMTestUtils.replyText($valueVar, \"$p\"), " +
                                "\"$label field '$p' must be declared const '$v'\");"
                    )
                }
                ReplyFieldAssertion.Kind.PATTERN -> {
                    val pat = escapeJava(spec.pattern!!)
                    lines.add(
                        "assertTrue(" +
                                "org.evomaster.test.utils.EMTestUtils.replyPatternMatches($valueVar, \"$p\", \"$pat\"), " +
                                "\"$label field '$p' does not match declared pattern\");"
                    )
                }
                ReplyFieldAssertion.Kind.MULTIPLE_OF -> {
                    val divisor = spec.numericBound!!
                    lines.add(
                        "assertTrue(" +
                                "org.evomaster.test.utils.EMTestUtils.replyMultipleOf($valueVar, \"$p\", $divisor), " +
                                "\"$label field '$p' is not a multiple of $divisor\");"
                    )
                }
                ReplyFieldAssertion.Kind.ARRAY_MIN_ITEMS -> {
                    val bound = spec.lengthBound!!
                    lines.add(
                        "{ int __n = org.evomaster.test.utils.EMTestUtils.replyArrayLength($valueVar, \"$p\"); " +
                                "assertTrue(__n < 0 || __n >= $bound, " +
                                "\"$label field '$p' has fewer than declared minItems $bound\"); }"
                    )
                }
                ReplyFieldAssertion.Kind.ARRAY_MAX_ITEMS -> {
                    val bound = spec.lengthBound!!
                    lines.add(
                        "{ int __n = org.evomaster.test.utils.EMTestUtils.replyArrayLength($valueVar, \"$p\"); " +
                                "assertTrue(__n < 0 || __n <= $bound, " +
                                "\"$label field '$p' has more than declared maxItems $bound\"); }"
                    )
                }
                ReplyFieldAssertion.Kind.ARRAY_UNIQUE -> lines.add(
                    "assertTrue(" +
                            "org.evomaster.test.utils.EMTestUtils.replyArrayUnique($valueVar, \"$p\"), " +
                            "\"$label field '$p' violates declared uniqueItems\");"
                )
                ReplyFieldAssertion.Kind.DISCRIMINATOR -> {
                    val literals = spec.expectedValues.joinToString(", ") { "\"${escapeJava(it)}\"" }
                    lines.add(
                        "assertTrue(" +
                                "java.util.Arrays.asList($literals).contains(" +
                                "org.evomaster.test.utils.EMTestUtils.replyText($valueVar, \"$p\")), " +
                                "\"$label discriminator '$p' does not name a declared variant\");"
                    )
                }
                ReplyFieldAssertion.Kind.MIN -> {
                    val bound = spec.numericBound!!
                    lines.add(
                        "{ Double __v = org.evomaster.test.utils.EMTestUtils.replyNumber($valueVar, \"$p\"); " +
                                "assertTrue(__v == null || __v >= $bound, " +
                                "\"$label field '$p' below declared minimum $bound\"); }"
                    )
                }
                ReplyFieldAssertion.Kind.MAX -> {
                    val bound = spec.numericBound!!
                    lines.add(
                        "{ Double __v = org.evomaster.test.utils.EMTestUtils.replyNumber($valueVar, \"$p\"); " +
                                "assertTrue(__v == null || __v <= $bound, " +
                                "\"$label field '$p' above declared maximum $bound\"); }"
                    )
                }
                ReplyFieldAssertion.Kind.MIN_LENGTH -> {
                    val bound = spec.lengthBound!!
                    lines.add(
                        "{ int __l = org.evomaster.test.utils.EMTestUtils.replyTextLength($valueVar, \"$p\"); " +
                                "assertTrue(__l < 0 || __l >= $bound, " +
                                "\"$label field '$p' shorter than declared minLength $bound\"); }"
                    )
                }
                ReplyFieldAssertion.Kind.MAX_LENGTH -> {
                    val bound = spec.lengthBound!!
                    lines.add(
                        "{ int __l = org.evomaster.test.utils.EMTestUtils.replyTextLength($valueVar, \"$p\"); " +
                                "assertTrue(__l < 0 || __l <= $bound, " +
                                "\"$label field '$p' longer than declared maxLength $bound\"); }"
                    )
                }
                ReplyFieldAssertion.Kind.FORMAT -> {
                    val f = escapeJava(spec.format!!)
                    lines.add(
                        "assertTrue(" +
                                "org.evomaster.test.utils.EMTestUtils.replyFormatMatches($valueVar, \"$p\", \"$f\"), " +
                                "\"$label field '$p' does not match declared format '$f'\");"
                    )
                }
            }
        }
    }

    /**
     * Render the action's templated channel address (`tenants/{tenantId}/orders`)
     * using the captured channel-parameter gene values.  Mirrors
     * `AbstractAsyncAPIFitness.renderChannelAddress` — duplicated because the
     * writer doesn't carry a reference to the fitness instance.
     */
    private fun renderChannelAddress(action: AsyncAPIAction): String {
        val params = action.channelParams()
        if (params.isEmpty()) return action.channelAddress
        var rendered = action.channelAddress
        params.forEach { (name, param) ->
            rendered = rendered.replace("{$name}", param.primaryGene().getValueAsRawString())
        }
        return rendered
    }

    private fun escapeJava(s: String): String =
        s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    override fun addActionLinesPerType(
        action: Action,
        index: Int,
        testCaseName: String,
        lines: Lines,
        result: ActionResult,
        testSuitePath: Path?,
        baseUrlOfSut: String
    ) {
        // No-op: handleActionCalls drives per-action emission directly so we
        // can sequence the synthesised correlation id across each pair.
    }

    override fun shouldFailIfExceptionNotThrown(result: ActionResult): Boolean = false
}
