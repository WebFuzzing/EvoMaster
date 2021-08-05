import {StringSpecialization} from "./StringSpecialization";
import {TaintType} from "./TaintType";

export class StringSpecializationInfo {

    private readonly stringSpecialization: StringSpecialization;

    /**
     * A possible value to provide context to the specialization.
     * For example, if the specialization is a CONSTANT, then the "value" here would
     * the content of the constant
     */
    private readonly value: string;

    private readonly type: TaintType;

    constructor(stringSpecialization: StringSpecialization, value: string, taintType: TaintType = TaintType.FULL_MATCH) {
        this.stringSpecialization = stringSpecialization;
        this.value = value;
        if (!taintType || taintType === TaintType.NONE) {
            throw new Error("Invalid type: " + taintType);
        }
        this.type = taintType;
    }

    getStringSpecialization(): StringSpecialization {
        return this.stringSpecialization;
    }

    getValue(): string {
        return this.value;
    }

    getType(): TaintType {
        return this.type;
    }

    equalsTo(other: StringSpecializationInfo) : boolean {
        if(!other){
            return false;
        }

        return this.value === other.value && this.type === other.type && this.stringSpecialization === other.stringSpecialization;
    }
}