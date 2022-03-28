import plugin, {Babel, PluginOptions} from "./instrumentation/babel-plugin-evomaster";

import EMController from "./controller/EMController";
import SutController from "./controller/SutController";

import AuthenticationDto from "./controller/api/dto/AuthenticationDto";
import HeaderDto from "./controller/api/dto/HeaderDto";
import ProblemInfo from "./controller/api/dto/problem/ProblemInfo";
import RestProblemDto from "./controller/api/dto/problem/RestProblemDto";
import GraphQLProblemDto from "./controller/api/dto/problem/GraphQLProblemDto";
import {OutputFormat} from "./controller/api/dto/SutInfoDto";
import SutRunDto from "./controller/api/dto/SutRunDto";
import ControllerConstants from "./controller/api/ControllerConstants";

import InjectedFunctions from "./instrumentation/InjectedFunctions";
import ExecutionTracer from "./instrumentation/staticstate/ExecutionTracer";
import ObjectiveRecorder from "./instrumentation/staticstate/ObjectiveRecorder";
import ObjectiveNaming from "./instrumentation/shared/ObjectiveNaming";
import EMTestUtils from "./EMTestUtils";

import {Visitor} from "@babel/traverse";
import JsonTokenPostLoginDto from "./controller/api/dto/JsonTokenPostLoginDto";



interface EM {
    (babel: Babel):  {visitor: Visitor<PluginOptions>},
    SutController: typeof SutController,
    EMController: typeof EMController;
    dto: {
        AuthenticationDto: typeof AuthenticationDto,
        HeaderDto: typeof HeaderDto,
        JsonTokenPostLoginDto: typeof JsonTokenPostLoginDto,
        ProblemInfo: typeof ProblemInfo,
        RestProblemDto: typeof RestProblemDto,
        GraphQLProblemDto: typeof GraphQLProblemDto,
        OutputFormat: typeof OutputFormat,
        SutRunDto: typeof SutRunDto
    },
    InjectedFunctions: typeof InjectedFunctions,
    internal: {
        ExecutionTracer: typeof ExecutionTracer,
        ObjectiveRecorder: typeof ObjectiveRecorder,
        ObjectiveNaming: typeof ObjectiveNaming,
        ControllerConstants: typeof ControllerConstants
    },
    EMTestUtils: typeof  EMTestUtils
}

//@ts-ignore
const f : EM = plugin;
f.SutController =  SutController;
f.EMController = EMController;
f.dto = {
    AuthenticationDto: AuthenticationDto,
    HeaderDto: HeaderDto,
    JsonTokenPostLoginDto: JsonTokenPostLoginDto,
    ProblemInfo: ProblemInfo,
    RestProblemDto: RestProblemDto,
    GraphQLProblemDto: GraphQLProblemDto,
    OutputFormat: OutputFormat,
    SutRunDto: SutRunDto
};
f.InjectedFunctions = InjectedFunctions;
f.internal = {
    ExecutionTracer: ExecutionTracer,
    ObjectiveRecorder: ObjectiveRecorder,
    ObjectiveNaming: ObjectiveNaming,
    ControllerConstants: ControllerConstants
};
f.EMTestUtils = EMTestUtils;

export = f;
