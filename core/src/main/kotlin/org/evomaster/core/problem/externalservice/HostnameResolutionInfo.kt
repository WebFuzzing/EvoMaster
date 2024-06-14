package org.evomaster.core.problem.externalservice


/**
 * Information of the read hostname resolution, without any of our local IP address changes
 */
class HostnameResolutionInfo (
    val remoteHostName: String,
    /**
     * Real IP address resolved in the SUT.
     * Should NEVER be used directly in the test.
     * It can be useful though for "harvesting", where the "core" will make direct calls
     * to real service to fetch possible data for seeding
     */
    val realResolvedAddress: String?
) {

    fun isResolved() = realResolvedAddress != null

}
