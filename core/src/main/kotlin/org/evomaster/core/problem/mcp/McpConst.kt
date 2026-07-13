package org.evomaster.core.problem.mcp

object McpConst {

    /**
     * JSON-RPC version used for all MCP messages, as required by the MCP specification.
     */
    const val JSONRPC_VERSION = "2.0"

    /**
     * MCP protocol version negotiated during the initialize handshake.
     */
    const val PROTOCOL_VERSION = "2025-11-25"

    /**
     * HTTP header used by the Streamable HTTP transport to carry the session id
     * returned by the server after a successful initialize handshake.
     */
    const val SESSION_ID_HEADER = "Mcp-Session-Id"
}
