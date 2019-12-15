import AdditionalInfoDto from "./AdditionalInfoDto";
import ExtraHeuristicsDto from "./ExtraHeuristicsDto";
import TargetInfoDto from "./TargetInfoDto";

export default class TestResultsDto {

    public targets: TargetInfoDto[] = [];

    public additionalInfoList = new Array<AdditionalInfoDto>();

    public extraHeuristics = new Array<ExtraHeuristicsDto>();
}
