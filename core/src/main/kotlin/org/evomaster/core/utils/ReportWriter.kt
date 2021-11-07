package org.evomaster.core.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * handle outputted reports, e.g., mutation log, sql log
 */
object ReportWriter {

    private val log: Logger = LoggerFactory.getLogger(ReportWriter::class.java)

    /**
     * write/append [value] to a specified [path]
     */
    fun writeByChannel(path : Path, value :String, doAppend : Boolean = false){

        if (!doAppend){
            if (path.parent != null && !Files.exists(path.parent)) Files.createDirectories(path.parent)

            if (Files.exists(path))
                log.warn("existing file ${path.toFile().absolutePath} will be replaced")
            Files.deleteIfExists(path)
            Files.createFile(path)
        }

        val options = if (!doAppend) setOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE)
                    else setOf(StandardOpenOption.APPEND)
        val buffer = ByteBuffer.wrap(value.toByteArray())
        FileChannel.open(path, options).run {
            writeToChannel(this, buffer)
        }
    }

    /**
     * @return a value wrapped with Quotation
     */
    fun wrapWithQuotation(value: String) = "\"$value\""

    private fun writeToChannel(channel: FileChannel, buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            channel.write(buffer)
        }
        channel.close()
    }
}