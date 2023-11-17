package org.evomaster.core.problem.externalservice


/**
 * Information of the read hostname resolution, without any of our local IP address changes
 */
class HostnameResolutionInfo (
    val remoteHostName: String,
    val resolvedAddress: String?
) {

    fun isResolved() = resolvedAddress != null

}
