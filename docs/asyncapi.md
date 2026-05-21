# AsyncAPI

EvoMaster has experimental support for fuzzing AsyncAPI 3.0 SUTs over Apache Kafka. Both **black-box** and **white-box** modes are wired in — the search publishes test messages onto channels the SUT consumes from, optionally awaits replies on `request`/`reply` channels, and scores the run with a mix of schema-derivable and (white-box only) bytecode-coverage fitness targets.

This is a starter slice. Scope and limitations are listed at the end of the page.

## Quick start (black-box)

You need:

- An AsyncAPI 3.0 schema URL (or local file) describing the SUT's channels and operations.
- A reachable Kafka broker (the SUT must be configured to talk to the same one).

```bash
java -jar evomaster.jar \
  --blackBox true \
  --problemType ASYNCAPI \
  --bbAsyncApiUrl http://localhost:8080/asyncapi.yaml \
  --bbBrokerUrl localhost:9092 \
  --maxTime 60s \
  --outputFormat JAVA_JUNIT_5
```

CLI options new for AsyncAPI:

| Flag | Required | Notes |
|---|---|---|
| `--problemType ASYNCAPI` | yes | the type is `experimental`; expect rough edges |
| `--bbAsyncApiUrl` | black-box only | URL or `file://` path of the AsyncAPI 3.0 schema |
| `--bbBrokerUrl` | black-box only | broker bootstrap servers (Kafka: `host:port`) |

The familiar `--outputFormat`, `--maxTime`, etc. all behave as for REST. `PYTHON_UNITTEST` is rejected for AsyncAPI; pick `JAVA_JUNIT_5` (default) or `KOTLIN_JUNIT_5`.

### Authenticated brokers

Real Kafka deployments usually require SASL or SSL on the connection. The black-box engine supports three families today (M9):

| Flag | Notes |
|---|---|
| `--bbBrokerAuthType` | one of `NONE` (default), `SASL_PLAIN`, `SASL_SCRAM_256`, `SSL` |
| `--bbBrokerUsername` / `--bbBrokerPassword` | required for both SASL variants |
| `--bbBrokerSaslOverTls` | when true, SASL is wrapped in TLS (`SASL_SSL`); otherwise `SASL_PLAINTEXT` |
| `--bbBrokerTruststorePath` / `--bbBrokerTruststorePassword` | server cert verification (SSL only) |
| `--bbBrokerKeystorePath` / `--bbBrokerKeystorePassword` | client cert for mTLS (SSL only; optional) |

SASL/OAUTHBEARER and Kerberos are not yet supported; track the follow-up in the M9 plan.

