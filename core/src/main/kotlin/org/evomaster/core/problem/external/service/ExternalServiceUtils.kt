package org.evomaster.core.problem.external.service

import java.io.IOException
import java.net.*

object ExternalServiceUtils {

    fun nextIPAddress(host: String) : String {
        val tokens = host.split(".").toMutableList()
        if (tokens.size != 4) {
            throw IllegalArgumentException("Invalid IP Address")
        } else {
            for ( i in tokens.size - 1 downTo 0) {
                var part = tokens[i].toInt()
                if (part < 255) {
                    part += 1
                    tokens[i] = part.toString()
                    for (j in i + 1 until tokens.size) {
                        tokens[j] = "0"
                    }
                    break
                }
            }

        }
        return String.format("%s.%s.%s.%s", tokens[0], tokens[1], tokens[2], tokens[3])
    }

    fun isAddressAvailable(host: String, port: Int) : Boolean {
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), 500)
            println("Socket is active")
            false
        } catch (e: ConnectException) {
            println("ConnectException: " + e.message)
            true
        } catch (e: SocketTimeoutException) {
            println("SocketTimeoutException: " + e.message)
            true
        } catch (e: IOException) {
            println(e.message)
            true
        } finally {
            if (socket != null) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    println(e.message)
                }
            }
        }
    }
}