package org.evomaster.core.problem.externalservice.httpws

import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.*

object HttpWsExternalServiceUtils {

    private val log: Logger = LoggerFactory.getLogger(HttpWsExternalServiceUtils::class.java)

    /**
     * This method provides the next IP address from the given value for
     * loopback range. If generated IP address is not in the range, this
     * will throw an Exception.
     */
    fun nextIPAddress(address: String): String {
        val tokens = address.split(".").toMutableList()
        if (tokens.size != 4) {
            throw IllegalArgumentException("Invalid IP address format")
        } else {
            if (tokens[0].toInt() != 127) {
                throw IllegalStateException("Next available IP address is out of usable range")
            }

            for (i in tokens.size - 1 downTo 0) {
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
        if (isReservedIP(ip)) {
            throw IllegalStateException("Next IP is a reserved address")
        }
        return ip
    }

    /**
     * In the loopback address range 127.0.0.0/8, 127.255.255.255 will be the broadcast
     * address. 127.0.0.1 is skipped because the default loopback address and used in
     * other services commonly. 127.0.0.0 skipped because is the network address with
     * the mask 255.0.0.0 describes the whole loopback addresses.
     */
    fun isReservedIP(ip: String): Boolean {
        val reservedIPAddresses = arrayOf("127.0.0.0", "127.255.255.255", "127.0.0.1")
        if (reservedIPAddresses.contains(ip)) {
            return true
        }
        return false
    }

    /**
     * This will generate random IP address for loopback range.
     *
     * Note: there is a chance for randomly generated IP address from different
     * execution to end-up closer to each other. Although the likelihood is low, but
     * can happen. As the result when creating the next IP address from the last used,
     * there will be negligible amount of small impact on the speed of the execution.
     */
    fun generateRandomIPAddress(randomness: Randomness): String {
        val (p1, p2, p3) = Triple(
            randomness.randomIPBit(),
            randomness.randomIPBit(),
            randomness.randomIPBit(),
        )
        var ip = String.format("127.%s.%s.%s", p1, p2, p3)

        if (isReservedIP(ip)) {
            while (isReservedIP(ip)) {
                ip = generateRandomIPAddress(randomness)
            }
        }
        return ip
    }

    /**
     * Method will check whether the given IP address and port are available
     * for use by creating a Socket.
     *
     * Code will try to connect to the given port on the specified IP. If the
     * connection succeed the given destination is not available.
     *
     * Connection timeout, connection refused are considered as available,
     * while successful connection as unavailable.
     *
     * True if connection available, false if not.
     */
    fun isAddressAvailable(address: String, port: Int): Boolean {
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(address, port), 1000)
            false
        } catch (e: Exception) {
            when (e) {
                is ConnectException, is SocketTimeoutException, is IOException -> {
                    true
                }
                else -> {
                    throw e
                }
            }
        } finally {
            if (socket != null) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    log.warn(address + ": " + e.message)
                }
            }
        }
    }
}