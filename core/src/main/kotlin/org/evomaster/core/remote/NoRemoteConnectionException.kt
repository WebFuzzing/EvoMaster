package org.evomaster.core.remote


class NoRemoteConnectionException(
        port: Int, host: String
) : RuntimeException(
        "Cannot communicate with remote EvoMaster Driver for the system under test at $host:$port")