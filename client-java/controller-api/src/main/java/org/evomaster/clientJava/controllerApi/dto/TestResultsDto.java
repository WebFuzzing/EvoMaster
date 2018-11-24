package org.evomaster.clientJava.controllerApi.dto;

import java.util.ArrayList;
import java.util.List;

public class TestResultsDto {

    public List<TargetInfoDto> targets = new ArrayList<>();


    /**
     * This list is sorted based on the action indices
     */
    public List<AdditionalInfoDto> additionalInfoList = new ArrayList<>();

    public List<ExtraHeuristicDto> extraHeuristics = new ArrayList<>();
}
