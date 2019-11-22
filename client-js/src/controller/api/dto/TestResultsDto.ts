import TargetInfoDto from "./TargetInfoDto"
import AdditionalInfoDto from "./AdditionalInfoDto";
import ExtraHeuristicsDto from "./ExtraHeuristicsDto";


export default class TestResultsDto {

    targets: Array<TargetInfoDto> = [];


    additionalInfoList = new Array<AdditionalInfoDto>();

    extraHeuristics = new Array<ExtraHeuristicsDto>();
}
