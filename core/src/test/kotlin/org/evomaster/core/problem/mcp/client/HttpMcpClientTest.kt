package org.evomaster.core.problem.mcp.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HttpMcpClientTest {

    private lateinit var wm: WireMockServer
    private lateinit var client: HttpMcpClient

    @BeforeEach
    fun setUp() {
        wm = WireMockServer(WireMockConfiguration().dynamicPort())
        wm.start()
        client = HttpMcpClient("http://localhost:${wm.port()}/mcp")
    }

    @AfterEach
    fun tearDown() {
        wm.stop()
    }

    private fun stubPost(responseBody: String) {
        wm.stubFor(
            WireMock.post(urlEqualTo("/mcp"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                )
        )
    }

    @Test
    fun testListToolsParsesDefinitionsCorrectly() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"tools":[{"name":"foo","description":"bar","inputSchema":{}}]},"id":1}"""
        )

        val tools = client.listTools()

        assertEquals(1, tools.size)
        assertEquals("foo", tools[0].name)
        assertEquals("bar", tools[0].description)
    }

    @Test
    fun testListToolsHandlesPagination() {
        // First page
        wm.stubFor(
            WireMock.post(urlEqualTo("/mcp"))
                .inScenario("pagination")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """{"jsonrpc":"2.0","result":{"tools":[{"name":"tool1","description":"","inputSchema":{}}],"nextCursor":"page2"},"id":1}"""
                        )
                )
                .willSetStateTo("page2")
        )
        // Second page
        wm.stubFor(
            WireMock.post(urlEqualTo("/mcp"))
                .inScenario("pagination")
                .whenScenarioStateIs("page2")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """{"jsonrpc":"2.0","result":{"tools":[{"name":"tool2","description":"","inputSchema":{}}]},"id":2}"""
                        )
                )
        )

        val tools = client.listTools()

        assertEquals(2, tools.size)
        assertEquals("tool1", tools[0].name)
        assertEquals("tool2", tools[1].name)
    }

    @Test
    fun testCallToolReturnsSuccessResult() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"hello"}],"isError":false},"id":1}"""
        )

        val result = client.callTool("foo", mapOf("arg" to "value"))

        assertFalse(result.isError)
        assertEquals(1, result.content.size)
        assertEquals("text", result.content[0].type)
        assertEquals("hello", result.content[0].text)
    }

    @Test
    fun testCallToolReturnsErrorResult() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"error message"}],"isError":true},"id":1}"""
        )

        val result = client.callTool("foo", emptyMap())

        assertTrue(result.isError)
        assertEquals(1, result.content.size)
        assertEquals("error message", result.content[0].text)
    }

    @Test
    fun testCallToolWithMissingResultReturnsError() {
        stubPost(
            """{"jsonrpc":"2.0","error":{"code":-32601,"message":"Method not found"},"id":1}"""
        )

        val result = client.callTool("nonexistent", emptyMap())

        assertTrue(result.isError)
        assertTrue(result.content.isEmpty())
    }

    @Test
    fun testReadResourceParsesContentCorrectly() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"contents":[{"text":"resource content","uri":"file:///data/res","mimeType":"text/plain"}]},"id":1}"""
        )

        val result = client.readResource("file:///data/res")

        assertEquals(1, result.contents.size)
        assertEquals("resource content", result.contents[0].text)
        assertEquals("file:///data/res", result.contents[0].uri)
        assertEquals("text/plain", result.contents[0].mimeType)
    }

    @Test
    fun testReadResourceWithMissingResultReturnsEmpty() {
        stubPost(
            """{"jsonrpc":"2.0","error":{"code":-32602,"message":"Invalid params"},"id":1}"""
        )

        val result = client.readResource("unknown://uri")

        assertTrue(result.contents.isEmpty())
    }

    @Test
    fun testListResourcesParsesDefinitionsCorrectly() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"resources":[{"uri":"file:///data","name":"data","description":"A data resource","mimeType":"application/json"}]},"id":1}"""
        )

        val resources = client.listResources()

        assertEquals(1, resources.size)
        assertEquals("file:///data", resources[0].uri)
        assertEquals("data", resources[0].name)
        assertEquals("application/json", resources[0].mimeType)
    }

    @Test
    fun testListResourceTemplatesParsesTemplatesCorrectly() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"resourceTemplates":[{"uriTemplate":"file:///{path}","name":"fileTemplate","description":"A file template"}]},"id":1}"""
        )

        val templates = client.listResourceTemplates()

        assertEquals(1, templates.size)
        assertEquals("file:///{path}", templates[0].uriTemplate)
        assertEquals("fileTemplate", templates[0].name)
    }
}
