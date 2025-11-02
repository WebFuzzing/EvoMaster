import ActionDto from "./api/dto/ActionDto";
import AuthenticationDto from "./api/dto/AuthenticationDto";
import ProblemInfo from "./api/dto/problem/ProblemInfo";
import {OutputFormat} from "./api/dto/SutInfoDto";
import UnitsInfoDto from "./api/dto/UnitsInfoDto";
import SutHandler from "./SutHandler";
import AdditionalInfo from "../instrumentation/AdditionalInfo";
import ExecutionTracer from "../instrumentation/staticstate/ExecutionTracer";
import ObjectiveRecorder from "../instrumentation/staticstate/ObjectiveRecorder";
import TargetInfo from "../instrumentation/TargetInfo";
import Action from "../instrumentation/Action";
import UnitsInfoRecorder from "../instrumentation/staticstate/UnitsInfoRecorder";


export default abstract class SutController implements SutHandler {

    // ------- from SutHandler  ------------

    public abstract async resetStateOfSUT(): Promise<void>;

    public abstract async startSut(): Promise<string>;

    public abstract async stopSut(): Promise<void>;

    public setupForGeneratedTest(): Promise<void> {
        //nothing to do, at least for now???
        return Promise.resolve(undefined);
    }

    // ------- other abstract methods -------

    /**
     * @return a list of valid authentication credentials, or {@code null} if
     *      * none is necessary
     */
    public abstract  getInfoForAuthentication(): AuthenticationDto[];

    /**
     * @return the format in which the test cases should be generated
     */
    public abstract  getPreferredOutputFormat(): OutputFormat;

    /**
     * Check if the system under test (SUT) is running and fully initialized
     *
     * @return true if the SUT is running
     */
    public abstract isSutRunning(): boolean;

    /**
     * Depending of which kind of SUT we are dealing with (eg, REST, GraphQL or SPA frontend),
     * there is different info that must be provided
     *
     * @return an instance of object with all the needed data for the specific addressed problem
     */
    public abstract getProblemInfo(): ProblemInfo ;

    // ------- other methods that MUST not be overridden -----------
    /*
        WARNING: there is no way in Typescript/Javascript to prevent a method from being
        overridden...
     */


    /**
     * @return additional info for each action in the test.
     * The list is ordered based on the action index.
     */
    public  getAdditionalInfoList() : Array<AdditionalInfo>{
        return [...ExecutionTracer.exposeAdditionalInfoList()];
    }

    public getUnitsInfoDto(): UnitsInfoDto {

        const dto = new UnitsInfoDto();
        dto.unitNames = Array.from(UnitsInfoRecorder.getUnitNames());
        dto.numberOfLines = UnitsInfoRecorder.getNumberOfLines();
        dto.numberOfBranches = UnitsInfoRecorder.getNumberOfBranches();

        return dto;
    }

    /**
     * Check if instrumentation is on.
     *
     * @return true if the instrumentation is on
     */
    public isInstrumentationActivated(): boolean {
        return ObjectiveRecorder.getNumberOfTargets() > 0;
    }

    /**
     * Re-initialize all internal data to enable a completely new search phase
     * which should be independent from previous ones
     */
    public newSearch(): void {
        ExecutionTracer.reset();
        ObjectiveRecorder.reset(false);
    }

    /**
     * Re-initialize some internal data needed before running a new test
     */
    public newTest(): void {
        // actionIndex = -1;
        // resetExtraHeuristics();
        // extras.clear();

        ExecutionTracer.reset();

        /*
           Note: it should be fine but, if for any reason EM did not do
           a GET on the targets, then all those newly encountered targets
           would be lost, as EM will have no way to ask for them later, unless
           we explicitly say to return ALL targets
         */
        ObjectiveRecorder.clearFirstTimeEncountered();
    }

    public getTargetInfos(ids: Set<number>): Array<TargetInfo> {

        const list = new Array<TargetInfo>();

        const objectives = ExecutionTracer.getInternalReferenceToObjectiveCoverage();

        ids.forEach(id => {

            const descriptiveId = ObjectiveRecorder.getDescriptiveId(id);

            let info = objectives.get(descriptiveId);
            if(!info){
                info = TargetInfo.notReached(id);
            } else {
                info = info.withMappedId(id).withNoDescriptiveId();
            }

            list.push(info);
        });

        /*
         *  If new targets were found, we add them even if not requested by EM
         */
        ObjectiveRecorder.getTargetsSeenFirstTime().forEach(s => {

            const mappedId = ObjectiveRecorder.getMappedId(s);

            const info = objectives.get(s).withMappedId(mappedId);

            list.push(info);
        });

        return list;
    }

    /**
     * As some heuristics are based on which action (eg HTTP call, or click of button)
     * in the test sequence is executed, and their order, we need to keep track of which
     * action does cover what.
     *
     * @param dto the DTO with the information about the action (eg its index in the test)
     */
    public newAction(dto: ActionDto): void {

        /*
        if (dto.index > extras.size()) {
            extras.add(computeExtraHeuristics());
        }
        this.actionIndex = dto.index;
        resetExtraHeuristics();
        newActionSpecificHandler(dto);
         */

        ExecutionTracer.setAction(new Action(dto.index, new Set<string>(dto.inputVariables)));
    }

}