The AsyncAPI parser also surfaces `components.securitySchemes` and per-operation `security:` references into the in-memory model; the broker-side auth applied at connect time is driven by the CLI flags above (the schema's declared scheme names are advisory). White-box mode will plumb driver-supplied broker auth in a follow-up; for now use the CLI flags in both modes.

### Transports: Kafka, WebSocket, and AMQP (M11-PR8, M11-PR9)

AsyncAPI 3.0 channels can declare bindings for several wire transports. The black-box engine implements three so far, switched with one flag:

| `--bbBrokerTransport` | `--bbBrokerUrl` shape | Channel address shape |
|---|---|---|
| `KAFKA` (default) | `host:port` (Kafka bootstrap servers) | topic name |
| `WEBSOCKET` | `ws://host:port` or `wss://host:port` origin | endpoint path (joined onto the origin), or a fully-qualified `ws://` / `wss://` URL inline |
| `AMQP` | `amqp://user:pass@host:port/vhost` URI, or bare `host:port` (normalised to `amqp://host:port`) | routing key on the default exchange (= queue name) |

WebSocket uses JDK 11+ `java.net.http.WebSocket` (no extra runtime dep). Each channel keeps one connection open across publish/await calls so request/reply on the same socket works. Headers are JSON-enveloped into `{ "headers": {...}, "payload": "..." }` because raw WebSocket frames have no native headers; servers that ignore the envelope still see a JSON TEXT frame and round-trip cleanly. Broker auth (`--bbBrokerAuthType`) and the embedded Testcontainers broker (`--asyncApiEmbedBroker`) are rejected with WebSocket — both are Kafka-specific. Socket.IO and MQTT are out of scope for this slice.

AMQP uses the RabbitMQ `com.rabbitmq:amqp-client` (AMQP 0-9-1). The channel address is treated as the routing key on the default exchange (`""`) — equivalent to "publish to the queue with this name." Publishers send via `basicPublish`; subscribers declare the routing-key queue idempotently and consume via `basicConsume` with auto-ack. Headers ride in the message's native AMQP header table (UTF-8 string values). Credentials live in the broker URI (`amqp://user:pass@host:port/vhost`); the Kafka-shaped `--bbBrokerAuthType` flag is rejected because AMQP auth doesn't fit the SASL/SSL model. `--asyncApiEmbedBroker` is also rejected — the embed path is Kafka-specific. AMQP 1.0 is a different protocol family (Azure Service Bus, ActiveMQ Artemis) and not handled by this transport. Named-exchange routing with explicit queue bindings declared in the AsyncAPI `bindings.amqp` block is a follow-up; the starter slice handles default-exchange-as-queue, which is the convention the bookworm-family validation SUTs use.

### Output-channel observation (M9)

For every `action: RECEIVE` operation declared in the schema (= SUT-produced channel), the engine appends one `SUBSCRIBE_OUTPUT` action to each individual. The fitness layer brackets the post-PUBLISH window, collects everything that arrived on the channel, and emits these targets:

- `OUTPUT_RECEIVED:<channel>` / `OUTPUT_NOTHING:<channel>` — whether the SUT emitted anything during the window. Always emitted (not gated by `--advancedBlackBoxCoverage`).
- `OUTPUT_SCHEMA_VALID:<channel>:<messageId>` / `OUTPUT_SCHEMA_INVALID:<channel>` — per-arrival, per-variant schema match.
- `OUTPUT_MESSAGE_TYPE:<channel>=<messageId>` — fires the first time each declared variant is observed in a run.
- `OUTPUT_FIELD_PRESENCE:<channel>:<messageId>:<field>=present|absent` — per declared property of the matched variant; mirrors `FIELD_PRESENCE` on the publish side. Gated by `--advancedBlackBoxCoverage`.

The listen window is configured with `--asyncApiOutputObservationWindowMs` (default `1000`). Set to `0` to disable output observation entirely. Generated JUnit 5 tests include a `kafkaCollectAllWithin(...)` call per output channel so the captured message count is preserved as a debugging aid.

This is an *observational* oracle: the schema doesn't encode causality between a publish and an emitted event, so attribution is window-only. On a dedicated test broker (the engine is the only client) false positives from concurrent traffic are negligible.

### Per-field reply assertions (M9-PR5)

When the SUT replies to a `request` operation, the engine now walks the declared reply schema and emits one fitness target per (field × declared facet) pair, on top of the existing binary `REPLY_SCHEMA_VALID` signal:

| Target family | Fires when |
|---|---|
| `REPLY_FIELD:REQUIRED_PRESENT:<variant>:<channel>:<op>:<field>` / `_REQUIRED_ABSENT` | required field present / missing |
| `REPLY_FIELD:ENUM_IN_RANGE:<variant>:<channel>:<op>:<field>` / `_OUT_OF_RANGE` | observed enum value in / not in declared set |
| `REPLY_FIELD:BOUNDARY_OK:<variant>:<channel>:<op>:<field>` / `_VIOLATED` | numeric value within / outside `minimum`/`maximum` |
| `REPLY_FIELD:LENGTH_OK:<variant>:<channel>:<op>:<field>` / `_VIOLATED` | string length within / outside `minLength`/`maxLength` |
| `REPLY_FIELD:FORMAT_OK:<variant>:<channel>:<op>:<field>=<format>` / `_VIOLATED` | string value matches / doesn't match declared format (email, uuid, date, date-time, uri, hostname, ipv4) |

Generated JUnit 5 tests get matching assertions for the most common subset (`required` + `enum`) via `EMTestUtils.replyHas(...)` and `EMTestUtils.replyText(...)`. Bounds / length / format show up as fitness gradients today; per-field test assertions for those families will follow when the validation pass against real products surfaces concrete cases.

## White-box

White-box requires an EvoMaster driver in the SUT's repo that returns `AsyncAPIProblem` from `getProblemInfo()`:

```java
@Override
public ProblemInfo getProblemInfo() {
    return new AsyncAPIProblem(
        "http://localhost:" + getSutPort() + "/asyncapi.yaml",
        kafka.getBootstrapServers()
    );
}
```

Start the driver (defaults to controller port 40100) and run EvoMaster pointing at it:

```bash
java -jar evomaster.jar \
  --problemType ASYNCAPI \
  --maxTime 60s \
  --outputFormat JAVA_JUNIT_5
```

The schema URL and broker URL come from the driver, not the CLI, so neither `--bbAsyncApiUrl` nor `--bbBrokerUrl` is needed in white-box.

The white-box fitness layers branch/line coverage from the SUT's bytecode on top of the same schema-derivable targets the black-box mode produces. Coverage is polled once per individual after the action loop completes — the broker hop between EvoMaster's publish call and the SUT's `@KafkaListener` callback is asynchronous, so per-action polling would be racy.

A worked example lives in the WFD dataset at `jdk_17_maven/cs/messaging/rest-kafka-ncs` (driver in `jdk_17_maven/em/embedded/messaging/rest-kafka-ncs`).

## How the schema drives the search

EvoMaster only directly tests **operations the SUT consumes from**:

- AsyncAPI 3.0 `send` operations (the application sends a message) translate to a `PUBLISH` action — EvoMaster mutates the message payload's gene tree and publishes it.
- A `send` operation that declares `reply` (request/reply pattern) gets a paired `SUBSCRIBE_REPLY` action right after the `PUBLISH`. The structure mutator keeps the pair adjacent.
- `receive` operations (the SUT publishes; EvoMaster would observe) are skipped in the starter slice — there is no way to *trigger* the SUT through them, only to assert on them, and standalone observation actions aren't yet modelled.

The reply-message correlation header is read from the AsyncAPI `correlationId.location` field, e.g. `'$message.header#/evm-correlation-id'`. EvoMaster generates a unique UUID per publish, stamps it on the outgoing Kafka header, and filters incoming replies by the same value.

### Fitness targets

For each `PUBLISH` and the optional paired `SUBSCRIBE_REPLY`, the search records:

- `Local:DELIVERY_OK:<channel>:<operation>` / `DELIVERY_FAIL:...`
- `Local:REPLY_RECEIVED:<channel>:<operation>` / `REPLY_TIMEOUT:...`
- `Local:REPLY_CORRELATION_MATCH:<channel>:<operation>` (when the schema declares a correlationId)
- `Local:REPLY_SCHEMA_VALID:<channel>:<operation>` / `REPLY_SCHEMA_INVALID:...`
- `Local:REPLY_VARIANT:<variantName>:<channel>:<operation>` (one target per `oneOf`/`anyOf` branch in the reply payload)

Schema-validity is the cheap "required + enum" check, not full JSON-Schema validation. White-box runs additionally pull branch/line targets from the EM Driver via the existing `RemoteController.getTestResults` call.

## Generated tests

The starter slice emits a structural JUnit 5 skeleton with one test method per individual and one comment block per action describing what was published or awaited. Real `KafkaProducer` / `KafkaConsumer` setup in the generated tests is parked for the next slice — keeping it out lets the test suite compile without dragging `kafka-clients` into the project's runtime dependencies.

## Fire-and-forget operations and the hybrid wait

For `send` operations that declare a `reply`, EvoMaster awaits the reply (5s default) and that wait acts as the synchronisation barrier between the publish and the SUT's consumer code finishing.

For fire-and-forget `send` operations there is no such barrier. The behaviour depends on the mode:

- **Black-box** sleeps for `--asyncApiFireAndForgetSettleMs` (default 200ms) after each fire-and-forget publish. Tune this if your consumers are slow.
- **White-box** runs a hybrid coverage-stabilisation strategy: it polls the EM Driver's `getTestResults` every `--asyncApiCoverageStabilisationPollMs` (default 100ms) until the covered-target count has been stable for `--asyncApiCoverageStabilisationWindowMs` (default 300ms), capped at `--asyncApiCoverageStabilisationMaxMs` (default 3000ms). The simple settle still runs as a lower-bound floor before polling starts, so the driver has time to receive at least one batch of targets before "stable" is meaningful.

The settle / polling are skipped entirely for `request`/`reply` operations because the reply-await already serves as the barrier.

## Known limitations (starter slice)

- **AsyncAPI 3.0 only.** 2.x schemas are rejected with a clear error referencing this page; 2.x support will go through the same internal model and is mechanical to add.
- **Kafka only.** The `MessageBrokerClient` interface is shaped to accept MQTT/AMQP implementations later, but only `KafkaBrokerClient` ships today.
- **Fixed reply timeout.** 5 seconds per `SUBSCRIBE_REPLY`; not yet configurable. (The hybrid wait above only addresses fire-and-forget operations.)
- **No driver-side `ScheduleTask`-style instrumentation.** White-box coverage relies on the SUT's `@KafkaListener` callback running before the end-of-individual poll fires.
- **Generated test bodies are stubs.** Action sequences and outcomes are emitted as comments; concrete Kafka client code generation is follow-up work.
- **No content-equality reply oracles.** Assertions stay schema-derivable: schema validity, correlation match, oneOf-variant detection. SUT-specific oracles are out of scope and shouldn't be added — the AsyncAPI search is meant to ride on the schema.
- **Black-box and white-box generation in the same run** is not supported — pick one via `--blackBox`.

## Pointers

- Schema parsing: `core/.../problem/asyncapi/schema/AsyncAPIAccess.kt`
- Action data model: `core/.../problem/asyncapi/data/AsyncAPIAction.kt`
- Action builder: `core/.../problem/asyncapi/builder/AsyncAPIActionBuilder.kt`
- Fitness functions: `core/.../problem/asyncapi/service/fitness/{AbstractAsyncAPIFitness,AsyncAPIBlackBoxFitness,AsyncAPIFitness}.kt`
- Broker bridge: `core/.../problem/asyncapi/broker/{MessageBrokerClient,KafkaBrokerClient}.kt`
- Driver-side DTO: `client-java/controller/.../problem/AsyncAPIProblem.java`
