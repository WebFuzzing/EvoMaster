package org.evomaster.core.remote

import java.net.BindException
import java.net.ConnectException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import javax.ws.rs.ProcessingException


object TcpUtils {


    fun isTooManyRedirections(e: ProcessingException): Boolean {

        return (e.cause?.message?.contains("redirected too many") == true) && e.cause is ProtocolException
    }

    fun isTimeout(e: ProcessingException) = e.cause is SocketTimeoutException


    /**
     * This one is tricky.
     * A TCP connection also needs to open a TCP port, typically an available ephemeral one.
     * If we make too many connections, we might run out of them if OS is not fast enough to
     * release them once we close them.
     * This is a particular issue with Tomcat that, by default (at least currently),
     * shuts down connections at every 100 requests.
     */
    fun isOutOfEphemeralPorts(e: ProcessingException): Boolean {

        /*
            Problem is that there is no clear error message for such occurrence.
            And as TCP sockets are handled differently on different OS, it seems we also
            see different error messages / exception types :-(
         */

                //seen on Windows
        return (e.cause is BindException && checkText(e.cause!!, "Address already in use"))
                ||
                //seen on Linux
                (e.cause is ConnectException && checkText(e.cause!!, "Cannot assign requested address"))

    }

    private fun checkText(e: Throwable, text: String) =
            e.message?.contains(text, ignoreCase = true) == true
}