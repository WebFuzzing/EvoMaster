import {ReplacementType} from "../methodreplacement/ReplacementType";

export default class ObjectiveNaming {

    /**
     * Prefix identifier for file coverage objectives.
     * A file is "covered" if at least one of its lines is executed.
     *
     * Note: this is different from Java where we rather look at CLASS
     */
    static readonly FILE: string = "File";

    /**
     * Prefix identifier for line coverage objectives
     */
    static readonly LINE: string = "Line";

    /**
     * Prefix identifier for statement coverage objectives
     */
    static readonly STATEMENT: string = "Statement";

    /**
     * Prefix identifier for branch coverage objectives
     */
    static readonly BRANCH: string = "Branch";

    /**
     * Tag used in a branch id to specify it is for the "true"/then branch
     */
    static readonly TRUE_BRANCH: string = "_trueBranch";

    /**
     * Tag used in a branch id to specify it is for the "false"/else branch
     */
    static readonly FALSE_BRANCH: string = "_falseBranch";

    /**
     * Prefix identifier for MethodReplacement objectives, where we want
     * to cover both possible outcomes, eg true and false
     */
    static readonly METHOD_REPLACEMENT: string = "MethodReplacement";


    //FIXME: with statement objective, this maybe is not so important?
    /**
     * Prefix identifier for objectives related to calling methods without exceptions
     */
    static readonly SUCCESS_CALL: string = "Success_Call";


    static fileObjectiveName(fileId: string): string {
        return ObjectiveNaming.FILE + "_" + fileId;
    }

    static getFileIdFromFileObjectiveName(target: string): string {
        const prefix = ObjectiveNaming.FILE + "_";
        return target.substr(prefix.length);
    }

    static lineObjectiveName(fileId: string, line: number): string {
        return ObjectiveNaming.LINE + "_" + fileId + "_" + ObjectiveNaming.padNumber(line);
    }

    static statementObjectiveName(fileId: string, line: number, index: number): string {
        return ObjectiveNaming.STATEMENT + "_" + fileId + "_"
            + ObjectiveNaming.padNumber(line) + "_" + index;
    }

    static successCallObjectiveName(fileId: string, line: number, index: number): string {
        return ObjectiveNaming.SUCCESS_CALL + "_at_" + fileId + "_"
            + ObjectiveNaming.padNumber(line) + "_" + index;
    }

    static methodReplacementObjectiveNameTemplate(fileId: string, line: number, index: number): string {
        return ObjectiveNaming.METHOD_REPLACEMENT + "_at_" + fileId + "_"
            + ObjectiveNaming.padNumber(line) + "_" + index;
    }

    static methodReplacementObjectiveName(template: string, result: boolean, type: ReplacementType) {
        if (!template || !template.startsWith(ObjectiveNaming.METHOD_REPLACEMENT)) {
            throw new Error("Invalid template for boolean method replacement: " + template);
        }
        return template + "_" + type + "_" + result;
    }


    static branchObjectiveName(fileId: string, line: number, branchId: number, thenBranch: boolean): string {

        let name = ObjectiveNaming.BRANCH + "_at_" + fileId
            + "_at_line_" + ObjectiveNaming.padNumber(line) + "_position_" + branchId;
        if (thenBranch) {
            name += ObjectiveNaming.TRUE_BRANCH;
        } else {
            name += ObjectiveNaming.FALSE_BRANCH;
        }
        return name;
    }

    static padNumber(value: number): string {
        if (value < 0) {
            throw new Error("Negative number to pad");
        }
        if (value < 10) {
            return "0000" + value;
        }
        if (value < 100) {
            return "000" + value;
        }
        if (value < 1_000) {
            return "00" + value;
        }
        if (value < 10_000) {
            return "0" + value;
        } else {
            return "" + value;
        }
    }
}
