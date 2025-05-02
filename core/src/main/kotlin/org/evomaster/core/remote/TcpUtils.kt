package org.evomaster.core.remote

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.*
import javax.ws.rs.ProcessingException


object TcpUtils {

    private val log : Logger = LoggerFactory.getLogger(TcpUtils::class.java)

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

    fun handleEphemeralPortIssue(){
        val seconds = 60
        log.warn("Running out of ephemeral ports. Waiting $seconds seconds before re-trying connection." +
                " You might want to check the 'Troubleshooting' section in the documentation to see how" +
                " to change your OS settings to avoid this kind of problems.")
        Thread.sleep(seconds * 1000L)
    }

    /**
     *  This is a weird one... in theory, a server should never close a connection while
     *  a client is making a request.
     *  But we have seen this happening on Windows... sporadically.
     *  Maybe it is just a glitch, but, even in that case, we still need to handle it
     *  without crashing the whole EM process.
     */
    fun isStreamClosed(e: ProcessingException) : Boolean{

        return e.cause is IOException && checkText(e.cause!!, "Stream closed")
    }

    /**
     * Yet another weird exception...
     */
    fun isEndOfFile(e: ProcessingException) : Boolean{

        return  e.cause is SocketException && checkText(e.cause!!, "end of file")
    }

    /**
     * This does happen when we connect to a TCP socket at a given IP:port, but nothing
     * is listening there
     */
    fun isRefusedConnection(e: ProcessingException) : Boolean{

        return e.cause is ConnectException && checkText(e.cause!!, "Connection refused")
    }


    fun isUnknownHost(e: ProcessingException) : Boolean{
        return e.cause is java.net.UnknownHostException
    }

    fun isInternalError(e: ProcessingException) : Boolean{
        return e.cause is java.lang.IllegalArgumentException
    }

    private fun checkText(e: Throwable, text: String) =
            e.message?.contains(text, ignoreCase = true) == true
}