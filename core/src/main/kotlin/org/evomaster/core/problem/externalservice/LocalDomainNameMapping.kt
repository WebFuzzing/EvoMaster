package org.evomaster.core.problem.externalservice

import org.evomaster.core.problem.externalservice.HostnameInfo

open class LocalDomainNameMapping(
    val remoteHostnameInfo: HostnameInfo,
    val localIP: String
)
