package org.evomaster.core.problem.util

import org.evomaster.core.logging.LoggingUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object HttpWsUtil {

    val log: Logger = LoggerFactory.getLogger(HttpWsUtil::class.java)

    fun isContentTypeJson(contentType: String) = contentType.contains("json", ignoreCase = true)

    fun isContentTypeXml(contentType: String) = contentType.contains("xml", ignoreCase = true)

    fun isContentTypeTextPlain(contentType: String) = contentType.contains("text/plain", ignoreCase = true)

    fun isContentTypeForm(contentType: String) = contentType.contains("application/x-www-form-urlencoded", ignoreCase = true)

    fun isContentTypeMultipartForm(contentType: String) = contentType.contains("multipart/form-data", ignoreCase = true)

    const val CONTENT_TYPE_JSON = "application/json"
    const val CONTENT_TYPE_OCTET_STREAM = "application/octet-stream"
    const val CONTENT_TYPE_PDF = "application/pdf"
    const val CONTENT_TYPE_TEXT_PLAIN = "text/plain"
    const val CONTENT_TYPE_CSV = "text/csv"
    const val CONTENT_TYPE_PNG = "image/png"
    const val CONTENT_TYPE_JPEG = "image/jpeg"
    const val CONTENT_TYPE_GIF = "image/gif"

    //substrings, as these types have several variants (eg, image/svg+xml, application/x-zip-compressed)
    private const val CONTENT_TYPE_SVG_PREFIX = "image/svg"
    private const val CONTENT_TYPE_ZIP_TOKEN = "zip"
    private const val CONTENT_TYPE_XLSX_TOKEN = "spreadsheetml"
    private const val CONTENT_TYPE_DOCX_TOKEN = "wordprocessingml"
    private const val CONTENT_TYPE_PPTX_TOKEN = "presentationml"
    private const val CONTENT_TYPE_XLS_TOKEN = "ms-excel"
    private const val CONTENT_TYPE_JSON_TOKEN = "json"
    private const val CONTENT_TYPE_XML_TOKEN = "xml"

    private const val EXTENSION_PNG = ".png"
    private const val EXTENSION_JPG = ".jpg"
    private const val EXTENSION_GIF = ".gif"
    private const val EXTENSION_SVG = ".svg"
    private const val EXTENSION_PDF = ".pdf"
    private const val EXTENSION_CSV = ".csv"
    private const val EXTENSION_ZIP = ".zip"
    private const val EXTENSION_XLSX = ".xlsx"
    private const val EXTENSION_DOCX = ".docx"
    private const val EXTENSION_PPTX = ".pptx"
    private const val EXTENSION_XLS = ".xls"
    private const val EXTENSION_JSON = ".json"
    private const val EXTENSION_XML = ".xml"
    private const val EXTENSION_TXT = ".txt"
    private const val EXTENSION_BIN = ".bin"

    //many endpoints check the file extension before reading the content
    fun extensionForContentType(contentType: String): String = when {
        contentType.contains(CONTENT_TYPE_PNG, true) -> EXTENSION_PNG
        contentType.contains(CONTENT_TYPE_JPEG, true) -> EXTENSION_JPG
        contentType.contains(CONTENT_TYPE_GIF, true) -> EXTENSION_GIF
        contentType.contains(CONTENT_TYPE_SVG_PREFIX, true) -> EXTENSION_SVG
        contentType.contains(CONTENT_TYPE_PDF, true) -> EXTENSION_PDF
        contentType.contains(CONTENT_TYPE_CSV, true) -> EXTENSION_CSV
        contentType.contains(CONTENT_TYPE_ZIP_TOKEN, true) -> EXTENSION_ZIP
        //Office types contain "openxmlformats", so they must come before the "xml" check
        contentType.contains(CONTENT_TYPE_XLSX_TOKEN, true) -> EXTENSION_XLSX
        contentType.contains(CONTENT_TYPE_DOCX_TOKEN, true) -> EXTENSION_DOCX
        contentType.contains(CONTENT_TYPE_PPTX_TOKEN, true) -> EXTENSION_PPTX
        contentType.contains(CONTENT_TYPE_XLS_TOKEN, true) -> EXTENSION_XLS
        contentType.contains(CONTENT_TYPE_JSON_TOKEN, true) -> EXTENSION_JSON
        contentType.contains(CONTENT_TYPE_XML_TOKEN, true) -> EXTENSION_XML
        contentType.contains(CONTENT_TYPE_TEXT_PLAIN, true) -> EXTENSION_TXT
        else -> EXTENSION_BIN
    }


    fun getContentType(s: String) : String{
        return when{
            isContentTypeJson(s) -> getJsonContentType()
            isContentTypeForm(s) -> getFormContentType()
            isContentTypeXml(s) -> getXmlContentType()
            isContentTypeTextPlain(s) -> getTextPlainContentType()
            isContentTypeMultipartForm(s) -> getMultipartFormContentType()
            else -> {
                LoggingUtil.uniqueWarn(log, "Do not support the content type $s")
                getTextPlainContentType()
            }
        }
    }

    fun getJsonContentType() = "application/json"


    fun getXmlContentType() = "application/xml"

    fun getTextPlainContentType() = "text/plain"

    fun getFormContentType() = "application/x-www-form-urlencoded"

    fun getMultipartFormContentType() = "multipart/form-data"

}