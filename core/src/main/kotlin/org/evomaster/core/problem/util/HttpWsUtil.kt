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