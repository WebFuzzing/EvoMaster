package org.evomaster.core.output.formatter

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.net.URLDecoder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private fun looksLikeNumberOrBoolean(s: String): Boolean {
    val t = s.trim()
    if (t == "true" || t == "false") return true
    return t.toBigDecimalOrNull() != null
}

/**
 * @javatypes: manzhang
 * @date: 27/08/2018
 * @description: this class can be extended for supporting different styles of outputs (i.e., test cases),
 *                 currently only json is supported with string input
 */
abstract class OutputFormatter (val name: String) {

    companion object {
        private var formatters = mutableMapOf<String, OutputFormatter>()

        //this function can be used to find the proper formatter
        fun findFormatter(type: String): OutputFormatter? {
            return formatters.get(type)
        }

        fun registerFormatter(formatter: OutputFormatter){
            formatters.put(formatter.name, formatter)
        }

        fun getFormatters():List<OutputFormatter>?{
            if(formatters.size > 0)
                return formatters.values.toList()
            return null
        }

        val JSON_FORMATTER = object : OutputFormatter("JSON_FORMATTER"){
                /*
                    GSON does not follow standard for JSON.
                    Should not be used for validation.
                    https://stackoverflow.com/questions/43233898/how-to-check-if-json-is-valid-in-java-using-gson
                 */
            val objectMapper  = ObjectMapper()
                    //also Jackson by default is happy to accept garbage :(
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)

            override fun isValid(content: String): Boolean{

                return try {
                    objectMapper.readTree(content)
                    true
                } catch (e: JsonProcessingException) {
                    false
                }

            }
            override fun getFormatted(content: String): String{
                if(this.isValid(content)){
                    return objectMapper.readTree(content)
                        .toPrettyString()
                        //on Windows, Jackson "might" use CRLF,
                        //which can be problematic
                        .replace("\r\n", "\n")
                }
                throw MismatchedFormatException(this, content)
            }

            override fun readFields(
                content: String,
                fieldNames: Set<String>?,
                numericAndBooleanOnly: Boolean
            ): Map<String, String>? {
                return try {
                    val node = objectMapper.readTree(content)
                    if (!node.isObject) return null
                    val out = mutableMapOf<String, String>()
                    val it = node.fields()
                    while (it.hasNext()) {
                        val (name, value) = it.next()
                        // JSON null is reported as field-absent so callers cannot confuse
                        // it with the literal 4-char string "null" (asText() collapses both).
                        if (value.isNull) continue
                        if (fieldNames != null && name !in fieldNames) continue
                        if (numericAndBooleanOnly && !value.isNumber && !value.isBoolean) continue
                        out[name] = value.asText()
                    }
                    out
                } catch (e: Exception) {
                    null
                }
            }
        }


        val XML_FORMATTER = object : OutputFormatter("XML_FORMATTER") {
            private val xmlFactory = DocumentBuilderFactory.newInstance()

            override fun isValid(content: String): Boolean {
                return try {
                    xmlFactory.newDocumentBuilder()
                        .parse(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)))
                    true
                } catch (e: Exception) {
                    false
                }
            }

            override fun getFormatted(content: String): String {
                if (!isValid(content)) throw MismatchedFormatException(this, content)
                val doc = xmlFactory.newDocumentBuilder()
                    .parse(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)))
                val transformer = TransformerFactory.newInstance().newTransformer()
                transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
                val writer = StringWriter()
                transformer.transform(DOMSource(doc), StreamResult(writer))
                return writer.toString().replace("\r\n", "\n")
            }

            override fun readFields(
                content: String,
                fieldNames: Set<String>?,
                numericAndBooleanOnly: Boolean
            ): Map<String, String>? {
                return try {
                    val doc = xmlFactory.newDocumentBuilder()
                        .parse(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)))
                    doc.documentElement.normalize()
                    val out = mutableMapOf<String, String>()
                    if (fieldNames != null) {
                        // resolve by tag name (any depth) to preserve historical behavior
                        for (field in fieldNames) {
                            val nodes = doc.getElementsByTagName(field)
                            if (nodes.length == 0) continue
                            val text = nodes.item(0).textContent ?: continue
                            if (numericAndBooleanOnly && !looksLikeNumberOrBoolean(text)) continue
                            out[field] = text
                        }
                    } else {
                        // walk the tree and collect leaf elements (those with no child elements).
                        // Spring/JAXB often wrap data in envelope/root elements, so iterating only
                        // top-level children would miss nested fields and produce concatenated
                        // text content from non-leaf elements.
                        collectLeafElements(doc.documentElement, out, numericAndBooleanOnly)
                    }
                    out
                } catch (e: Exception) {
                    null
                }
            }

            private fun collectLeafElements(
                element: org.w3c.dom.Element,
                out: MutableMap<String, String>,
                numericAndBooleanOnly: Boolean
            ) {
                val children = element.childNodes
                val elementChildren = mutableListOf<org.w3c.dom.Element>()
                for (i in 0 until children.length) {
                    val n = children.item(i)
                    if (n.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                        elementChildren.add(n as org.w3c.dom.Element)
                    }
                }
                if (elementChildren.isEmpty()) {
                    val text = element.textContent ?: return
                    if (numericAndBooleanOnly && !looksLikeNumberOrBoolean(text)) return
                    out[element.tagName] = text
                } else {
                    for (child in elementChildren) {
                        collectLeafElements(child, out, numericAndBooleanOnly)
                    }
                }
            }
        }


        val FORM_FORMATTER = object : OutputFormatter("FORM_FORMATTER") {

            override fun isValid(content: String): Boolean {
                return parseForm(content).isNotEmpty()
            }

            override fun getFormatted(content: String): String = content

            override fun readFields(
                content: String,
                fieldNames: Set<String>?,
                numericAndBooleanOnly: Boolean
            ): Map<String, String>? {
                return try {
                    val parsed = parseForm(content)
                    if (parsed.isEmpty()) return null
                    val out = mutableMapOf<String, String>()
                    for ((name, value) in parsed) {
                        if (fieldNames != null && name !in fieldNames) continue
                        if (numericAndBooleanOnly && !looksLikeNumberOrBoolean(value)) continue
                        out[name] = value
                    }
                    out
                } catch (e: Exception) {
                    null
                }
            }

            private fun parseForm(body: String): Map<String, String> {
                if (body.isBlank()) return emptyMap()
                return body.split("&").mapNotNull { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) {
                        try {
                            URLDecoder.decode(parts[0], "UTF-8") to
                                URLDecoder.decode(parts[1], "UTF-8")
                        } catch (e: Exception) { null }
                    } else null
                }.toMap()
            }
        }

        init {
            registerFormatter(JSON_FORMATTER)
            registerFormatter(XML_FORMATTER)
            registerFormatter(FORM_FORMATTER)
        }


    }

    abstract fun isValid(content: String): Boolean
    abstract fun getFormatted(content: String): String

    /**
     * Read fields from [content].
     *
     * @param fieldNames if non-null, only these fields are returned; if null, all top-level
     *                   primitive fields are returned.
     * @param numericAndBooleanOnly if true, fields whose value is not a number or boolean are
     *                              skipped (useful for oracles that compare two responses but
     *                              want to ignore strings to avoid flakiness from timestamps,
     *                              UUIDs, etc.)
     * @return map from field name to canonical string value, or null if [content] cannot be parsed
     */
    abstract fun readFields(
        content: String,
        fieldNames: Set<String>? = null,
        numericAndBooleanOnly: Boolean = false
    ): Map<String, String>?
}
