package org.evomaster.client.java.controller.api.dto;

import java.util.ArrayList;
import java.util.List;

public class BootTimeInfoDto {

    public List<TargetInfoDto> targets;

    /**
     * Information about the external services used inside the SUT
     */
    public List<ExternalServiceInfoDto> externalServicesDto;
}
