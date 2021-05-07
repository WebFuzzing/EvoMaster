import ReplacementList from "./ReplacementList";


export default class Replacement{


    public static replaceCall(idTemplate: string, caller: any, targetFunction: Function, ...inputs: any) : any{

        const r = ReplacementList.getReplacement(targetFunction);

        if(r){
            return r.replacement(idTemplate, caller, ...inputs);
        } else {
            return targetFunction.call(caller, ...inputs);
        }
    }

}
