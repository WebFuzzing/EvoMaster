import SutController from "../../../src/controller/SutController";
import AuthenticationDto from "../../../src/controller/api/dto/AuthenticationDto";
import {OutputFormat} from "../../../src/controller/api/dto/SutInfoDto";
import ProblemInfo from "../../../src/controller/api/dto/problem/ProblemInfo";
import RestProblemDto from "../../../src/controller/api/dto/problem/RestProblemDto";


export default class AppController  extends SutController{


    getInfoForAuthentication(): AuthenticationDto[] {
        return [];
    }

    getPreferredOutputFormat(): OutputFormat {
        return OutputFormat.JAVA_JUNIT_4; //TODO JavaScript
    }

    getProblemInfo(): ProblemInfo {
        const dto = new RestProblemDto();
        dto.swaggerJsonUrl = "TODO";

        return undefined;
    }

    isSutRunning(): boolean {
        return false;
    }

    resetStateOfSUT(): void {
    }

    startSut(): string {
        return "";
    }

    stopSut(): void {
    }


}