# MCP Problem Type — Implementation Plan

This document describes the incremental development plan for adding **MCP (Model Context Protocol)**
as a new `problemType` in EvoMaster.

Each PR is designed to be self-contained: the build must not break, all existing tests must continue
to pass, and the newly added code can be safely disabled or guarded behind the `MCP(experimental=true)`
flag.

---

## Background: What is MCP?

[Model Context Protocol](https://modelcontextprotocol.io) is an open protocol (by Anthropic) for
exposing server-side capabilities — called **Tools**, **Resources**, and **Prompts** — to AI
clients. The wire protocol is **JSON-RPC 2.0**.

**Scope of this implementation:** HTTP transport only, using plain POST request/response. Each
JSON-RPC call is a single HTTP POST; the server returns the full JSON-RPC response in the HTTP
response body. SSE streaming and stdio transport are explicitly out of scope and deferred to
future work.

From EvoMaster's perspective, an MCP server is analogous to an RPC server: it exposes a typed
interface (discovered at runtime via `tools/list`) and EvoMaster generates sequences of tool calls
with varying inputs to maximise code coverage and find faults.

This makes MCP conceptually closer to `RPC` than to `REST` or `GraphQL`. The implementation will
mirror the RPC problem type wherever possible.

---

## Inheritance chain (target state)

```
Individual
  └── EnterpriseIndividual
        └── ApiWsIndividual
              └── McpIndividual

Action
  └── ApiWsAction
        └── McpCallAction

Sampler<T>
  └── ApiWsSampler<T>
        └── McpSampler

FitnessFunction<T>
  └── ApiWsFitness<T>
        └── McpFitness

StructureMutator
  └── ApiWsStructureMutator
        └── McpStructureMutator

EnterpriseModule (Guice)
  └── McpModule
```

---

## Phase 0 — Scaffolding

Goals: the enum value exists, all `when` branches in `Main.kt` are updated, and stub classes
compile cleanly. No MCP functionality is reachable yet; any attempt to use `--problemType MCP`
will print a clear "not yet implemented" message.

### PR 0.1 — Add `MCP` to `EMConfig.ProblemType`

**Files changed:**
- `core/src/main/kotlin/org/evomaster/core/EMConfig.kt`

**Changes:**
- Add `MCP(experimental = true)` to the `ProblemType` enum.
- Add a `@Cfg` entry for `--problemType MCP` to the description string if one exists.

**Outcome:** The enum value compiles. The existing `else -> throw IllegalStateException(...)` branches
in `Main.kt` will catch `MCP` at runtime, so using it fails gracefully with a clear message.
No other files need touching. All existing tests pass unchanged.

---

### PR 0.2 — Controller-API DTOs (`client-java`)

**Files to create (all in `client-java/controller-api/src/main/java/org/evomaster/client/java/controller/api/dto/problem/`):**

```
McpProblemDto.java
mcp/McpToolSchemaDto.java
mcp/McpToolCallResultDto.java
```

**`McpProblemDto.java`** (extends `ProblemInfoDto`):
```java
public class McpProblemDto extends ProblemInfoDto {
    /** Base URL of the MCP server, e.g. http://localhost:8080/mcp */
    public String serverUrl;
    /** Optional Bearer token for authenticated servers */
    public String authToken;
    /** Optional: pre-discovered tool schemas; null means EvoMaster discovers them */
    public List<McpToolSchemaDto> tools;
}
```

**`McpToolSchemaDto.java`**:
```java
public class McpToolSchemaDto {
    public String name;
    public String description;
    /** JSON Schema string for the tool's input parameters */
    public String inputSchemaJson;
}
```

**`McpToolCallResultDto.java`** (returned per action execution):
```java
public class McpToolCallResultDto {
    public boolean isError;
    /** MCP error code if isError == true */
    public Integer errorCode;
    public String errorMessage;
    /** Raw JSON of the result content list */
    public String resultJson;
}
```

**Files changed:**
- `client-java/controller-api/src/main/java/org/evomaster/client/java/controller/api/dto/SutInfoDto.java`
  — add `public McpProblemDto mcpProblem;`

**Outcome:** DTOs compile in client-java. `SutInfoDto` has the new field. No core changes.

---

### PR 0.3 — Core data-model stubs

**Directory:** `core/src/main/kotlin/org/evomaster/core/problem/mcp/`

**Files to create:**

`McpCallResultCategory.kt`:
```kotlin
enum class McpCallResultCategory {
    SUCCESS,
    TOOL_ERROR,
    INVALID_PARAMS,
    TRANSPORT_ERROR,
    INTERNAL_ERROR
}
```

`McpCallResult.kt` — extends `ActionResult`, stores `McpCallResultCategory` and raw response JSON.

`McpCallAction.kt` — extends `ApiWsAction`:
- Fields: `toolName: String`, `id: String` (= toolName for now)
- `getName()` returns `id`
- `seeTopGenes()` returns `parameters.flatMap { it.seeGenes() }`
- `copyContent()` deep-copies parameters

`McpIndividual.kt` — mirrors `RPCIndividual` exactly, replacing `RPCCallAction` with `McpCallAction`.

**Outcome:** All four classes compile. No search algorithm or Guice wiring yet.

---

### PR 0.4 — Service stubs + full `Main.kt` wiring

**Files to create** under `core/src/main/kotlin/org/evomaster/core/problem/mcp/service/`:

`McpEndpointsHandler.kt` — stub, single method `initActionCluster(...)` that throws
`UnsupportedOperationException("MCP not yet implemented")`.

`McpSampler.kt` — extends `ApiWsSampler<McpIndividual>`:
- `@PostConstruct initialize()` throws `UnsupportedOperationException`.
- `sampleAtRandom()` and `smartSample()` throw `UnsupportedOperationException`.

`McpFitness.kt` — extends `ApiWsFitness<McpIndividual>`:
- `doCalculateCoverage(...)` throws `UnsupportedOperationException`.

`McpStructureMutator.kt` — extends `ApiWsStructureMutator`:
- `mutateStructure(...)` throws `UnsupportedOperationException`.

`McpModule.kt` — extends `EnterpriseModule`, mirrors `RPCModule.kt` exactly, all bindings
point to the stub classes above.

**Files changed:**
- `core/src/main/kotlin/org/evomaster/core/Main.kt`:
  - Auto-detect: `else if (info.mcpProblem != null) config.problemType = EMConfig.ProblemType.MCP`
  - `init()` when-block: `EMConfig.ProblemType.MCP -> McpModule()`
  - `getAlgorithmKey()` when-block: `EMConfig.ProblemType.MCP -> getAlgorithmKeyMcp(config)` (new function, mirrors `getAlgorithmKeyRPC`)
  - Phase handlers (`phaseFlaky`, `phaseSecurity`, `phaseHttpOracle`): add `EMConfig.ProblemType.MCP -> { log.warn("... not yet handled for MCP") }`

**Outcome:** The project builds fully. Running with `--problemType MCP` reaches
`McpSampler.initialize()` and throws `UnsupportedOperationException("MCP not yet implemented")` —
visible in logs, not a silent crash. All existing tests pass.

---

## Phase 1 — MCP Client

Goal: a standalone `McpClient` that can speak JSON-RPC 2.0 over HTTP to any MCP server. No
EvoMaster search integration yet — just the transport layer, tested in isolation.

### PR 1.1 — `McpClient`: JSON-RPC 2.0 over HTTP POST

**File:** `core/src/main/kotlin/org/evomaster/core/problem/mcp/service/McpClient.kt`

The client sends each JSON-RPC 2.0 call as a plain HTTP POST and reads the full response from
the HTTP response body. No SSE, no streaming, no session state. Each call is stateless from the
transport perspective.

Behaviour:
- Serialises the JSON-RPC 2.0 request envelope (`jsonrpc`, `method`, `params`, `id`) as the POST body
- Reads the JSON-RPC 2.0 response envelope from the HTTP response body (handles both `result` and `error` forms)
- Throws `McpTransportException` on non-2xx HTTP status or network errors

Minimum methods:
```kotlin
fun initialize(): McpInitializeResult   // sends the MCP initialize handshake
fun listTools(): List<McpToolSchemaDto>
fun callTool(name: String, arguments: Map<String, Any?>): McpToolCallResultDto
fun shutdown()                          // sends the MCP shutdown notification
```

Use the existing OkHttp / Jackson infrastructure already in EvoMaster core — do not introduce new
HTTP client dependencies.

**Also create:**
- `McpTransportException.kt` — runtime exception for transport-level failures
- `McpClientConfig.kt` — holds `serverUrl`, `authToken`, `timeoutMs`

**Tests:** unit tests using `MockWebServer` (already on the test classpath) that verify correct
JSON-RPC request framing, `result` response parsing, and `error` response parsing.

**Outcome:** `McpClient` is independently testable. `McpSampler` still throws
`UnsupportedOperationException`.

---

### PR 1.2 — `McpEndpointsHandler`: tool discovery + primitive Gene conversion

**File:** `core/src/main/kotlin/org/evomaster/core/problem/mcp/service/McpEndpointsHandler.kt`

Implements:
```kotlin
fun initActionCluster(
    problem: McpProblemDto,
    actionCluster: MutableMap<String, Action>,
    client: McpClient
)
```

Steps:
1. Call `client.listTools()` to get tool schemas.
2. For each tool, parse its `inputSchemaJson` (JSON Schema string) and build a list of `Param`
   objects backed by EvoMaster `Gene` types.
3. Construct a `McpCallAction` per tool and insert it into `actionCluster`.

**JSON Schema → Gene mapping (initial, primitive types only):**

| JSON Schema type | Gene |
|---|---|
| `string` | `StringGene` |
| `integer` | `IntegerGene` |
| `number` | `DoubleGene` |
| `boolean` | `BooleanGene` |
| `string` with `enum` | `EnumGene<String>` |
| required vs optional | wrap in `OptionalGene` when not required |

Reuse GraphQL's JSON Schema gene-building logic where it already exists
(`core/.../problem/graphql/`). If a type is unsupported, log a warning and skip the parameter —
do not crash.

**Tests:** unit tests with hand-crafted JSON Schema strings asserting correct `Gene` tree structure.

---

### PR 1.3 — `McpEndpointsHandler`: composite types

Extend the JSON Schema → Gene conversion to handle:

| JSON Schema construct | Gene |
|---|---|
| `object` with `properties` | `ObjectGene` |
| `array` with `items` | `ArrayGene<T>` |
| `oneOf` / `anyOf` | `ChoiceGene` (or best available equivalent) |
| nested `object` | recursive `ObjectGene` |
| `null` allowed | `NullableGene` wrapper |

**Tests:** unit tests for each new construct.

---

## Phase 2 — Test Generation

### PR 2.1 — `McpSampler`: working implementation

Replace the stub in `McpSampler.kt` with a real implementation:

**`@PostConstruct initialize()`:**
1. `rc.checkConnection()` / `rc.startSUT()`
2. Fetch `SutInfoDto`; extract `mcpProblem`
3. Instantiate `McpClient` with `mcpProblem.serverUrl` and optional auth token
4. Call `client.initialize()` (MCP handshake)
5. Call `rpcHandler.initActionCluster(mcpProblem, actionCluster, client)`
6. Call `initSqlInfo(infoDto)`, `initAdHocInitialIndividuals()`

**`sampleAtRandom()`:**
- Pick `len` ∈ `[1, config.maxTestSize]` tools at random from `actionCluster`
- Wrap each in `EnterpriseActionGroup(mutableListOf(action), McpCallAction::class.java)`
- Build and return `McpIndividual`

**`smartSample()`:**
- Drain `adHocInitialIndividuals` first (one per tool), then fall back to `sampleAtRandom()`

**`initAdHocInitialIndividuals()`:**
- One `McpIndividual` per tool in `actionCluster`, each with one action, genes initialized.

**Outcome:** The sampler produces real individuals. Fitness still throws `UnsupportedOperationException`,
so a full run still fails, but the sampling phase can be observed by stepping through.

---

### PR 2.2 — `McpStructureMutator`: working implementation

Replace the stub with a real implementation mirroring `RPCStructureMutator`:

```kotlin
override fun mutateStructure(individual, evaluatedIndividual, mutatedGenes, targets) {
    if (!individual.canMutateStructure()) return
    if (config.maxTestSize == 1) return
    // randomly add or remove one McpCallAction
}
```

Add helper `addInitializingActions()` delegating to the inherited base method.

**Tests:** unit tests verifying add/remove produce valid `McpIndividual` instances.

---

## Phase 3 — Fitness Evaluation

### PR 3.1 — `McpFitness`: basic execution

Replace `doCalculateCoverage()` stub:

```kotlin
override fun doCalculateCoverage(individual, targets, allTargets, fullyCovered, descriptiveIds): EvaluatedIndividual<McpIndividual>? {
    rc.resetSUT()
    val actionResults = mutableListOf<ActionResult>()

    doDbCalls(individual.seeInitializingActions().filterIsInstance<SqlAction>(), actionResults)

    val fv = FitnessValue(individual.size().toDouble())

    individual.seeMainExecutableActions().forEachIndexed { index, action ->
        val ok = executeAction(action, index, actionResults)
        if (!ok) return@forEachIndexed
    }

    val dto = updateFitnessAfterEvaluation(targets, allTargets, fullyCovered, descriptiveIds, individual, fv)
        ?: return null
    handleExtra(dto, fv)

    return EvaluatedIndividual(fv, individual.copy() as McpIndividual, actionResults, ...)
}
```

**`executeAction()`:**
1. Build the tool call DTO from the action's genes (serialize to `Map<String, Any?>`)
2. Delegate to `rc.executeMcpToolCallAndGetResult(dto)` — new controller API endpoint (see PR 3.1b)
   **or** call `McpClient.callTool()` directly from core (simpler for initial cut)
3. Populate `McpCallResult` with the response

At this point a full EvoMaster run with `--problemType MCP` should complete without crashing,
even if coverage and fault detection are minimal.

**Companion PR 3.1b (controller-api):** Add `executeMcpToolCall` endpoint to the REST controller
interface in `client-java/controller-api` (mirrors `executeNewRPCActionAndGetResponse`).

---

### PR 3.2 — `McpFitness`: response-based fitness targets

Add `handleResponseTargets()` to translate `McpCallResultCategory` into `FitnessValue` updates,
mirroring `RPCFitness.handleAdditionalTargetsDescription()`:

| Category | Fitness targets updated |
|---|---|
| `SUCCESS` | `handled:<toolName>` → 1.0 |
| `TOOL_ERROR` | `fault:tool_error:<toolName>` → 1.0 |
| `INVALID_PARAMS` | `fault:invalid_params:<toolName>` → 1.0 (separate target to reward invalid-input discovery) |
| `INTERNAL_ERROR` | `fault:internal:<lastStatement>:<toolName>` → 1.0 |
| `TRANSPORT_ERROR` | no reward |

Add `ExperimentalFaultCategory` entries for `MCP_TOOL_ERROR` and `MCP_INTERNAL_ERROR` in
`core/.../problem/enterprise/ExperimentalFaultCategory.kt`.

---

## Phase 4 — Test Output

### PR 4.1 — `McpTestCaseWriter`: initial code generation

**File:** `core/src/main/kotlin/org/evomaster/core/output/service/McpTestCaseWriter.kt`

Extends `TestCaseWriter`. Generates Java test code (JUnit 5) that:
1. Creates an MCP HTTP client pointed at the SUT URL
2. For each `McpCallAction` in the individual, emits a `callTool("toolName", Map.of(...))` call
3. Adds a basic assertion that no transport error occurred

The generated code should be runnable against the same MCP server that EvoMaster tested, using
a thin MCP client utility class that is also emitted into the test output directory.

---

### PR 4.2 — `McpTestCaseWriter`: response assertions

Extend the generated tests to assert:
- Tool result is not an error when `McpCallResult` records `SUCCESS`
- Specific result content assertions when the tool returns structured JSON

---

## Phase 5 — Controller Driver

### PR 5.1 — `AbstractMcpController` (client-java)

**File:** `client-java/controller/src/main/java/org/evomaster/client/java/controller/problem/mcp/AbstractMcpController.java`

Base class for SUT drivers that wrap an MCP server. Provides:
- Abstract `startMcpServer()` / `stopMcpServer()` lifecycle hooks
- Default `getMcpProblemInfo()` returning `McpProblemDto` with a localhost URL
- Delegates `resetStateOfSUT()` to `resetMcpServer()` (abstract)

**Outcome:** Users can subclass `AbstractMcpController` to test their own MCP servers with
white-box instrumentation.

---

### PR 5.2 — Example MCP SUT + E2E test

**Location:** `core-tests/e2e-tests/src/test/java/.../mcp/`

- A minimal MCP server (Kotlin, using an MCP server library or hand-rolled JSON-RPC) that exposes
  2–3 tools with simple business logic (e.g., `add(a, b)`, `divide(a, b)` — the latter can throw
  on divide-by-zero as a detectable fault)
- An `ExampleMcpController` extending `AbstractMcpController`
- An E2E test that runs EvoMaster against this server and asserts:
  - Tools were discovered
  - At least one successful tool call was generated
  - The divide-by-zero fault was detected (when coverage permits)

---

## Phase 6 — Black-box mode

### PR 6.1 — `McpBlackBoxModule` + black-box sampler

Mirrors `BlackBoxRestModule`. Allows:
```bash
evomaster --problemType MCP --blackBox --bbTargetUrl http://myserver/mcp
```

No SUT controller required. EvoMaster:
1. Calls `tools/list` directly from core
2. Generates and executes tool calls without bytecode coverage (fitness = response targets only)

**Files:**
- `McpBlackBoxModule.kt`
- `McpBlackBoxSampler.kt` (no `rc` calls; uses `McpClient` directly)
- Update `Main.kt` `init()` to instantiate `McpBlackBoxModule` when `config.blackBox == true`

---

## Phase 7 — Future work (not scoped yet)

These are items to design after the core loop is proven working. They are listed here to avoid
architectural dead-ends in earlier phases.

- **Resources support** — `resources/list` and `resources/read` as a second action type
  (`McpResourceAction`) added to `McpIndividual`
- **Prompts support** — `prompts/list` + `prompts/get`
- **SSE streaming** — handling `text/event-stream` responses for long-running tool calls
- **stdio transport** — subprocess lifecycle management; requires careful integration with
  `EmbeddedSutController`
- **Stateful session testing** — tracking which tools mutate server state and ordering calls
  accordingly (analogous to REST resource dependency graph)
- **Seed test cases** — allow users to provide example tool-call sequences via
  `McpProblemDto.seededTests`
- **OAuth / header auth** — extend `McpAuthenticationInfo` beyond Bearer tokens
- **`seedTestCases` support** in `McpSampler`

---

## PR dependency graph

```
0.1 (enum)
  └── 0.2 (DTOs)
        └── 0.3 (data model stubs)
              └── 0.4 (service stubs + Main.kt wiring)   ← build stable from here
                    ├── 1.1 (McpClient)
                    │     └── 1.2 (handler: primitives)
                    │           └── 1.3 (handler: composites)
                    │                 └── 2.1 (McpSampler)
                    │                       └── 2.2 (McpStructureMutator)
                    │                             └── 3.1 (McpFitness: execution)
                    │                                   └── 3.2 (McpFitness: targets)
                    │                                         └── 4.1 (writer stub)
                    │                                               └── 4.2 (writer assertions)
                    │
                    ├── 5.1 (AbstractMcpController)  [parallel with 1.x, only needs 0.2]
                    │     └── 5.2 (E2E test)          [needs 3.1 and 5.1]
                    │
                    └── 6.1 (black-box mode)           [needs 2.1 + 1.1]
```

PRs within Phase 0 are strictly sequential. PRs 5.1 and 6.1 can be developed in parallel with
Phase 1 once Phase 0 is merged.

---

## Key file paths (target state)

| Purpose | Path |
|---|---|
| Enum + config | `core/.../EMConfig.kt` |
| Main wiring | `core/.../Main.kt` |
| Individual | `core/.../problem/mcp/McpIndividual.kt` |
| Action | `core/.../problem/mcp/McpCallAction.kt` |
| Call result | `core/.../problem/mcp/McpCallResult.kt` |
| Result category | `core/.../problem/mcp/McpCallResultCategory.kt` |
| Client | `core/.../problem/mcp/service/McpClient.kt` |
| Endpoints handler | `core/.../problem/mcp/service/McpEndpointsHandler.kt` |
| Sampler | `core/.../problem/mcp/service/McpSampler.kt` |
| Fitness | `core/.../problem/mcp/service/McpFitness.kt` |
| Structure mutator | `core/.../problem/mcp/service/McpStructureMutator.kt` |
| Guice module | `core/.../problem/mcp/service/McpModule.kt` |
| Test writer | `core/.../output/service/McpTestCaseWriter.kt` |
| Problem DTO | `client-java/.../dto/problem/McpProblemDto.java` |
| Tool schema DTO | `client-java/.../dto/problem/mcp/McpToolSchemaDto.java` |
| Controller base | `client-java/.../controller/problem/mcp/AbstractMcpController.java` |
| E2E test | `core-tests/e2e-tests/.../mcp/` |
