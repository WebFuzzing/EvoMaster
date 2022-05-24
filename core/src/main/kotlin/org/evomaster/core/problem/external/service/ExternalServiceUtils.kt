package org.evomaster.core.problem.external.service

import java.io.IOException
import java.net.*

object ExternalServiceUtils {

    /**
     * This method provides the next IP address from the given value for
     * loopback range. If generated IP address is not in the range, this
     * will throw an Exception.
     */
    fun nextIPAddress(address: String) : String {
        val tokens = address.split(".").toMutableList()
        if (tokens.size != 4) {
            throw IllegalArgumentException("Invalid IP address format")
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
        var ip = String.format("%s.%s.%s.%s", tokens[0], tokens[1], tokens[2], tokens[3])
        if (tokens[0].toInt() != 127) {
            throw Exception("Next available IP address is out of usable range")
        }
        // In the loopback address range 127.0.0.0/8, 127.255.255.255 will be the broadcast
        // address. 127.0.0.1 is skipped because the default loopback address and used in
        // other services commonly. 127.0.0.0 skipped because is the network address with
        // the mask 255.0.0.0 describes the whole loopback addresses.
        val reservedIPAddresses = arrayOf("127.0.0.0", "127.255.255.255", "127.0.0.1")
        if (reservedIPAddresses.contains(ip)) {
            ip = nextIPAddress(ip)
        }
        return ip
    }

    /**
     * This will generate random IP address for loopback range.
     *
     * Note: there is a chance for randomly generated IP address from different
     * execution to end-up closer to each other. Although the likelihood is low, but
     * can happen. As the result when creating the next IP address from the last used,
     * there will be negligible amount of small impact on the speed of the execution.
     */
    fun generateRandomIPAddress() : String {
        val (p1, p2, p3) = Triple(
            (0..255).shuffled().first(),
            (0..255).shuffled().first(),
            (0..254).shuffled().first(),
        )
        return String.format("127.%s.%s.%s", p1, p2, p3)
    }

    /**
     * Method will check whether the given IP address and port are available
     * for use by creating a Socket.
     * Connection timeout, connection refused are considered as available,
     * while successful connection as unavailable.
     * True if connection available, false if not.
     */
    fun isAddressAvailable(address: String, port: Int) : Boolean {
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(address, port), 1000)
            false
        } catch (e: ConnectException) {
            true
        } catch (e: SocketTimeoutException) {
            true
        } catch (e: IOException) {
            true
        } finally {
            if (socket != null) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    println(address + ": " + e.message)
                }
            }
        }
    }
}