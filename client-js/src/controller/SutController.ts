import SutHandler from './SutHandler'
import AuthenticationDto from "./api/dto/AuthenticationDto";
import {OutputFormat} from './api/dto/SutInfoDto'
import ProblemInfo from "./api/dto/problem/ProblemInfo";
import UnitsInfoDto from "./api/dto/UnitsInfoDto";

export default abstract class SutController implements SutHandler{

    // ------- from SutHandler  ------------

    abstract resetStateOfSUT(): void;

    abstract startSut(): string;

    abstract stopSut(): void;


    // ------- other abstract methods -------

    /**
     * @return a list of valid authentication credentials, or {@code null} if
     *      * none is necessary
     */
    abstract  getInfoForAuthentication(): Array<AuthenticationDto>;



    /**
     * @return the format in which the test cases should be generated
     */
    abstract  getPreferredOutputFormat(): OutputFormat;


    /**
     * Check if the system under test (SUT) is running and fully initialized
     *
     * @return true if the SUT is running
     */
    abstract isSutRunning(): boolean;


    /**
     * Depending of which kind of SUT we are dealing with (eg, REST, GraphQL or SPA frontend),
     * there is different info that must be provided
     *
     * @return an instance of object with all the needed data for the specific addressed problem
     */
    abstract getProblemInfo(): ProblemInfo ;



    //------- other methods that MUST not be overridden -----------
    /*
        WARNING: there is no way in Typescript/Javascript to prevent a method from being
        overridden...
     */

    getUnitsInfoDto() : UnitsInfoDto{
        return null; //TODO
    }
}