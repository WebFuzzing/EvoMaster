package org.evomaster.core.output.formatter

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

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

            override fun readFields(content: String, fieldNames: Set<String>): Map<String, String>? {
                return try {
                    val node = objectMapper.readTree(content)
                    if (!node.isObject) return null
                    fieldNames.mapNotNull { field ->
                        val value = node.get(field) ?: return@mapNotNull null
                        field to value.asText()
                    }.toMap()
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

            override fun readFields(content: String, fieldNames: Set<String>): Map<String, String>? {
                return try {
                    val doc = xmlFactory.newDocumentBuilder()
                        .parse(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)))
                    doc.documentElement.normalize()
                    fieldNames.mapNotNull { field ->
                        val nodes = doc.getElementsByTagName(field)
                        if (nodes.length > 0) field to nodes.item(0).textContent else null
                    }.toMap()
                } catch (e: Exception) {
                    null
                }
            }
        }

        init {
            registerFormatter(JSON_FORMATTER)
            registerFormatter(XML_FORMATTER)
        }


    }

    abstract fun isValid(content: String): Boolean
    abstract fun getFormatted(content: String): String
    abstract fun readFields(content: String, fieldNames: Set<String>): Map<String, String>?


}

