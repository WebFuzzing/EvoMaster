# Using LLMs with EvoMaster

EvoMaster has experimental LLM-assisted heuristics. They are disabled by default and can be
enabled with `--llm true`. The provider is selected with `--llmProvider`:

| Provider | Notes |
|---|---|
| `OLLAMA` | local models, default `--llmURL http://localhost:11434` |
| `OPENAI` | default `--llmURL https://api.openai.com/v1` |
| `DEEPSEEK` | OpenAI-compatible, default `--llmURL https://api.deepseek.com` |
| `AZURE_OPENAI` | requires `--llmURL` (endpoint) and `--llmName` (deployment) |
| `ORCAROUTER` | OpenAI-compatible gateway, default `--llmURL https://api.orcarouter.ai/v1` |
| `MOCK` | fixed local responses, for tests |

The API key is passed with `--llmApiKey`, or through the configuration file, as for the other
providers. All LLM-assisted features (dictionary generation, input examples, response
classification, test-case naming) use the selected provider through the same code path.

## OrcaRouter

[OrcaRouter](https://www.orcarouter.ai) is an OpenAI-compatible AI gateway that routes many
providers behind one endpoint at `https://api.orcarouter.ai/v1`.

1. Create an API key at [https://www.orcarouter.ai/console](https://www.orcarouter.ai/console).
2. Pass it to EvoMaster, either on the command line:

   ```bash
   evomaster --blackBox true --bbSwaggerUrl <url> \
       --llm true --llmProvider ORCAROUTER --llmApiKey sk-orca-… \
       --llmName orcacyber-zero
   ```

   or through `ORCA_API_KEY` (or `ORCAROUTER_API_KEY`) in the environment. `--llmApiKey`
   takes precedence. The key is used as a Bearer token and never written to logs or output.
3. Inspect the models your key can actually use:

   ```bash
   evomaster --llm true --llmProvider ORCAROUTER --llmListModels true
   ```

   This reads `GET https://api.orcarouter.ai/v1/models?capability=chat` and prints the
   chat-capable models. Model ids are kept exactly as advertised (`vendor/model`).

**Model selection.** The default and recommended model is **OrcaCyber Zero**
(`orcacyber-zero`). It is intended for security teams working under a business authorization
and permits authorized security testing that general-purpose gateways often refuse. The model
is unlocked at workspace level through the Security Research tier (identity check, card,
acceptable-use acceptance and human review — see
[https://docs.orcarouter.ai/getting-started/cyber-models](https://docs.orcarouter.ai/getting-started/cyber-models));
without that tier, calls return `403`. When it is not available to your key,
`orcarouter/fusion` is the fallback, and `--llmListModels` shows the rest. A `--llmName` that
is not a chat-capable model in the current catalog is rejected before any request is made.

**Discovery and fallback.** Live discovery is authoritative. If the catalog endpoint is
unreachable, EvoMaster uses a small verified fallback catalog and prints a warning; it never
turns model selection into an unchecked free-text field.

**Credential lifecycle.** An OrcaRouter API key is a durable grant, not a refresh token: it is
reused until you revoke it in your OrcaRouter console
([https://www.orcarouter.ai/console](https://www.orcarouter.ai/console)).
There is no refresh call. If the key is rejected with `401`, EvoMaster stops with an
actionable message; replace `--llmApiKey` (or the environment variable) with a valid key.

**Further reading.** [API reference](https://docs.orcarouter.ai/api-reference/chat/create-a-chat-completion),
[model catalog](https://www.orcarouter.ai/models), and [terms of service](https://www.orcarouter.ai/terms.html).

**Self-hosted deployments.** Set `ORCA_API_BASE_URL` to the inference base of a deployment, or
`ORCA_BASE_URL` to a shared origin (EvoMaster appends `/v1`). An explicit `--llmURL` always
wins. Remote origins must use HTTPS; plain HTTP is accepted only for loopback addresses.
