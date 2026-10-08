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
        assertEquals("hello", (result.content[0] as McpTextToolContent).text)
        assertNull(result.structuredContent)
    }

    @Test
    fun testCallToolReturnsErrorResult() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"error message"}],"isError":true},"id":1}"""
        )

        val result = client.callTool("foo", emptyMap())

        assertTrue(result.isError)
        assertEquals(1, result.content.size)
        assertEquals("error message", (result.content[0] as McpTextToolContent).text)
    }

    @Test
    fun testCallToolParsesImageContent() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"content":[{"type":"image","data":"image-data","mimeType":"image/png"}],"isError":false},"id":1}"""
        )

        val result = client.callTool("foo", emptyMap())

        assertEquals(1, result.content.size)
        assertEquals("image", result.content[0].type)
        val image = result.content[0] as McpImageToolContent
        assertEquals("image-data", image.data)
        assertEquals("image/png", image.mimeType)
    }

    @Test
    fun testCallToolParsesAudioContent() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"content":[{"type":"audio","data":"audio-data","mimeType":"audio/wav"}],"isError":false},"id":1}"""
        )

        val result = client.callTool("foo", emptyMap())

        assertEquals(1, result.content.size)
        assertEquals("audio", result.content[0].type)
        val audio = result.content[0] as McpAudioToolContent
        assertEquals("audio-data", audio.data)
        assertEquals("audio/wav", audio.mimeType)
    }

    @Test
    fun testCallToolParsesResourceLinkContent() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"content":[{"type":"resource_link","uri":"file:///project/src/main.rs","name":"main.rs","description":"Primary application entry point","mimeType":"text/x-rust"}],"isError":false},"id":1}"""
        )

        val result = client.callTool("foo", emptyMap())

        assertEquals(1, result.content.size)
        assertEquals("resource_link", result.content[0].type)
        val link = result.content[0] as McpResourceLinkToolContent
        assertEquals("file:///project/src/main.rs", link.uri)
        assertEquals("main.rs", link.name)
        assertEquals("Primary application entry point", link.description)
        assertEquals("text/x-rust", link.mimeType)
    }

    @Test
    fun testCallToolParsesEmbeddedTextResourceContent() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"content":[{"type":"resource","resource":{"uri":"file:///project/src/main.rs","mimeType":"text/x-rust","text":"fn main() {}"}}],"isError":false},"id":1}"""
        )

        val result = client.callTool("foo", emptyMap())

        assertEquals(1, result.content.size)
        assertEquals("resource", result.content[0].type)
        val embedded = result.content[0] as McpEmbeddedResourceToolContent
        val resource = embedded.resource as McpTextResourceContent
        assertEquals("file:///project/src/main.rs", resource.uri)
        assertEquals("text/x-rust", resource.mimeType)
        assertEquals("fn main() {}", resource.text)
    }

    @Test
    fun testCallToolParsesEmbeddedBlobResourceContent() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"content":[{"type":"resource","resource":{"uri":"file:///data.bin","mimeType":"application/octet-stream","blob":"YmluYXJ5"}}],"isError":false},"id":1}"""
        )

        val result = client.callTool("foo", emptyMap())

        assertEquals(1, result.content.size)
        assertEquals("resource", result.content[0].type)
        val embedded = result.content[0] as McpEmbeddedResourceToolContent
        val resource = embedded.resource as McpBlobResourceContent
        assertEquals("file:///data.bin", resource.uri)
        assertEquals("application/octet-stream", resource.mimeType)
        assertEquals("YmluYXJ5", resource.blob)
    }

    @Test
    fun testCallToolParsesStructuredContent() {
        // Per spec, a tool returning structured content SHOULD also return the serialized JSON in a text block.
        stubPost(
            """{"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"{\"temperature\":22.5,\"conditions\":\"Partly cloudy\",\"humidity\":65}"}],"structuredContent":{"temperature":22.5,"conditions":"Partly cloudy","humidity":65}},"id":1}"""
        )

        val result = client.callTool("get_weather_data", mapOf("location" to "New York"))

        assertFalse(result.isError)
        assertNotNull(result.structuredContent)
        val structured = result.structuredContent!!
        assertEquals(22.5, structured["temperature"])
        assertEquals("Partly cloudy", structured["conditions"])
        assertEquals(65, structured["humidity"])
        // The unstructured text block should still be present alongside the structured content
        assertEquals(1, result.content.size)
        assertEquals("text", result.content[0].type)
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
    fun testCallToolWithMissingContentTypeThrows() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"content":[{"text":"no type here"}],"isError":false},"id":1}"""
        )

        assertThrows(IllegalStateException::class.java) {
            client.callTool("foo", emptyMap())
        }
    }

    @Test
    fun testCallToolWithUnsupportedContentTypeThrows() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"content":[{"type":"video","data":"video-data","mimeType":"video/mp4"}],"isError":false},"id":1}"""
        )

        assertThrows(IllegalStateException::class.java) {
            client.callTool("foo", emptyMap())
        }
    }

    @Test
    fun testReadResourceParsesContentCorrectly() {
        stubPost(
            """{"jsonrpc":"2.0","result":{"contents":[{"text":"resource content","uri":"file:///data/res","mimeType":"text/plain"}]},"id":1}"""
        )

        val result = client.readResource("file:///data/res")

        assertEquals(1, result.contents.size)
        assertEquals("resource content", (result.contents[0] as McpTextResourceContent).text)
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
