import ActionDto from "./api/dto/ActionDto";
import AuthenticationDto from "./api/dto/AuthenticationDto";
import ProblemInfo from "./api/dto/problem/ProblemInfo";
import {OutputFormat} from "./api/dto/SutInfoDto";
import TargetInfoDto from "./api/dto/TargetInfoDto";
import UnitsInfoDto from "./api/dto/UnitsInfoDto";
import SutHandler from "./SutHandler";

export default abstract class SutController implements SutHandler {

    // ------- from SutHandler  ------------

    public abstract async resetStateOfSUT(): Promise<void>;

    public abstract async startSut(): Promise<string>;

    public abstract async stopSut(): Promise<void>;

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

    public getUnitsInfoDto(): UnitsInfoDto {
        return null; // TODO
    }

    /**
     * Check if bytecode instrumentation is on.
     *
     * @return true if the instrumentation is on
     */
    public isInstrumentationActivated(): boolean {
        return false; // TODO
    }

    /**
     * Re-initialize all internal data to enable a completely new search phase
     * which should be independent from previous ones
     */
    public newSearch(): void {
        // TODO
    }

    /**
     * Re-initialize some internal data needed before running a new test
     */
    public newTest(): void {
        // TODO
    }

    public getTargetInfos(ids: Set<number>): TargetInfoDto[] {
        // TODO
        return null;
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
    }

}
