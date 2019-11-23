import bodyParser from "body-parser";
import express from "express";
import * as c from "./api/ControllerConstants";
import ActionDto from "./api/dto/ActionDto";
import ControllerInfoDto from "./api/dto/ControllerInfoDto";
import RestProblemDto from "./api/dto/problem/RestProblemDto";
import {SutInfoDto} from "./api/dto/SutInfoDto";
import SutRunDto from "./api/dto/SutRunDto";
import TestResultsDto from "./api/dto/TestResultsDto";
import WrappedResponseDto from "./api/dto/WrappedResponseDto";
import SutController from "./SutController";
import {AddressInfo} from "net";

export default class EMController {

    private sutController: SutController;
    private app = express();
    private baseUrlOfSUT: string;

    private controllerPort = c.DEFAULT_CONTROLLER_PORT;
    private controllerHost = c.DEFAULT_CONTROLLER_HOST;

    /**
     * Might be different from controllerPort, if that was ephemeral
     */
    private actualPort: number;

    constructor(sutController: SutController) {
        this.sutController = sutController;

        this.initExpress();
    }

    startTheControllerServer(): boolean {

        const server = this.app.listen(this.controllerPort, this.controllerHost, () => {
            this.actualPort = (server.address() as AddressInfo).port;
        });

        return true;
    }

    setPort(value: number){
       this.controllerPort = value;
    }


    private initExpress(): void {

        // to handle JSON payloads
        this.app.use(bodyParser.json());

        this.app.get(c.BASE_PATH + c.INFO_SUT_PATH, (req, res) => {

            const dto = new SutInfoDto();
            dto.isSutRunning = this.sutController.isSutRunning();
            dto.baseUrlOfSUT = this.baseUrlOfSUT;
            dto.infoForAuthentication = this.sutController.getInfoForAuthentication();
            // dto.sqlSchemaDto = this.sutController.getSqlDatabaseSchema();
            dto.defaultOutputFormat = this.sutController.getPreferredOutputFormat();

            const info = this.sutController.getProblemInfo();

            if (!info) {
                res.status(500);
                res.json(WrappedResponseDto.withError("Undefined problem type in the EM Controller"));
                return;
            } else if (info instanceof RestProblemDto) {
                dto.restProblem = info;
            } else {
                res.status(500);
                res.json(WrappedResponseDto.withError("Unrecognized problem type: " + (typeof info)));
                return;
            }

            dto.unitsInfoDto = this.sutController.getUnitsInfoDto();
            if (!dto.unitsInfoDto) {
                res.status(500);
                res.json(WrappedResponseDto.withError("Failed to extract units info"));
                return;
            }

            res.status(200);
            res.json(WrappedResponseDto.withData(dto));
        });

        this.app.get(c.BASE_PATH + c.CONTROLLER_INFO, (req, res) => {

            const dto = new ControllerInfoDto();
            dto.fullName = (typeof this.sutController);
            dto.isInstrumentationOn = this.sutController.isInstrumentationActivated();

            // TODO will need something to identify the file to import

            res.status(200);
            res.json(WrappedResponseDto.withData(dto));
        });

        this.app.post(c.BASE_PATH + c.NEW_SEARCH, (req, res) => {

            this.sutController.newSearch();

            res.status(201);
            res.json(WrappedResponseDto.withNoData());
        });

        this.app.put(c.BASE_PATH + c.RUN_SUT_PATH, (req, res) => {

            if (!req.body) {
                res.status(400);
                res.json(WrappedResponseDto.withError("No provided JSON payload"));
                return;
            }

            const dto = req.body as SutRunDto;

            if (dto.run === undefined || dto.run === null) {
                res.status(400);
                res.json(WrappedResponseDto.withError("Invalid JSON: 'run' field is required"));
                return;
            }

            if (!dto.run) {
                if (dto.resetState) {
                    res.status(400);
                    res.json(WrappedResponseDto.withError("Invalid JSON: cannot reset state and stop service at same time"));
                    return;
                }

                // if on, we want to shut down the server
                if (this.sutController.isSutRunning()) {
                    this.sutController.stopSut();
                    this.baseUrlOfSUT = null;
                }

            } else {
                /*
                  If SUT is not up and running, let's start it
                 */

                if (!this.sutController.isSutRunning()) {
                    this.baseUrlOfSUT = this.sutController.startSut();
                    if (this.baseUrlOfSUT == null) {
                        // there has been an internal failure in starting the SUT
                        res.status(500);
                        res.json(WrappedResponseDto.withError("Internal failure: cannot start SUT based on given configuration"));
                        return;
                    }

                } else {
                    // TODO as starting should be blocking, need to check
                    // if initialized, and wait if not
                }

                /*
                regardless of where it was running or not, need to reset state.
                this is controlled by a boolean, although most likely we ll always
                want to do it
                */
                if (dto.resetState != null && dto.resetState) {
                    this.sutController.resetStateOfSUT();
                    this.sutController.newTest();
                }

                /*
                Note: here even if we start the SUT, the starting of a "New Search"
                cannot be done here, as in this endpoint we also deal with the reset
                of state. When we reset state for a new test run, we do not want to
                reset all the other data regarding the whole search
                */
            }

            res.status(204);
            res.json(WrappedResponseDto.withNoData());

        });

        this.app.get(c.BASE_PATH + c.TEST_RESULTS, (req, res) => {

            const idsParam = (req.query.ids) as string;

            const ids = new Set(idsParam
                .split(",")
                .filter((s) => s && s.trim().length > 0)
                .map((s) => parseInt(s, 10))
            );

            const targetInfos = this.sutController.getTargetInfos(ids);
            if (!targetInfos) {
                res.status(500);
                res.json(WrappedResponseDto.withError("Failed to collect target information for " + ids.size + " ids"));
                return;
            }

            const dto = new TestResultsDto();

            targetInfos.forEach((t) => { dto.targets.push(t); });

            // TODO AdditionalInfoDto

            res.status(200);
            res.json(WrappedResponseDto.withData(dto));
        });

        this.app.put(c.BASE_PATH + c.NEW_ACTION, (req, res) => {

            const dto = (req.body) as ActionDto;

            this.sutController.newAction(dto);

            res.status(204);
            res.json(WrappedResponseDto.withNoData());
        });

    }

}
