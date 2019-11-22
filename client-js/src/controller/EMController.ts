import SutController from './SutController';
import * as c from './api/ControllerConstants'
import express from "express";
import bodyParser from "body-parser";
import {SutInfoDto} from './api/dto/SutInfoDto'
import WrappedResponseDto from "./api/dto/WrappedResponseDto";
import RestProblemDto from "./api/dto/problem/RestProblemDto";

export default class EMController {

    private sutController: SutController;
    private app = express();
    private baseUrlOfSUT: string;

    private controllerPort = c.DEFAULT_CONTROLLER_PORT;
    private controllerHost = c.DEFAULT_CONTROLLER_HOST;


    constructor(sutController: SutController) {
        this.sutController = sutController;

        this.initExpress();
    }

    startTheControllerServer(): boolean {
        return false;
    }


    private initExpress(): void {

        // to handle JSON payloads
        this.app.use(bodyParser.json());

        this.app.get(c.BASE_PATH + c.INFO_SUT_PATH, (req, res) => {

            const dto = new SutInfoDto();
            dto.isSutRunning = this.sutController.isSutRunning();
            dto.baseUrlOfSUT = this.baseUrlOfSUT;
            dto.infoForAuthentication = this.sutController.getInfoForAuthentication();
            //dto.sqlSchemaDto = this.sutController.getSqlDatabaseSchema();
            dto.defaultOutputFormat = this.sutController.getPreferredOutputFormat();

            const info = this.sutController.getProblemInfo();

            if (!info) {
                res.status(500);
                res.json(WrappedResponseDto.withError("Undefined problem type in the EM Controller"))
                return;
            } else if (info instanceof RestProblemDto){
                dto.restProblem = info;
            } else {
                res.status(500);
                res.json(WrappedResponseDto.withError("Unrecognized problem type: " + (typeof info)))
                return;
            }

            dto.unitsInfoDto = this.sutController.getUnitsInfoDto();
            if(!dto.unitsInfoDto){
                res.status(500);
                res.json(WrappedResponseDto.withError("Failed to extract units info"))
                return;
            }

            res.status(200);
            res.json(WrappedResponseDto.withData(dto));
        });




    }

}