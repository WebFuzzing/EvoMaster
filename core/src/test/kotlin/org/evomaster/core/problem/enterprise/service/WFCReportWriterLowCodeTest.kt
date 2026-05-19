package org.evomaster.core.problem.enterprise.service

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WFCReportWriterLowCodeTest {

    private val index = """
        <!doctype html>
        <html><head>
          <link rel="icon" type="image/svg+xml" href="/assets/icon.svg"/>
          <script type="module" crossorigin src="/assets/report.js"></script>
          <link rel="stylesheet" crossorigin href="/assets/report.css">
        </head><body><div id="root"></div></body></html>
    """.trimIndent()

    private val js = "console.log('bundle');"
    private val css = ".foo { color: red; }"
    private val icon = "<svg xmlns='http://www.w3.org/2000/svg'/>"
    private val json = """{"testFilePaths":["/abs/Foo.java"]}"""
    private val files = mapOf("/abs/Foo.java" to "class Foo {}")

    private fun build(
        js: String = this.js,
        css: String = this.css,
        json: String = this.json,
        files: Map<String, String> = this.files
    ) = WFCReportWriter.buildLowCodeHtml(index, js, css, icon, json, files)

    @Test
    fun testOutputIsWellFormedAndPreservesRoot() {
        val out = build()
        val doc = Jsoup.parse(out)
        assertTrue(out.lowercase().trimStart().startsWith("<!doctype"))
        assertNotNull(doc.selectFirst("#root"))
    }

    @Test
    fun testExternalBundleReferencesReplacedWithInlinedContent() {
        val out = build(js = "var markJs=1;", css = ".mark-css{}")
        val doc = Jsoup.parse(out)

        assertTrue(doc.select("script[src*=report.js]").isEmpty())
        assertTrue(doc.select("link[href*=report.css]").isEmpty())
        assertTrue(doc.select("style").any { it.data().contains(".mark-css") })
        assertTrue(doc.select("script[type=module]").any { it.data().contains("markJs") })
    }

    @Test
    fun testLowCodeFlagSetAndBootstrapRunsBeforeModule() {
        val out = build()
        val scripts = Jsoup.parse(out).select("script")
        val bootstrapIdx = scripts.indexOfFirst { it.data().contains("__WFC_LOW_CODE__") }
        val moduleIdx = scripts.indexOfFirst { it.attr("type") == "module" }

        assertTrue(out.contains("window.__WFC_LOW_CODE__ = true"))
        assertTrue(bootstrapIdx in 0 until moduleIdx)
    }

    @Test
    fun testEmbeddedMapIsJsObjectLiteralWithReportAndTestFiles() {
        val out = build()
        val assignIdx = out.indexOf("window.__WFC_EMBEDDED__ = ")
        assertTrue(assignIdx >= 0)
        assertEquals('{', out.substring(assignIdx + 26).trimStart().first())
        assertTrue(out.contains("./report.json"))
        assertTrue(out.contains("/abs/Foo.java"))
    }

    @Test
    fun testClosingScriptTagIsEscapedInAllEmbeddedContexts() {
        val evilJs = "var x='</script><img id=a src=x>';"
        val evilJson = """{"p":"</script><img id=b src=x>"}"""
        val evilFiles = mapOf("/f" to "//</script><img id=c src=x>")

        val doc = Jsoup.parse(build(js = evilJs, json = evilJson, files = evilFiles))

        assertTrue(doc.select("img#a").isEmpty(), "js bundle leak")
        assertTrue(doc.select("img#b").isEmpty(), "report.json leak")
        assertTrue(doc.select("img#c").isEmpty(), "test file leak")
    }

    @Test
    fun testFaviconInlinedAsDataUri() {
        val icon = Jsoup.parse(build()).selectFirst("link[rel=icon]")
        assertNotNull(icon)
        assertTrue(icon!!.attr("href").startsWith("data:image/svg+xml;base64,"))
    }

    @Test
    fun testCssContainingClosingStyleIsRejected() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            build(css = ".x{content:\"</style>\"}")
        }
        assertTrue(ex.message!!.contains("</style"))
    }

    @Test
    fun testEmptyTestFilesStillProducesValidDocument() {
        val out = build(files = emptyMap())
        assertNotNull(Jsoup.parse(out).selectFirst("#root"))
        assertTrue(out.contains("./report.json"))
    }
}
