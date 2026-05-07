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

## Known limitations (starter slice)

- **AsyncAPI 3.0 only.** 2.x schemas are rejected with a clear error referencing this page; 2.x support will go through the same internal model and is mechanical to add.
- **Kafka only.** The `MessageBrokerClient` interface is shaped to accept MQTT/AMQP implementations later, but only `KafkaBrokerClient` ships today.
- **Fixed reply timeout.** 5 seconds per `SUBSCRIBE_REPLY`. The plan calls for a hybrid coverage-stabilisation strategy; tracked as follow-up.
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
