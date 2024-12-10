package org.evomaster.client.java.controller.api.dto;

import java.util.List;

/**
 * this dto represents the info collected during sut booting
 */
public class BootTimeInfoDto {

    /**
     * Info about the targets collected at boot-time
     */
    public List<TargetInfoDto> targets;

    /**
     * Information about the external services used inside the SUT
     */
    public List<ExternalServiceInfoDto> externalServicesDto;

    /**
     * Collections of hostnames collected through InetAddressReplacement
     */
    public List<HostnameResolutionInfoDto> hostnameResolutionInfoDtos;
}
