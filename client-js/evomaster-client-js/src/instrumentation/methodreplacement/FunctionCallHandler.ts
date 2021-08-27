import ObjectiveNaming from "../shared/ObjectiveNaming";
import Replacement from "./Replacement";
import HeuristicsForBooleans from "../heuristic/HeuristicsForBooleans";

/*
    Note: this class was needed to avoid import cycles between replacement classes and ReplacementList
 */

export default class FunctionCallHandler{

    public static handleFunctionCallBase(f: () => any) : any {

        HeuristicsForBooleans.clearLastEvaluation();
        const res = f();
        HeuristicsForBooleans.clearLastEvaluation();

        return res;
    }

    public static handleFunctionCallTracked(fileName: string, line: number, branchId: number, obj: any, functionName: string, ...args: any[]) : any {

        HeuristicsForBooleans.clearLastEvaluation();

        const idTemplate = ObjectiveNaming.methodReplacementObjectiveNameTemplate(fileName, line, branchId);

        //const res = obj[functionName](...args);
        const res = Replacement.replaceCall(idTemplate, obj,obj[functionName], ...args);

        HeuristicsForBooleans.clearLastEvaluation();

        return res;
    }

}