package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.instrumentation.shared.dto.ControlDependenceGraphDto;

import java.util.ArrayList;
import java.util.List;

public class TestResultsDto {

    public List<TargetInfoDto> targets = new ArrayList<>();


    /**
     * This list is sorted based on the action indices
     */
    public List<AdditionalInfoDto> additionalInfoList = new ArrayList<>();

    public List<ExtraHeuristicsDto> extraHeuristics = new ArrayList<>();

    /**
     * Incremental DynaMOSA control-dependence graphs discovered since the last handshake.
     */
    public List<ControlDependenceGraphDto> dynamosaCdgs = new ArrayList<>();
}
