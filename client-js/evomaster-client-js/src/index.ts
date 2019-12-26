import babel from "./instrumentation/babel-plugin-evomaster";

import * as ControllerConstants from "./controller/api/ControllerConstants";
import EMController from "./controller/EMController";
import SutController from "./controller/SutController";

import AuthenticationDto from "./controller/api/dto/AuthenticationDto";
import ProblemInfo from "./controller/api/dto/problem/ProblemInfo";
import RestProblemDto from "./controller/api/dto/problem/RestProblemDto";
import {OutputFormat} from "./controller/api/dto/SutInfoDto";
import SutRunDto from "./controller/api/dto/SutRunDto";

module.exports = babel;
const ex = module.exports;

ex.SutController = SutController;
ex.EMController = EMController;
ex.ControllerConstants = ControllerConstants;

ex.dto = {};
ex.dto.AuthenticationDto = AuthenticationDto;
ex.dto.ProblemInfo = ProblemInfo;
ex.dto.RestProblemDto = RestProblemDto;
ex.dto.OutputFormat = OutputFormat;
ex.dto.SutRunDto = SutRunDto;
