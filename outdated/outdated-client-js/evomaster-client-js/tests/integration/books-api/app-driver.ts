import * as http from "http";
import {AddressInfo} from "net";
import AuthenticationDto from "../../../src/controller/api/dto/AuthenticationDto";
import ProblemInfo from "../../../src/controller/api/dto/problem/ProblemInfo";
import RestProblemDto from "../../../src/controller/api/dto/problem/RestProblemDto";
import {OutputFormat} from "../../../src/controller/api/dto/SutInfoDto";
import SutController from "../../../src/controller/SutController";
import app from "../../sut/books-api/app";
import rep from "../../sut/books-api/repository";

export default class AppController  extends SutController {

    private port: number;
    private server: http.Server;

    public getInfoForAuthentication(): AuthenticationDto[] {
        return [];
    }

    public getPreferredOutputFormat(): OutputFormat {
        return OutputFormat.JAVA_JUNIT_4; // TODO JavaScript
    }

    public getProblemInfo(): ProblemInfo {
        const dto = new RestProblemDto();
        dto.openApiUrl = "http://localhost:" + this.port + "/swagger.json";

        return dto;
    }

    public isSutRunning(): boolean {
        if (!this.server) {
            return false;
        }
        return this.server.listening;
    }

    public resetStateOfSUT(): Promise<void> {
        rep.reset();
        return Promise.resolve();
    }

    public startSut(): Promise<string> {

        return new Promise( (resolve) => {

            this.server = app.listen(0, "localhost", () => {
                this.port = (this.server.address() as AddressInfo).port;
                resolve("http://localhost:" + this.port);
            });
        });
    }

    public stopSut(): Promise<void> {
        return new Promise( (resolve) => this.server.close( () => resolve()));
    }

}
