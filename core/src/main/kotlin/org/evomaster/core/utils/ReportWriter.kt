package org.evomaster.core.utils

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption


object ReportWriter {

    /**
     * write [value] to a specified [path]
     */
    fun writeByChannel(path : Path, value :String, options: Set<StandardOpenOption> = setOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE)){
        if (!Files.exists(path.parent)) Files.createDirectories(path.parent)
        Files.createFile(path)
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