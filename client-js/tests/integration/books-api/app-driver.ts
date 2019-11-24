import SutController from "../../../src/controller/SutController";
import AuthenticationDto from "../../../src/controller/api/dto/AuthenticationDto";
import {OutputFormat} from "../../../src/controller/api/dto/SutInfoDto";
import ProblemInfo from "../../../src/controller/api/dto/problem/ProblemInfo";
import RestProblemDto from "../../../src/controller/api/dto/problem/RestProblemDto";
import {AddressInfo} from "net";
import app from "../../sut/books-api/app";
import rep from "../../sut/books-api/repository";
import * as http from "http";



export default class AppController  extends SutController{

    private port: number;
    private server: http.Server;

    getInfoForAuthentication(): AuthenticationDto[] {
        return [];
    }

    getPreferredOutputFormat(): OutputFormat {
        return OutputFormat.JAVA_JUNIT_4; //TODO JavaScript
    }

    getProblemInfo(): ProblemInfo {
        const dto = new RestProblemDto();
        dto.swaggerJsonUrl = "http://localhost:"+this.port+"/swagger.json";

        return undefined;
    }

    isSutRunning(): boolean {
        if(!this.server){
            return false;
        }
        return this.server.listening;
    }

    resetStateOfSUT(): Promise<void> {
        rep.reset();
        return Promise.resolve();
    }

    startSut(): Promise<string> {

        return new Promise( resolve => {

            this.server = app.listen(0, "localhost", () => {
                this.port = (this.server.address() as AddressInfo).port;
                resolve("http://localhost:" + this.port);
            });
        });
    }

    stopSut(): Promise<void> {
        return new Promise( resolve => this.server.close( () => resolve()));
    }


}